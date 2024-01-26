package uk.nhs.england.fhirvalidator.service.interactions

import io.swagger.v3.oas.models.media.ArraySchema
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.*
import org.springframework.stereotype.Service
import uk.nhs.england.fhirvalidator.service.ImplementationGuideParser
import uk.nhs.england.fhirvalidator.service.SearchParameterSupport

@Service
class FHIRRESTful(
    implementationGuideParser: ImplementationGuideParser,
    private val searchParameterSupport : SearchParameterSupport,
    supportChain: ValidationSupportChain
) {


    fun test(resource: IBaseResource): List<OperationOutcome.OperationOutcomeIssueComponent> {
        val outcomes = mutableListOf<OperationOutcome.OperationOutcomeIssueComponent>()

        if (resource !is CapabilityStatement) {
            return outcomes
        }
        val capabilityStatement = resource as CapabilityStatement
        for((index,rest) in capabilityStatement.rest.withIndex()) {
            for((idx,resource) in rest.resource.withIndex()) {
                if (resource.hasSearchParam()) {
                   for(searchParameter in resource.searchParam) {
                       checkSearchParameter(resource.type,"CapabilityStatement.rest[$index].resource[$idx]",searchParameter,outcomes)

                   }
                }
            }
        }

       return outcomes
    }

    fun checkSearchParameter(
        resourceType: String,
        locaton: String,
        apiParameter: CapabilityStatement.CapabilityStatementRestResourceSearchParamComponent,
        outcomes: MutableList<OperationOutcome.OperationOutcomeIssueComponent>
    ) {
        if (apiParameter.name == null) {
            val issue = addOperationIssue(
                outcomes,
                OperationOutcome.IssueType.CODEINVALID,
                OperationOutcome.IssueSeverity.WARNING,
                "Name must not be null for resource type = "+resourceType
            )
            issue.location.add(StringType(locaton))
        }
        val searchParameter = getSearchParameter(outcomes, resourceType,apiParameter.name)
        if (searchParameter == null) {
            val issue = addOperationIssue(
                outcomes,
                OperationOutcome.IssueType.CODEINVALID,
                OperationOutcome.IssueSeverity.WARNING,
                "Unable to find FHIR SearchParameter of for: " + apiParameter.name
            )
            issue.location.add(StringType(locaton))
        } else {
            if (searchParameter.hasName() && searchParameter.name.startsWith("_include")) {

            }
            else {
                if (searchParameter.hasType() && apiParameter.hasType() && !searchParameter.type.equals(apiParameter.type)) {
                    val issue = addOperationIssue(
                        outcomes,
                        OperationOutcome.IssueType.CODEINVALID,
                        OperationOutcome.IssueSeverity.INFORMATION,
                        "Parameter type for **" + resourceType + '.'+ apiParameter.name + "** should be `" + searchParameter.type.toCode() + "` but is  `" + apiParameter.type.toCode()+"`."
                    )
                    issue.location.add(StringType(locaton))
                }
                if (searchParameter.hasType() && !apiParameter.hasType() && !searchParameter.type.equals(apiParameter.type)) {
                    val issue = addOperationIssue(
                        outcomes,
                        OperationOutcome.IssueType.CODEINVALID,
                        OperationOutcome.IssueSeverity.INFORMATION,
                        "Parameter type for **" + resourceType + '.'+ apiParameter.name + "** should be `" + searchParameter.type.toCode() + "` but is not present."
                    )
                    issue.location.add(StringType(locaton))
                }
                /*
                when (searchParameter.type) {
                    Enumerations.SearchParamType.STRING -> {
                        if (!apiParameter.type.equals(Enumerations.SearchParamType.STRING)) {
                            addOperationIssue(
                                outcomes,
                                OperationOutcome.IssueType.CODEINVALID,
                                OperationOutcome.IssueSeverity.INFORMATION,
                                "Parameter type for :" + apiParameter.name + " should be a `" + searchParameter.type.toCode() + "`, is  `" + apiParameter.type.toCode()+"`"
                            )
                        }
                    }

                    Enumerations.SearchParamType.REFERENCE -> {
                        if (!apiParameter.type.equals(Enumerations.SearchParamType.REFERENCE)) {
                            addOperationIssue(
                                outcomes,
                                OperationOutcome.IssueType.CODEINVALID,
                                OperationOutcome.IssueSeverity.INFORMATION,
                                "Parameter type for :" + apiParameter.name + " should be a `" + searchParameter.type.toCode() + "`, is  `" + apiParameter.type.toCode()+"`"
                            )
                        }
                    }

                    Enumerations.SearchParamType.TOKEN -> {
                        if (!apiParameter.type.equals(Enumerations.SearchParamType.TOKEN)) {
                            addOperationIssue(
                                outcomes,
                                OperationOutcome.IssueType.CODEINVALID,
                                OperationOutcome.IssueSeverity.INFORMATION,
                                "Parameter type for :" + apiParameter.name + " should be a `" + searchParameter.type.toCode() + "`, is  `" + apiParameter.type.toCode()+"`"
                            )
                        }
                    }

                    Enumerations.SearchParamType.DATE -> {
                        if (!apiParameter.type.equals(Enumerations.SearchParamType.DATE)) {
                            addOperationIssue(
                                outcomes,
                                OperationOutcome.IssueType.CODEINVALID,
                                OperationOutcome.IssueSeverity.INFORMATION,
                                "Parameter type for :" + apiParameter.name + " should be a `" + searchParameter.type.toCode() + "`, is  `" + apiParameter.type.toCode()+"`"
                            )
                        }
                    }

                    Enumerations.SearchParamType.NUMBER -> {
                        addOperationIssue(
                            outcomes,
                            OperationOutcome.IssueType.CODEINVALID,
                            OperationOutcome.IssueSeverity.INFORMATION,
                            "Parameter type for :" + apiParameter.name + " should be a `" + searchParameter.type.toCode() + "`, is  `" + apiParameter.type.toCode()+"`"
                        )
                    }

                    else -> {}
                }*/
            }
        }
    }

private fun addOperationIssue(outcomes: MutableList<OperationOutcome.OperationOutcomeIssueComponent>, code : OperationOutcome.IssueType, severity :OperationOutcome.IssueSeverity, message : String?  ): OperationOutcome.OperationOutcomeIssueComponent {
    val operation = OperationOutcome.OperationOutcomeIssueComponent()
    operation.code = code
    operation.severity = severity
    if (message!=null) operation.diagnostics = message
    outcomes.add(operation)
    return operation
}
    fun getSearchParameter(outcomes : MutableList<OperationOutcome.OperationOutcomeIssueComponent> , resourceType: String, name : String) : SearchParameter? {
        val parameters = name.split(".")

        val searchParameter = searchParameterSupport.getSearchParameter(resourceType,name)

        if (parameters.size>1) {
            if (searchParameter?.type != Enumerations.SearchParamType.REFERENCE) {
                // maybe throw error?
            } else {

                var resourceType: String?

                // A bit coarse
                resourceType = "Resource"
                if (searchParameter.hasTarget() ) {
                    for (resource in searchParameter.target) {
                        if (!resource.code.equals("Group")) resourceType=resource.code
                    }
                }

                var newSearchParamName = parameters.get(1)
                // Add back in remaining chained parameters
                for (i in 3..parameters.size) {
                    newSearchParamName += "."+parameters.get(i)
                }

            }
        }

        return searchParameter
    }

}
