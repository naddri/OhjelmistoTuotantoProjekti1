FROM eclipse-temurin:21-jdk

WORKDIR /app

# 1. Install GUI libraries (needed to render the JavaFX window over X11)
RUN apt-get update && apt-get install -y \
    libx11-6 libxext6 libxrender1 libxtst6 libxi6 libgtk-3-0 mesa-utils wget unzip \
    && rm -rf /var/lib/apt/lists/*

# 2. Download JavaFX SDK (linux build, matches javafx.version in pom.xml)
RUN mkdir -p /javafx-sdk \
    && wget -O javafx.zip https://download2.gluonhq.com/openjfx/21.0.4/openjfx-21.0.4_linux-x64_bin-sdk.zip \
    && unzip javafx.zip -d /javafx-sdk \
    && rm -rf javafx.zip

# 3. Copy fat jar (run `mvn clean package` locally first)
COPY target/flashcard-app-1.0-SNAPSHOT.jar app.jar

# Set DISPLAY for Windows (Xming)
ENV DISPLAY=host.docker.internal:0.0

# Run JavaFX app
CMD ["java", \
     "--module-path", "/javafx-sdk/javafx-sdk-21.0.4/lib", \
     "--add-modules", "javafx.controls", \
     "-Dprism.order=sw", \
     "-Dprism.verbose=true", \
     "-cp", "app.jar", "com.flashcards.FlashcardApp"]

# how to run
# mvn clean package
# docker build -t flashcard-app .
# docker run --rm -e DISPLAY=host.docker.internal:0.0 flashcard-app