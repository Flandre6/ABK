package com.abk.kernel.viewmodel

import com.abk.kernel.data.model.AbkRuntimeModule

internal enum class RuntimeModuleControlBackend {
    ABK_CONTROL,
    KSU,
    NONE
}

internal fun AbkRuntimeModule.preferredControlBackend(): RuntimeModuleControlBackend =
    when {
        controllable && source.split(',').any { it.trim() == "abk" } -> RuntimeModuleControlBackend.ABK_CONTROL
        normalizedRuntimeModuleType() == "standard" || source.split(',').any { it.trim() == "ksud" } -> RuntimeModuleControlBackend.KSU
        else -> RuntimeModuleControlBackend.NONE
    }

private fun AbkRuntimeModule.normalizedRuntimeModuleType(): String =
    type.ifBlank {
        when {
            source.split(',').any { it.trim() == "kpm" } -> "kpm"
            source.split(',').any { it.trim() == "ksud" } -> "standard"
            else -> "builtin"
        }
    }
