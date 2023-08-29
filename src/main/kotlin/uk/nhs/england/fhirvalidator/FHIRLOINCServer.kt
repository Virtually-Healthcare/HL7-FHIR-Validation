package uk.nhs.england.fhirvalidator

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.api.EncodingEnum
import ca.uhn.fhir.rest.server.RestfulServer
import com.amazonaws.services.sqs.AmazonSQS
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import uk.nhs.england.fhirvalidator.configuration.FHIRServerProperties
import uk.nhs.england.fhirvalidator.configuration.MessageProperties
import uk.nhs.england.fhirvalidator.interceptor.AWSAuditEventLoggingInterceptor
import uk.nhs.england.fhirvalidator.interceptor.ValidationInterceptor
import uk.nhs.england.fhirvalidator.providerLOINC.QuestionnaireProvider
import java.util.*
import javax.servlet.annotation.WebServlet

@WebServlet("/LOINC/R4/*", loadOnStartup = 1)
class FHIRLOINCServer(
    @Qualifier("R4") fhirContext: FhirContext,
    @Autowired(required = false) val sqs : AmazonSQS?,
    val questionnaireProvider: QuestionnaireProvider,
    val fhirServerProperties: FHIRServerProperties,
    private val messageProperties: MessageProperties
) : RestfulServer(fhirContext) {

    override fun initialize() {
        super.initialize()

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        registerProvider(questionnaireProvider)

        val awsAuditEventLoggingInterceptor =
            AWSAuditEventLoggingInterceptor(
                this.fhirContext,
                fhirServerProperties,
                messageProperties,
                sqs
            )
        interceptorService.registerInterceptor(awsAuditEventLoggingInterceptor)

        val validationInterceptor = ValidationInterceptor(fhirContext,messageProperties)
        interceptorService.registerInterceptor(validationInterceptor)

        isDefaultPrettyPrint = true
        defaultResponseEncoding = EncodingEnum.JSON
    }
}
