FROM eclipse-temurin:18.0.2

VOLUME /tmp

ENV JAVA_OPTS="-Xms128m -Xmx2048m"

ADD target/fhir-validator.jar fhir-validator.jar

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/fhir-validator.jar"]


