package com.at.recallly.domain.model

enum class OnboardingStep(val value: Int) {
    NOT_STARTED(0),
    PERSONA_COMPLETED(1),
    FIELDS_COMPLETED(2),
    COMPLETED(3);

    companion object {
        fun fromValue(value: Int): OnboardingStep =
            entries.firstOrNull { it.value == value } ?: NOT_STARTED
    }
}
