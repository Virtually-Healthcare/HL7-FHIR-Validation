package uk.nhs.england.fhirvalidator.provider

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.annotation.*
import ca.uhn.fhir.rest.param.TokenParam
import ca.uhn.fhir.rest.server.IResourceProvider
import io.swagger.util.Yaml
import io.swagger.v3.core.util.Json
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.CapabilityStatement
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.nhs.england.fhirvalidator.service.ImplementationGuideParser
import uk.nhs.england.fhirvalidator.service.oas.CapabilityStatementToOpenAPIConversion
import java.nio.charset.StandardCharsets
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Component
class CapabilityStatementProvider(@Qualifier("R4") private val fhirContext: FhirContext,
                                  private val oasParser : CapabilityStatementToOpenAPIConversion,
                                  private val supportChain: ValidationSupportChain)  : IResourceProvider {
    /**
     * The getResourceType method comes from IResourceProvider, and must
     * be overridden to indicate what type of resource this provider
     * supplies.
     */
    override fun getResourceType(): Class<CapabilityStatement> {
        return CapabilityStatement::class.java
    }
    var implementationGuideParser: ImplementationGuideParser? = ImplementationGuideParser(fhirContext)



    @Search
    fun search(@OptionalParam(name = CapabilityStatement.SP_URL) url: TokenParam?): List<CapabilityStatement> {
        val list = HashMap<String, CapabilityStatement>()
        if (url !== null) {
            val decodeUri = java.net.URLDecoder.decode(url.value, StandardCharsets.UTF_8.name())
            val resource = supportChain.fetchResource(CapabilityStatement::class.java,decodeUri)
            if (resource != null) list.put(resource.url, resource)
            return list.values.toList()
        };
        for (resource in supportChain.fetchAllConformanceResources()!!) {
            if (resource is CapabilityStatement) {
                val cs = resource as CapabilityStatement
                if (list.get(cs.url) === null) {
                    list.put(resource.url,resource)
                }
            }
        }
        return list.values.toList()
    }

    @Operation(name = "openapi", idempotent = true,manualResponse=true, manualRequest=true)
    fun convertOpenAPI(
        servletRequest: HttpServletRequest,
        servletResponse: HttpServletResponse,
        @ResourceParam inputResource: IBaseResource,
        @OperationParam(name = "enhance") abstract: BooleanType?,
    ) {

       // var input = IOUtils.toString(servletRequest.getReader());
      //  var inputResource : IBaseResource
        servletResponse.setContentType("application/json")
        servletResponse.setCharacterEncoding("UTF-8")
        /*
        try {
            inputResource = fhirContext.newJsonParser().parseResource(input)
        } catch (ex : Exception) {
            inputResource = fhirContext.newXmlParser().parseResource(input)
        }

         */
        if (inputResource is CapabilityStatement) {
            val cs : CapabilityStatement = inputResource

            var boilerPlate = true
            if (abstract !== null && !abstract.booleanValue()) boilerPlate = false
            val os = oasParser.generateOpenApi(cs, boilerPlate);
            val yaml = Yaml.pretty().writeValueAsString(os);
            // System.out.println(yaml);
            servletResponse.writer.write(Json.pretty(os))
            servletResponse.writer.flush()
            return
        }
        servletResponse.writer.write("{}")
        servletResponse.writer.flush()
        return
    }

}
