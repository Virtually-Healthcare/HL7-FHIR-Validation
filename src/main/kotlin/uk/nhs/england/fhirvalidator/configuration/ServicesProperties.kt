package uk.nhs.england.fhirvalidator.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "services")
data class ServicesProperties(
    var STU3: Boolean,
    var R4: Boolean,
    var LOINC: Boolean,
    var R4B: Boolean
) {

}
