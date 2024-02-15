## AWS 

Initial user Setup

https://nhsd-confluence.digital.nhs.uk/display/AWS/001+-+Use+AWS+CLI+with+MFA

Logon using MFA - MFA needs replacing in the following

aws --profile kevin.mayfield1 sts get-session-token --serial-number arn:aws:iam::347250048819:mfa/kevin.mayfield1 --duration-seconds 129600 --token-code MFA

aws configure --profile default set aws_access_key_id {from above}
aws configure --profile default set aws_secret_access_key {from above}
aws configure --profile default set aws_session_token

Then run

aws sts assume-role --role-arn arn:aws:iam::365027538941:role/NHSDAdminRole --role-session-name bob

And repeat configure step?

aws ecr get-login-password --region eu-west-2 | docker login --username AWS --password-stdin 365027538941.dkr.ecr.eu-west-2.amazonaws.com

Think we need to output the angular app to resources static folder

ng build --configuration production --output-path ../src/main/resources/public --base-href /


mvn clean install

ng build --configuration production --output-path ../src/main/resources/static --base-href ./


docker build -t fhir-validator-r4 .

docker tag fhir-validator-r4:latest 365027538941.dkr.ecr.eu-west-2.amazonaws.com/fhir-validator-r4:latest
docker tag fhir-validator-r4:latest 365027538941.dkr.ecr.eu-west-2.amazonaws.com/fhir-validator-r4:6.10.33

docker push 365027538941.dkr.ecr.eu-west-2.amazonaws.com/fhir-validator-r4:6.10.33
docker push 365027538941.dkr.ecr.eu-west-2.amazonaws.com/fhir-validator-r4:latest

## Docker

ng build --configuration docker --output-path ../src/main/resources/static --base-href ./

mvn clean install

docker build -t fhir-validator-r4 .

docker tag fhir-validator-r4:latest thorlogic/fhir-validator-r4:latest
docker tag fhir-validator-r4:latest thorlogic/fhir-validator-r4:6.10.33

docker push thorlogic/fhir-validator-r4:latest
docker push thorlogic/fhir-validator-r4:6.10.33

### Cloud Formation Notes

Do not use

aws cloudformation deploy --template-file C:\Development\NHSDigital\validation-service-fhir-r4\cloudfront\IOPSValidation.yaml --stack-name test-stack

