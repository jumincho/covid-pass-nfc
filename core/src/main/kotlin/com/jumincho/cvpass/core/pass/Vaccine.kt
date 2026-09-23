package com.jumincho.cvpass.core.pass

/**
 * COVID-19 vaccines used in Korea's 2021–22 campaign, with the names a certificate may
 * print for them (Korean brand name, English name and international product name).
 */
enum class Vaccine(
    /** Doses that complete the primary series. */
    val primarySeriesDoses: Int,
    internal val keywords: List<String>,
) {
    PFIZER(2, listOf("화이자", "pfizer", "코미나티", "comirnaty")),
    MODERNA(2, listOf("모더나", "moderna", "스파이크박스", "spikevax")),
    ASTRAZENECA(2, listOf("아스트라제네카", "astrazeneca", "백스제브리아", "vaxzevria")),
    JANSSEN(1, listOf("얀센", "janssen")),
    NOVAVAX(2, listOf("노바백스", "novavax", "뉴백소비드", "nuvaxovid")),
    ;

    companion object {
        /** Doses that complete the primary series when a certificate mentions [vaccines]. */
        fun primarySeriesDoses(vaccines: Set<Vaccine>): Int =
            if (vaccines.isNotEmpty() && vaccines.all { it.primarySeriesDoses == 1 }) 1 else 2
    }
}
