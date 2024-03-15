package uk.nhs.england.fhirvalidator.providerLOINC

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.annotation.*
import ca.uhn.fhir.rest.param.StringParam
import ca.uhn.fhir.rest.param.TokenParam
import ca.uhn.fhir.rest.server.IResourceProvider
import mu.KLogging
import org.hl7.fhir.r4.model.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.nhs.england.fhirvalidator.interceptor.BasicAuthInterceptor
import jakarta.servlet.http.HttpServletRequest

@Component
class QuestionnaireProvider (@Qualifier("R4") private val fhirContext: FhirContext,
    private val basicAuthInterceptor: BasicAuthInterceptor,
    private val codeSystemLOINCProvider: CodeSystemLOINCProvider
) : IResourceProvider {
    /**
     * The getResourceType method comes from IResourceProvider, and must
     * be overridden to indicate what type of resource this provider
     * supplies.
     */
    override fun getResourceType(): Class<Questionnaire> {
        return Questionnaire::class.java
    }


    companion object : KLogging()

    @Read
    fun read( httpRequest : HttpServletRequest,@IdParam internalId: IdType): Questionnaire? {
        val resource: Resource? = basicAuthInterceptor.readFromUrl(httpRequest.pathInfo,  null, null)

        if (resource is Questionnaire) {
            var questionnaire = resource as Questionnaire
            if (questionnaire.hasItem()) getUnits(questionnaire.item)
            return questionnaire
        }

        return null
    }

    private fun getUnits(items: List<Questionnaire.QuestionnaireItemComponent>) {
       for (item in items) {
           if (item.hasCode() && item.code.size > 0 && item.codeFirstRep.hasSystem() && item.codeFirstRep.system.equals("http://loinc.org")) {
               val units = codeSystemLOINCProvider.getUnits(item.codeFirstRep.code)
               if (units !== null && units.hasParameter()) {
                   if (item.type.equals(Questionnaire.QuestionnaireItemType.DECIMAL)) {
                       // if it has units it is not a decimal
                       item.type = Questionnaire.QuestionnaireItemType.QUANTITY
                   }
                   for (unit in units.parameter) {
                       if (unit.hasValue() && unit.value is StringType) {
                           item.initial.add(
                               Questionnaire.QuestionnaireItemInitialComponent()
                                   .setValue(
                                       Coding().setSystem("http://unitsofmeasure.org").setCode((unit.value as StringType).value))
                           )
                           item.extension.add(
                               Extension()
                                   .setUrl("http://hl7.org/fhir/StructureDefinition/questionnaire-unitOption")
                                   .setValue(
                                       Coding().setSystem("http://unitsofmeasure.org").setCode((unit.value as StringType).value))
                           )
                       }
                   }
               }
           }
       }
    }

    @Search
    fun search(
        httpRequest : HttpServletRequest,
        @OptionalParam(name = Questionnaire.SP_URL) url: TokenParam?,
        @OptionalParam(name = Questionnaire.SP_TITLE) title: StringParam?,
        @OptionalParam(name = Questionnaire.SP_CODE) code: TokenParam?,
        @OptionalParam(name = "_content") content: StringParam?,
        @OptionalParam(name = "_count") count: StringParam?
               ): Bundle? {
        val resource: Resource? =
            basicAuthInterceptor.readFromUrl(httpRequest.pathInfo, httpRequest.queryString, "Patient")
        if (resource != null && resource is Bundle) {
            return resource
        }
        return null
    }

}
