FROM openjdk:23

VOLUME /tmp

ENV JAVA_OPTS="-Xms128m -Xmx8192m"

ADD target/fhir-validator.jar fhir-validator.jar

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/fhir-validator.jar"]


