package org.fossify.camera.extensions

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import org.fossify.camera.helpers.FLASH_OFF
import org.fossify.camera.helpers.FLASH_ON

fun Int.toCameraXFlashMode(): Int {
    return when (this) {
        FLASH_ON -> ImageCapture.FLASH_MODE_ON
        else -> ImageCapture.FLASH_MODE_OFF
    }
}

fun Int.toAppFlashMode(): Int {
    return when (this) {
        ImageCapture.FLASH_MODE_ON -> FLASH_ON
        else -> FLASH_OFF
    }
}

fun Int.toCameraSelector(): CameraSelector {
    return if (this == CameraSelector.LENS_FACING_FRONT) {
        CameraSelector.DEFAULT_FRONT_CAMERA
    } else {
        CameraSelector.DEFAULT_BACK_CAMERA
    }
}
