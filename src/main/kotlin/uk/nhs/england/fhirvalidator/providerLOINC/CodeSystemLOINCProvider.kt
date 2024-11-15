package uk.nhs.england.fhirvalidator.providerLOINC

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.annotation.*
import ca.uhn.fhir.rest.server.IResourceProvider
import org.hl7.fhir.r4.model.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.nhs.england.fhirvalidator.interceptor.BasicAuthInterceptor
import jakarta.servlet.http.HttpServletRequest

@Component
class CodeSystemLOINCProvider (@Qualifier("R4") private val fhirContext: FhirContext,
                               private val basicAuthInterceptor: BasicAuthInterceptor
) : IResourceProvider {
    /**
     * The getResourceType method comes from IResourceProvider, and must
     * be overridden to indicate what type of resource this provider
     * supplies.
     */
    override fun getResourceType(): Class<CodeSystem> {
        return CodeSystem::class.java
    }
    @Operation(name = "\$lookup", idempotent = true)
    fun lookup (
        httpRequest : HttpServletRequest,
        @OperationParam(name = "code") code: String?,
        @OperationParam(name = "system") system: String?,
        @OperationParam(name = "property") property: String?,
        @OperationParam(name = "version") version: String?
    ) : Parameters? {
        val resource: Resource? = basicAuthInterceptor.readFromUrl(httpRequest.pathInfo,  httpRequest.queryString, null)
        return if (resource is Parameters) resource else null
    }


    @Operation(name = "\$units", idempotent = true)
    open fun getUnits(@OperationParam(name = "code")  code: String): Parameters {
        val coding = Parameters()
        val resource: Resource? = basicAuthInterceptor.readFromUrl("/CodeSystem/\$lookup",  "system=http://loinc.org&code="+code, null)
        if (resource is Parameters) {
            val parameters = resource
            for ( param in parameters.parameter) {
                if (param.hasName() && param.name.equals("property")) {
                    if (param.hasPart()) {
                        var isUCUM = false
                        var unit = ""
                        for (part in param.part) {
                            if (part.hasValue()) {
                                if ((part.value is CodeType) && (part.value as CodeType).code.equals("EXAMPLE_UCUM_UNITS")) {
                                    isUCUM =
                                        true
                                }
                                if ((part.value is StringType)) {
                                    unit =
                                        (part.value as StringType).value
                                }
                            }
                        }
                        if (isUCUM) {
                            val units = unit.split(";")
                            for (ut in units) {
                                coding.parameter.add(
                                    Parameters.ParametersParameterComponent().setValue(StringType().setValue(ut))
                                )
                            }
                        }
                    }
                }
            }
        }
        return coding
    }

}
