

## Environmental Variables

#### fhir.server.baseUrl

This controls the url used in the OAS/Swagger page. This needs to be altered when hosted on AWS (for example the base url of the ELB service).

`http://localhost:9001`

### fhir.igs

Optional - Use to override the default packages. Package id and version can be found from FHIR Implementation Guides.

`fhir.r4.ukcore.stu3.currentbuild#0.0.3-pre-release,uk.nhsengland.r4#0.0.0-prerelease`

### Ontology Server Configuration 

Optional, if Ontoserver is to be used then all parameters are required. 
Configuation details are found by requesting a [System-to-system account] from NHS England Ontology Server 

#### terminology.authorization.clientId

#### terminology.authorization.clientSecret

#### terminology.authorization.tokenUrl

`https://ontology.nhs.uk/authorisation/auth/realms/nhs-digital-terminology/protocol/openid-connect/token`

#### terminology.url

'https://ontology.nhs.uk/authoring/fhir/;

#### terminology.loincUrl

Experimental - do not use

'https://r4.ontoserver.csiro.au/fhir'

### AWS FHIRWorks Configuration

Can be obtained from NHS England Interoperability Standards

#### aws.apiKey

AWS FHIRWorks API Gateway Key 

#### aws.clientId

AWS Cognito token clientId

#### aws.pass

AWS FHIR Works password 

#### aws.tokenUrl

AWS Cognito token url

#### aws.user

AWS FHIR Works username

#### cdr.fhirServer

Url of AWS FHIRWorks in API Gateway
https://cnuc9zdola.execute-api.eu-west-2.amazonaws.com/dev




