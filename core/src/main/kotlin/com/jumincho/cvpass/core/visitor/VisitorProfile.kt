package com.jumincho.cvpass.core.visitor

/**
 * The visitor's own details, entered once during onboarding and attached to every
 * check-in so that contact tracers can reach them.
 *
 * @property name a name already normalised by [PersonName.normalize].
 */
data class VisitorProfile(val name: String, val phone: PhoneNumber) {
    init {
        require(PersonName.normalize(name) == name) { "Name must be normalised first" }
    }
}
