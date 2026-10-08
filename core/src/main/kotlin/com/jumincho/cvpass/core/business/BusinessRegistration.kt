package com.jumincho.cvpass.core.business

import java.time.LocalDate

/** The details the tax service needs to confirm that a business registration is genuine. */
data class BusinessRegistration(
    val number: BusinessNumber,
    /** Name of the representative (대표자명) exactly as registered. */
    val representativeName: String,
    /** Opening date (개업일자) exactly as registered. */
    val openingDate: LocalDate,
)
