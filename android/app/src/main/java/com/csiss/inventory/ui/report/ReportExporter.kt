package com.csiss.inventory.ui.report

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.csiss.inventory.data.GeneratedReportData
import com.csiss.inventory.data.ReportFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ExportedReportFile(
    val fileName: String,
    val format: ReportFormat,
    val uri: Uri?,
    val locationLabel: String,
    val rowCount: Int
)

object ReportExporter {

    private val fileStampFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.US)

    suspend fun export(
        context: Context,
        data: GeneratedReportData,
        format: ReportFormat
    ): ExportedReportFile = withContext(Dispatchers.IO) {
        val safeType = data.type.dbValue.lowercase(Locale.US)
        val stamp = LocalDateTime.now().format(fileStampFormatter)
        val fileName = "CSISS_${safeType}_${stamp}.${format.extension}"

        val writeContent: (OutputStream) -> Unit = { out ->
            when (format) {
                ReportFormat.CSV -> writeCsv(out, data)
                ReportFormat.PDF -> writePdf(out, data)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/CSISS")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Unable to create file in Downloads.")

            try {
                resolver.openOutputStream(uri)?.use(writeContent)
                    ?: error("Unable to write exported report file.")

                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }

            ExportedReportFile(
                fileName = fileName,
                format = format,
                uri = uri,
                locationLabel = "Downloads/CSISS/$fileName",
                rowCount = data.rows.size
            )
        } else {
            val dir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "CSISS"
            ).apply { mkdirs() }
            val file = File(dir, fileName)
            FileOutputStream(file).use(writeContent)

            ExportedReportFile(
                fileName = fileName,
                format = format,
                uri = Uri.fromFile(file),
                locationLabel = file.absolutePath,
                rowCount = data.rows.size
            )
        }
    }

    private fun writeCsv(out: OutputStream, data: GeneratedReportData) {
        val sb = StringBuilder()
        sb.appendLine("Report,${csvEscape(data.reportName)}")
        sb.appendLine("Type,${csvEscape(data.type.label)}")
        sb.appendLine("Date Range,${csvEscape(data.dateRangeLabel)}")
        sb.appendLine("Generated At,${csvEscape(data.generatedAtLabel)}")
        sb.appendLine()
        data.summaryMetrics.forEach { (label, value) ->
            sb.appendLine("${csvEscape(label)},${csvEscape(value)}")
        }
        sb.appendLine()
        sb.appendLine(data.headers.joinToString(",") { csvEscape(it) })
        data.rows.forEach { row ->
            sb.appendLine(row.joinToString(",") { csvEscape(it) })
        }
        out.write(sb.toString().toByteArray(Charsets.UTF_8))
    }

    private fun csvEscape(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun writePdf(out: OutputStream, data: GeneratedReportData) {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val contentWidth = pageWidth - margin * 2f
        val bottomLimit = pageHeight - 48f

        val navyColor = Color.parseColor("#132945")
        val inkColor = Color.parseColor("#182B48")
        val mutedColor = Color.parseColor("#718198")
        val frostBg = Color.parseColor("#F1F6FB")
        val altRowBg = Color.parseColor("#F8FAFC")
        val borderColor = Color.parseColor("#E0E6EF")

        val headerBgPaint = Paint().apply { color = navyColor; style = Paint.Style.FILL }
        val cardBgPaint = Paint().apply { color = frostBg; style = Paint.Style.FILL }
        val altRowPaint = Paint().apply { color = altRowBg; style = Paint.Style.FILL }
        val borderPaint = Paint().apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D2E2F2")
            textSize = 9.5f
        }
        val metricLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedColor
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val metricValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = navyColor
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            textSize = 8.5f
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedColor
            textSize = 8.5f
        }

        val totalWeight = data.columnWeights.sum().coerceAtLeast(1f)
        val colWidths = data.columnWeights.map { (it / totalWeight) * contentWidth }

        val document = PdfDocument()
        try {
            var pageNumber = 1
            var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var canvas = page.canvas
            var y = margin

            val bannerRect = RectF(margin, y, pageWidth - margin, y + 72f)
            canvas.drawRoundRect(bannerRect, 10f, 10f, headerBgPaint)
            canvas.drawText("CSISS · COLD STORAGE INVENTORY SUPPLY SYSTEM", margin + 14f, y + 20f, brandPaint)
            canvas.drawText(fitText(data.reportName, titlePaint, contentWidth - 28f), margin + 14f, y + 42f, titlePaint)
            canvas.drawText(
                fitText("Range: ${data.dateRangeLabel}  |  Generated: ${data.generatedAtLabel}", subtitlePaint, contentWidth - 28f),
                margin + 14f,
                y + 59f,
                subtitlePaint
            )
            y += 86f

            if (data.summaryMetrics.isNotEmpty()) {
                val gap = 10f
                val boxWidth = (contentWidth - gap * (data.summaryMetrics.size - 1)) / data.summaryMetrics.size
                data.summaryMetrics.forEachIndexed { idx, (label, value) ->
                    val left = margin + idx * (boxWidth + gap)
                    val rect = RectF(left, y, left + boxWidth, y + 46f)
                    canvas.drawRoundRect(rect, 8f, 8f, cardBgPaint)
                    canvas.drawRoundRect(rect, 8f, 8f, borderPaint)
                    canvas.drawText(fitText(label.uppercase(Locale.ENGLISH), metricLabelPaint, boxWidth - 16f), left + 10f, y + 17f, metricLabelPaint)
                    canvas.drawText(fitText(value, metricValPaint, boxWidth - 16f), left + 10f, y + 36f, metricValPaint)
                }
                y += 60f
            }

            fun drawTableHeader() {
                val headerRect = RectF(margin, y, pageWidth - margin, y + 24f)
                canvas.drawRect(headerRect, headerBgPaint)
                var x = margin
                data.headers.forEachIndexed { idx, header ->
                    val width = colWidths.getOrElse(idx) { 60f }
                    canvas.drawText(fitText(header, tableHeaderPaint, width - 8f), x + 4f, y + 15.5f, tableHeaderPaint)
                    x += width
                }
                y += 24f
            }

            fun drawFooter(num: Int) {
                canvas.drawLine(margin, pageHeight - 36f, pageWidth - margin, pageHeight - 36f, borderPaint)
                canvas.drawText("CSISS Report Export", margin, pageHeight - 22f, footerPaint)
                val pageLabel = "Page $num"
                val labelWidth = footerPaint.measureText(pageLabel)
                canvas.drawText(pageLabel, pageWidth - margin - labelWidth, pageHeight - 22f, footerPaint)
            }

            drawTableHeader()

            val rowHeight = 20f
            if (data.rows.isEmpty()) {
                canvas.drawText("No records found for the selected criteria.", margin + 6f, y + 18f, footerPaint)
            } else {
                data.rows.forEachIndexed { rowIndex, row ->
                    if (y + rowHeight > bottomLimit) {
                        drawFooter(pageNumber)
                        document.finishPage(page)
                        pageNumber++
                        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                        canvas = page.canvas
                        y = margin
                        drawTableHeader()
                    }

                    if (rowIndex % 2 == 1) {
                        canvas.drawRect(margin, y, pageWidth - margin, y + rowHeight, altRowPaint)
                    }
                    canvas.drawLine(margin, y + rowHeight, pageWidth - margin, y + rowHeight, borderPaint)

                    var x = margin
                    row.forEachIndexed { colIdx, cell ->
                        val width = colWidths.getOrElse(colIdx) { 60f }
                        canvas.drawText(fitText(cell, cellPaint, width - 8f), x + 4f, y + 13.5f, cellPaint)
                        x += width
                    }
                    y += rowHeight
                }
            }

            drawFooter(pageNumber)
            document.finishPage(page)
            document.writeTo(out)
        } finally {
            document.close()
        }
    }

    private fun fitText(text: String, paint: Paint, maxWidth: Float): String {
        if (maxWidth <= 0f) return ""
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        val ellipsisWidth = paint.measureText(ellipsis)
        val count = paint.breakText(text, true, (maxWidth - ellipsisWidth).coerceAtLeast(0f), null)
        return if (count <= 0) ellipsis else text.substring(0, count).trimEnd() + ellipsis
    }
}
