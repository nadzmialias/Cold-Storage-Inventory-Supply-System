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

val FluentuiSystemIconsTemperature: ImageVector
    get() {
        if (_FluentuiSystemIconsTemperature != null) return _FluentuiSystemIconsTemperature!!

        _FluentuiSystemIconsTemperature = ImageVector.Builder(
            name = "temperature",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(12f, 2f)
                curveTo(13.733f, 2f, 15.1492f, 3.35645f, 15.2449f, 5.06546f)
                lineTo(15.25f, 5.24987f)
                lineTo(15.251f, 13.202f)
                lineTo(15.331f, 13.2709f)
                curveTo(16.2565f, 14.0975f, 16.8482f, 15.2418f, 16.9746f, 16.4939f)
                lineTo(16.9936f, 16.7457f)
                lineTo(17f, 17f)
                curveTo(17f, 19.7614f, 14.7614f, 22f, 12f, 22f)
                curveTo(9.23858f, 22f, 7f, 19.7614f, 7f, 17f)
                curveTo(7f, 15.6373f, 7.5496f, 14.3655f, 8.48922f, 13.4396f)
                lineTo(8.66993f, 13.2701f)
                lineTo(8.749f, 13.202f)
                lineTo(8.75f, 5.25f)
                curveTo(8.75f, 3.57886f, 10.0113f, 2.20232f, 11.6339f, 2.0204f)
                lineTo(11.8156f, 2.00514f)
                lineTo(12f, 2f)
                close()
                moveTo(12f, 3.5f)
                curveTo(11.0818f, 3.5f, 10.3288f, 4.20711f, 10.2558f, 5.10651f)
                lineTo(10.25f, 5.25004f)
                lineTo(10.2495f, 13.9445f)
                lineTo(9.94128f, 14.1691f)
                curveTo(9.04185f, 14.8246f, 8.5f, 15.8664f, 8.5f, 17f)
                curveTo(8.5f, 18.933f, 10.067f, 20.5f, 12f, 20.5f)
                curveTo(13.933f, 20.5f, 15.5f, 18.933f, 15.5f, 17f)
                curveTo(15.5f, 15.9376f, 15.0241f, 14.9558f, 14.2239f, 14.2971f)
                lineTo(14.0595f, 14.1697f)
                lineTo(13.7515f, 13.9451f)
                lineTo(13.75f, 5.25f)
                curveTo(13.75f, 4.2835f, 12.9665f, 3.5f, 12f, 3.5f)
                close()
                moveTo(12f, 8f)
                curveTo(12.4142f, 8f, 12.75f, 8.33579f, 12.75f, 8.75f)
                lineTo(12.7506f, 14.6146f)
                curveTo(13.7646f, 14.9334f, 14.5f, 15.8808f, 14.5f, 17f)
                curveTo(14.5f, 18.3807f, 13.3807f, 19.5f, 12f, 19.5f)
                curveTo(10.6193f, 19.5f, 9.5f, 18.3807f, 9.5f, 17f)
                curveTo(9.5f, 15.8804f, 10.2359f, 14.9328f, 11.2504f, 14.6143f)
                lineTo(11.25f, 8.75f)
                curveTo(11.25f, 8.33579f, 11.5858f, 8f, 12f, 8f)
                close()
            }
        }.build()

        return _FluentuiSystemIconsTemperature!!
    }

private var _FluentuiSystemIconsTemperature: ImageVector? = null