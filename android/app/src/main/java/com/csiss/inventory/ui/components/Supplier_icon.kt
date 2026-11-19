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

val FluentuiSystemIconsSupplier: ImageVector
    get() {
        if (_FluentuiSystemIconsSupplier != null) return _FluentuiSystemIconsSupplier!!

        _FluentuiSystemIconsSupplier = ImageVector.Builder(
            name = "supplier",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(3.75f, 4f)
                curveTo(2.7835f, 4f, 2f, 4.7835f, 2f, 5.75f)
                verticalLineTo(16.25f)
                curveTo(2f, 17.2165f, 2.7835f, 18f, 3.75f, 18f)
                horizontalLineTo(4.0586f)
                curveTo(4.3796f, 19.6967f, 5.8673f, 21f, 7.65f, 21f)
                reflectiveCurveTo(10.9204f, 19.6967f, 11.2414f, 18f)
                horizontalLineTo(16.0586f)
                curveTo(16.3796f, 19.6967f, 17.8673f, 21f, 19.65f, 21f)
                reflectiveCurveTo(22.9204f, 19.6967f, 23.2414f, 18f)
                horizontalLineTo(23.25f)
                curveTo(23.6642f, 18f, 24f, 17.6642f, 24f, 17.25f)
                verticalLineTo(11.5f)
                curveTo(24f, 11.0027f, 23.7922f, 10.528f, 23.4268f, 10.192f)
                lineTo(20.8f, 6.5772f)
                curveTo(20.4431f, 6.208f, 19.9538f, 6f, 19.4402f, 6f)
                horizontalLineTo(17f)
                verticalLineTo(5.75f)
                curveTo(17f, 4.7835f, 16.2165f, 4f, 15.25f, 4f)
                horizontalLineTo(3.75f)
                close()

                // Cargo Box inner cutout
                moveTo(15.5f, 5.5f)
                curveTo(15.6381f, 5.5f, 15.75f, 5.6119f, 15.75f, 5.75f)
                verticalLineTo(16.5f)
                horizontalLineTo(3.75f)
                curveTo(3.6119f, 16.5f, 3.5f, 16.3881f, 3.5f, 16.25f)
                verticalLineTo(5.75f)
                curveTo(3.5f, 5.6119f, 3.6119f, 5.5f, 3.75f, 5.5f)
                horizontalLineTo(15.5f)
                close()

                // Cab Window cutout
                moveTo(17.65f, 7.5f)
                horizontalLineTo(19.44f)
                curveTo(19.6027f, 7.5f, 19.7579f, 7.566f, 19.871f, 7.6833f)
                lineTo(22.484f, 10.5f)
                horizontalLineTo(17.65f)
                verticalLineTo(7.5f)
                close()

                // Rear wheel inner circle
                moveTo(7.65f, 19.5f)
                curveTo(6.4626f, 19.5f, 5.5f, 18.5374f, 5.5f, 17.35f)
                curveTo(5.5f, 17.23f, 5.51f, 17.1124f, 5.529f, 17f)
                horizontalLineTo(9.771f)
                curveTo(9.79f, 17.1124f, 9.8f, 17.23f, 9.8f, 17.35f)
                curveTo(9.8f, 18.5374f, 8.8374f, 19.5f, 7.65f, 19.5f)
                close()

                // Front wheel inner circle
                moveTo(19.65f, 19.5f)
                curveTo(18.4626f, 19.5f, 17.5f, 18.5374f, 17.5f, 17.35f)
                curveTo(17.5f, 17.23f, 17.51f, 17.1124f, 17.529f, 17f)
                horizontalLineTo(21.771f)
                curveTo(21.79f, 17.1124f, 21.8f, 17.23f, 21.8f, 17.35f)
                curveTo(21.8f, 18.5374f, 20.8374f, 19.5f, 19.65f, 19.5f)
                close()
            }
        }.build()

        return _FluentuiSystemIconsSupplier!!
    }

private var _FluentuiSystemIconsSupplier: ImageVector? = null
