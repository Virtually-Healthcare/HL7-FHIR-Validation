### IOPS FHIR Validator

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




