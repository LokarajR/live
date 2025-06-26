FROM openjdk:17

WORKDIR /app

COPY . .

RUN javac Main.java

EXPOSE 8080

CMD ["java", "Main"]
