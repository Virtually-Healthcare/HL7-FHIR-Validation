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




## Variables

FHIR packages: [manifest.json](https://github.com/NHSDigital/IOPS-FHIR-Validation-Service/blob/main/src/main/resources/manifest.json)
HAPI Update: [pom.xml](https://github.com/NHSDigital/IOPS-FHIR-Validation-Service/blob/main/pom.xml)

# Custom Checks
Cusom error messages can be found within  [/src/main/kotlin/uk/nhs/england/fhirvalidator/provider/ValidateR4Provider.kt](https://github.com/NHSDigital/IOPS-FHIR-Validation-Service/blob/update/6.8.3/src/main/kotlin/uk/nhs/england/fhirvalidator/provider/ValidateR4Provider.kt)
# Validator Update Guide

## Prerequisites

Installation of the following on a local machine

- [Maven](https://maven.apache.org/) 
  - Installation (Debian): [How to Install Apache Maven on Debian 11](https://www.itzgeek.com/how-tos/linux/debian/how-to-install-apache-maven-on-debian-11.html)

- [Docker](https://www.docker.com/) 
  - Installation (Debian): [Install Docker Engine](https://docs.docker.com/engine/install/)  
*Note:*If the os is a fork from Debian, e.g. LMDE, replace $(. /etc/os-release && echo “$VERSION_CODENAME”) with $(echo <Debian- version-codename>)
Where <Debian-version-codename> e.g. bookworm (lowercase) for Debian.

- [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)
- [AWS Toolkit (optional)](https://aws.amazon.com/search/?searchQuery=toolkit)

- HAPI-FHIR
  - The latest release of hapi-fhir can be found at [github.com/hapifhir/hapi-fhir](https://github.com/hapifhir/hapi-fhir) 
  - The changelog can be found at [hapifhir.io/hapi-fhir/docs/introduction/changelog](https://hapifhir.io/hapi-fhir/docs/introduction/changelog.html) 

## Update AWS Server

- Go to https://github.com/NHSDigital/IOPS-FHIR-Validation-Service repo
- Create a new branch named update/<hapi-fhir version-number>
- edit pom.xml
  - Set the <fhir.version> to the relevant hapi-fhir version
  - Set the <version> to the same hapi-fhir version.

- Within the validator folder run the following:
  - $ mvn clean install
  - $ docker build -t fhir-validator-r4 .
  - $ docker tag fhir-validator-r4:latest <account id>.dkr.ecr.eu-west-2.amazonaws.com/fhir-validator-r4:<hapi-fhir version-number>
  - $ docker tag fhir-validator-r4:latest <account id>.dkr.ecr.eu-west-2.amazonaws.com/fhir-validator-r4:latest

- Login in to the AWS website
- Click ‘Command line or programmatic access’ within NHS Digital IOPS FHIR dev
- Sign into AWS using either:
  - Copy option 1 if using bash / windows / powershell
  - Copy Option 2 into credentials.txt if you have aws toolkit installed

- run
  - $ aws get-login-password –region eu-west-2 | docker login –username AWS –password stdin <account id>.dkr.ecr.eu-west-2.amazonaws.com
  - $ docker push <account id>.dkr.ecr.eu-west-2.amazonaws.com/fhir-validator-r4:[hapi-fhir version-number]

- In AWS go to ‘Management console’
  - Search for ECR 
  - Ensure the server is set to ‘eu-west-2’
  - On the LHS choose repositories
  - Within Private repositories choose ‘fhir-validator-r4’
  - Ensure the latest image is the recently uploaded version.

  - Search for ECS 
  - Go to ‘Task definitions’ (on LHS)
  - Choose ‘iops-fhir-validation-service’
  - Choose latest revision
  - Choose ‘Create new revision’
  - Go to ‘Container-1’ section
  - Update the version number within the Image URI
  - Click ‘Create’

  - Go to Clusters (on LHS)
  - Choose ‘iops-fhir-r4’
  - Choose ‘svc-fhir-validator’
  - Click ‘Update service’
  - Change ‘Revision’ to the latest version
  - Click ‘Update’
This will start the checks. Logs can be found within CloudWatch. ECS is set up for 1 task only so the previous task will shut down automatically.

## Create PR with new update
Once the validator update has been checked to ensure no issues with the build create a GitHub PR and get it approved. Once approved create a new release and detail the changes made.

# Run Validator instance on local machine
$ mvn spring-boot:run
