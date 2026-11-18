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

val FluentuiSystemIconsPerson: ImageVector
    get() {
        if (_FluentuiSystemIconsPerson != null) return _FluentuiSystemIconsPerson!!

        _FluentuiSystemIconsPerson = ImageVector.Builder(
            name = "person",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(17.7545f, 14.0002f)
                curveTo(18.9966f, 14.0002f, 20.0034f, 15.007f, 20.0034f, 16.2491f)
                verticalLineTo(16.8245f)
                curveTo(20.0034f, 17.7188f, 19.6838f, 18.5836f, 19.1023f, 19.263f)
                curveTo(17.5329f, 21.0965f, 15.1457f, 22.0013f, 12.0004f, 22.0013f)
                curveTo(8.8545f, 22.0013f, 6.46849f, 21.0962f, 4.90219f, 19.2619f)
                curveTo(4.32242f, 18.583f, 4.00391f, 17.7195f, 4.00391f, 16.8267f)
                verticalLineTo(16.2491f)
                curveTo(4.00391f, 15.007f, 5.01076f, 14.0002f, 6.25278f, 14.0002f)
                horizontalLineTo(17.7545f)
                close()
                moveTo(17.7545f, 15.5002f)
                horizontalLineTo(6.25278f)
                curveTo(5.83919f, 15.5002f, 5.50391f, 15.8355f, 5.50391f, 16.2491f)
                verticalLineTo(16.8267f)
                curveTo(5.50391f, 17.3624f, 5.69502f, 17.8805f, 6.04287f, 18.2878f)
                curveTo(7.29618f, 19.7555f, 9.26206f, 20.5013f, 12.0004f, 20.5013f)
                curveTo(14.7387f, 20.5013f, 16.7063f, 19.7555f, 17.9627f, 18.2876f)
                curveTo(18.3117f, 17.8799f, 18.5034f, 17.361f, 18.5034f, 16.8245f)
                verticalLineTo(16.2491f)
                curveTo(18.5034f, 15.8355f, 18.1681f, 15.5002f, 17.7545f, 15.5002f)
                close()
                moveTo(12.0004f, 2.00488f)
                curveTo(14.7618f, 2.00488f, 17.0004f, 4.24346f, 17.0004f, 7.00488f)
                curveTo(17.0004f, 9.76631f, 14.7618f, 12.0049f, 12.0004f, 12.0049f)
                curveTo(9.23894f, 12.0049f, 7.00036f, 9.76631f, 7.00036f, 7.00488f)
                curveTo(7.00036f, 4.24346f, 9.23894f, 2.00488f, 12.0004f, 2.00488f)
                close()
                moveTo(12.0004f, 3.50488f)
                curveTo(10.0674f, 3.50488f, 8.50036f, 5.07189f, 8.50036f, 7.00488f)
                curveTo(8.50036f, 8.93788f, 10.0674f, 10.5049f, 12.0004f, 10.5049f)
                curveTo(13.9334f, 10.5049f, 15.5004f, 8.93788f, 15.5004f, 7.00488f)
                curveTo(15.5004f, 5.07189f, 13.9334f, 3.50488f, 12.0004f, 3.50488f)
                close()
            }
        }.build()

        return _FluentuiSystemIconsPerson!!
    }

private var _FluentuiSystemIconsPerson: ImageVector? = null