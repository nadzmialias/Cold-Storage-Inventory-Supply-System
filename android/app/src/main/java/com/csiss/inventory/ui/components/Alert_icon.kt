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

val FluentuiSystemIconsAlert: ImageVector
    get() {
        if (_FluentuiSystemIconsAlert != null) return _FluentuiSystemIconsAlert!!

        _FluentuiSystemIconsAlert = ImageVector.Builder(
            name = "alert",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(12.0001f, 1.99609f)
                curveTo(16.05f, 1.99609f, 19.3568f, 5.19084f, 19.4959f, 9.24515f)
                lineTo(19.5001f, 9.49609f)
                verticalLineTo(13.5931f)
                lineTo(20.8801f, 16.7491f)
                curveTo(20.9492f, 16.907f, 20.9848f, 17.0775f, 20.9848f, 17.2499f)
                curveTo(20.9848f, 17.9402f, 20.4252f, 18.4999f, 19.7348f, 18.4999f)
                lineTo(15.0001f, 18.5014f)
                curveTo(15.0001f, 20.1582f, 13.657f, 21.5014f, 12.0001f, 21.5014f)
                curveTo(10.4024f, 21.5014f, 9.09645f, 20.2524f, 9.0052f, 18.6776f)
                lineTo(8.99966f, 18.4991f)
                lineTo(4.27498f, 18.4999f)
                curveTo(4.10364f, 18.4999f, 3.93413f, 18.4646f, 3.77697f, 18.3964f)
                curveTo(3.14377f, 18.1213f, 2.85342f, 17.3851f, 3.12846f, 16.7519f)
                lineTo(4.50011f, 13.594f)
                verticalLineTo(9.49599f)
                curveTo(4.50071f, 5.3412f, 7.8522f, 1.99609f, 12.0001f, 1.99609f)
                close()
                moveTo(13.4997f, 18.4991f)
                lineTo(10.5001f, 18.5014f)
                curveTo(10.5001f, 19.3298f, 11.1717f, 20.0014f, 12.0001f, 20.0014f)
                curveTo(12.7798f, 20.0014f, 13.4206f, 19.4065f, 13.4932f, 18.6458f)
                lineTo(13.4997f, 18.4991f)
                close()
                moveTo(12.0001f, 3.49609f)
                curveTo(8.67995f, 3.49609f, 6.00059f, 6.17035f, 6.00011f, 9.49609f)
                verticalLineTo(13.9057f)
                lineTo(4.65613f, 16.9999f)
                horizontalLineTo(19.3526f)
                lineTo(18.0001f, 13.9067f)
                lineTo(18.0002f, 9.50895f)
                lineTo(17.9965f, 9.28375f)
                curveTo(17.8854f, 6.05027f, 15.2417f, 3.49609f, 12.0001f, 3.49609f)
                close()
            }
        }.build()

        return _FluentuiSystemIconsAlert!!
    }

private var _FluentuiSystemIconsAlert: ImageVector? = null