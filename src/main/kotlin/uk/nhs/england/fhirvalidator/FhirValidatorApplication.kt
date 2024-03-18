package uk.nhs.england.fhirvalidator


import mu.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.ServletComponentScan
import uk.nhs.england.fhirvalidator.configuration.*


@SpringBootApplication
@ServletComponentScan
@EnableConfigurationProperties(TerminologyValidationProperties::class,FHIRServerProperties::class, ServicesProperties::class)
open class FhirValidatorApplication : ApplicationRunner {

    private val logger = KotlinLogging.logger {}

    override fun run(args: ApplicationArguments?) {
        logger.debug("EXECUTING THE APPLICATION")
        if (args != null) {
            for (opt in args.optionNames) {
                logger.debug("args: {}", opt)
            }
        }
    }


}

fun main(args: Array<String>) {
    runApplication<FhirValidatorApplication>(*args)
}
