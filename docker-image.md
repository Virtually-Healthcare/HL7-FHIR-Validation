
## Docker Image 

**Experimental** 

https://hub.docker.com/repository/docker/thorlogic/fhir-validator-r4/general
This should work without the configuration below
Port is expected to be 9001

`docker run --env=fhir.igs=fhir.r4.ukcore.stu3.currentbuild#0.0.8-pre-release --env=fhir.server.baseUrl=http://localhost -p 80:9001 --runtime=runc thorlogic/fhir-validator-r4:latest`

Using an ontology service 

`docker run --env=fhir.igs=fhir.r4.ukcore.stu3.currentbuild#0.0.8-pre-release --env=fhir.server.baseUrl=http://localhost --env=terminology.authorization.clientId=REPLACE_ME_CLIENT_ID --env=terminology.authorization.clientSecret=REPLACE_ME_CLIENT_SECRET --env=terminology.authorization.tokenUrl=https://ontology.nhs.uk/authorisation/auth/realms/nhs-digital-terminology/protocol/openid-connect/token --env=terminology.url=https://ontology.nhs.uk/authoring/fhir/ -p 80:9001 --runtime=runc thorlogic/fhir-validator-r4:latest`

