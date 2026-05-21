package com.abk.kernel.viewmodel

import com.abk.kernel.data.model.AbkRuntimeModule
import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeModuleControlTest {
    @Test
    fun `ABK control backed module prefers ABK control`() {
        val module = AbkRuntimeModule(
            id = "meta-abk-mount",
            source = "abk",
            controllable = true
        )

        assertEquals(RuntimeModuleControlBackend.ABK_CONTROL, module.preferredControlBackend())
    }

    @Test
    fun `merged ABK and KSU module prefers ABK control`() {
        val module = AbkRuntimeModule(
            id = "meta-abk-mount",
            type = "standard",
            source = "ksud,abk",
            controllable = true
        )

        assertEquals(RuntimeModuleControlBackend.ABK_CONTROL, module.preferredControlBackend())
    }

    @Test
    fun `KSU only module uses KSU backend`() {
        val module = AbkRuntimeModule(
            id = "zygisk_lsposed",
            type = "standard",
            source = "ksud",
            controllable = true
        )

        assertEquals(RuntimeModuleControlBackend.KSU, module.preferredControlBackend())
    }

    @Test
    fun `readonly builtin has no control backend`() {
        val module = AbkRuntimeModule(
            id = "readonly",
            type = "builtin",
            source = "abk",
            controllable = false,
            readonly = true
        )

        assertEquals(RuntimeModuleControlBackend.NONE, module.preferredControlBackend())
    }
}
