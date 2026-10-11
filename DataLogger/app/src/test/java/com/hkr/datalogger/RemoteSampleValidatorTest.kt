package com.hkr.datalogger

import org.junit.Assert.*
import org.junit.Test

class RemoteSampleValidatorTest {
    private fun frame(sequence: Long) = "SAMPLE $sequence light lx 100 200 3.5"

    @Test fun rejectsDuplicateAndOutOfOrderSamples() {
        val validator = RemoteSampleValidator()
        assertNotNull(validator.accept(frame(1)))
        assertNull(validator.accept(frame(1)))
        assertNull(validator.accept(frame(0)))
        assertNotNull(validator.accept(frame(2)))
    }

    @Test fun resetAllowsNewStreamSequence() {
        val validator = RemoteSampleValidator()
        assertNotNull(validator.accept(frame(8)))
        assertNull(validator.accept(frame(1)))
        validator.reset()
        assertNotNull(validator.accept(frame(1)))
    }

    @Test fun malformedSampleDoesNotAdvanceSequence() {
        val validator = RemoteSampleValidator()
        assertNull(validator.accept("SAMPLE 9 light lx 100 200 NaN"))
        assertNotNull(validator.accept(frame(1)))
    }
}
