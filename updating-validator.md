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
