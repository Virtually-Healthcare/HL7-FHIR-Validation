package uk.nhs.england.fhirvalidator

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.api.EncodingEnum
import ca.uhn.fhir.rest.server.RestfulServer
import com.amazonaws.services.sqs.AmazonSQS
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.annotation.WebServlet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import uk.nhs.england.fhirvalidator.configuration.FHIRServerProperties
import uk.nhs.england.fhirvalidator.configuration.MessageProperties
import uk.nhs.england.fhirvalidator.interceptor.AWSAuditEventLoggingInterceptor
import uk.nhs.england.fhirvalidator.interceptor.ValidationInterceptor
import uk.nhs.england.fhirvalidator.providerLOINC.CodeSystemLOINCProvider
import uk.nhs.england.fhirvalidator.providerLOINC.QuestionnaireProvider
import uk.nhs.england.fhirvalidator.providerLOINC.ValueSetLOINCProvider
import java.util.*


@ConditionalOnProperty(prefix = "services", name = ["LOINC"])
@WebServlet("/LOINC/R4/*", loadOnStartup = 1)
class FHIRLOINCServer(
    @Qualifier("R4") fhirContext: FhirContext,
    @Autowired(required = false) val sqs : AmazonSQS?,
    val questionnaireProvider: QuestionnaireProvider,
    val codeSystemLOINCProvider: CodeSystemLOINCProvider,
    val valueSetLOINCProvider: ValueSetLOINCProvider,
    val fhirServerProperties: FHIRServerProperties,
    private val messageProperties: MessageProperties
) : RestfulServer(fhirContext) {

    override fun initialize() {
        super.initialize()

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        registerProvider(questionnaireProvider)
        registerProvider(codeSystemLOINCProvider)
        registerProvider(valueSetLOINCProvider)

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
