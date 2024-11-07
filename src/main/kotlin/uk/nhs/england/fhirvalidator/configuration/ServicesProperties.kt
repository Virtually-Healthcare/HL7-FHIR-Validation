package uk.nhs.england.fhirvalidator.configuration

import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties(prefix = "services")
data class ServicesProperties(
    var STU3: Boolean,
    var R4: Boolean,
    var LOINC: Boolean,
    var R4B: Boolean,
    var Utility : Boolean,
    var Experimental : Boolean
) {

}
