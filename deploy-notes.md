## General 

Do this first if app has changed or app code is not present in the static folder
(may need to run `git submodule update` (maybe `git submodule foreach git pull`) and `npm install`)

Run these separately as the angular has different location for the swagger pages

### docker

`mvn clean install -P dockerBuild,dockerRelease`

### AWS ECR

`aws ecr get-login-password --region eu-west-2 | docker login --username AWS --password-stdin 365027538941.dkr.ecr.eu-west-2.amazonaws.com`

`mvn clean install -P dockerBuild,awsRelease`

Run 

`mvn spring-boot:run` and check correct app is working on http://localhost:9001

### Cloud Formation Notes

Do not use

aws cloudformation deploy --template-file C:\Development\NHSDigital\validation-service-fhir-r4\cloudfront\IOPSValidation.yaml --stack-name test-stack

