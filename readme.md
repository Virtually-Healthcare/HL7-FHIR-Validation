# FHIR Development and Testing (FHIR Validation) Skunkworks

This project is classed as **skunkworks** it is not built for operational use.

[Demonstration Server](http://lb-fhir-validator-924628614.eu-west-2.elb.amazonaws.com/)

This project has three purposes: 

1. To provide a FHIR Validation Service which runs either in ECS or via commandline and provides a FHIR /$validate operation 
2. To provide a FHIR Validation Service which runs as a AWS Lambda and provides a FHIR /$validate operation
3. To provide a FHIR Validation service for AWS FHIR Works (which works with Simplifier generated packages)

It has several configuration options: 

a. To validate against a supplied set of FHIR Implementation Guides (NPM packages are supplied).
b. To validate against a configured FHIR Implementation Guide (NPM package are retrieved by the service and configured via environment variables)
c. Optionally validate using the NHS England Ontology Service (configured via environment variables).

The configuration is aimed at supporting different use cases. For example the lambda version with no ontology support is aimed at performing basic FHIR validation checks. This may just be FHIR core and schema validation but can also test against UKCore profiles.


## Docker Image 

**Experimental** 

https://hub.docker.com/repository/docker/thorlogic/fhir-validator-r4/general
This should work without the configuration below
Port is expected to be 9001

`docker run --env=fhir.igs=fhir.r4.ukcore.stu3.currentbuild#0.0.8-pre-release --env=fhir.server.baseUrl=http://localhost -p 80:9001 --runtime=runc thorlogic/fhir-validator-r4:latest`

Using an ontology service 

`docker run --env=fhir.igs=fhir.r4.ukcore.stu3.currentbuild#0.0.8-pre-release --env=fhir.server.baseUrl=http://localhost --env=terminology.authorization.clientId=REPLACE_ME_CLIENT_ID --env=terminology.authorization.clientSecret=REPLACE_ME_CLIENT_SECRET --env=terminology.authorization.tokenUrl=https://ontology.nhs.uk/authorisation/auth/realms/nhs-digital-terminology/protocol/openid-connect/token --env=terminology.url=https://ontology.nhs.uk/authoring/fhir/ -p 80:9001 --runtime=runc thorlogic/fhir-validator-r4:latest`



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




