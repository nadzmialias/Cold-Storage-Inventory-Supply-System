package com.csiss.inventory.ui.components

/*
MIT License

Copyright (c) 2020 Microsoft Corporation

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val FluentuiSystemIconsBox: ImageVector
    get() {
        if (_FluentuiSystemIconsBox != null) return _FluentuiSystemIconsBox!!

        _FluentuiSystemIconsBox = ImageVector.Builder(
            name = "box",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(10.5911f, 2.51301f)
                curveTo(11.4947f, 2.14671f, 12.5053f, 2.14671f, 13.4089f, 2.51301f)
                lineTo(20.9075f, 5.55298f)
                curveTo(21.5679f, 5.82071f, 22f, 6.46216f, 22f, 7.17477f)
                verticalLineTo(16.8275f)
                curveTo(22f, 17.5401f, 21.5679f, 18.1815f, 20.9075f, 18.4493f)
                lineTo(13.4089f, 21.4892f)
                curveTo(12.5053f, 21.8555f, 11.4947f, 21.8555f, 10.5911f, 21.4892f)
                lineTo(3.09252f, 18.4493f)
                curveTo(2.43211f, 18.1815f, 2f, 17.5401f, 2f, 16.8275f)
                verticalLineTo(7.17477f)
                curveTo(2f, 6.46216f, 2.43211f, 5.82071f, 3.09252f, 5.55298f)
                lineTo(10.5911f, 2.51301f)
                close()
                moveTo(12.8453f, 3.90312f)
                curveTo(12.3032f, 3.68334f, 11.6968f, 3.68334f, 11.1547f, 3.90312f)
                lineTo(9.24097f, 4.67894f)
                lineTo(16.7678f, 7.60604f)
                lineTo(19.437f, 6.57542f)
                lineTo(12.8453f, 3.90312f)
                close()
                moveTo(14.6911f, 8.40787f)
                lineTo(7.21472f, 5.50039f)
                lineTo(4.59029f, 6.56435f)
                lineTo(12.0013f, 9.44642f)
                lineTo(14.6911f, 8.40787f)
                close()
                moveTo(3.5f, 16.8275f)
                curveTo(3.5f, 16.9293f, 3.56173f, 17.0209f, 3.65607f, 17.0592f)
                lineTo(11.1547f, 20.0991f)
                curveTo(11.1863f, 20.112f, 11.2183f, 20.1241f, 11.2503f, 20.1354f)
                verticalLineTo(10.7638f)
                lineTo(3.5f, 7.74979f)
                verticalLineTo(16.8275f)
                close()
                moveTo(12.8453f, 20.0991f)
                lineTo(20.3439f, 17.0592f)
                curveTo(20.4383f, 17.0209f, 20.5f, 16.9293f, 20.5f, 16.8275f)
                verticalLineTo(7.77292f)
                lineTo(12.7503f, 10.7651f)
                verticalLineTo(20.1352f)
                curveTo(12.7822f, 20.1239f, 12.8139f, 20.1119f, 12.8453f, 20.0991f)
                close()
            }
        }.build()

        return _FluentuiSystemIconsBox!!
    }

private var _FluentuiSystemIconsBox: ImageVector? = null