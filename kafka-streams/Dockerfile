FROM maven:3.5-jdk-8 AS build  
COPY src /usr/src/app/src  
COPY pom.xml /usr/src/app  
RUN mvn -f /usr/src/app/pom.xml clean package assembly:single

FROM gcr.io/distroless/java  
COPY --from=build /usr/src/app/target/pizzashop-0.0.1-jar-with-dependencies.jar /usr/app/pizzashop.jar 
EXPOSE 8080  
ENTRYPOINT ["java","-cp","/usr/app/pizzashop.jar", "pizzashop.OrdersProductsJoin"]  