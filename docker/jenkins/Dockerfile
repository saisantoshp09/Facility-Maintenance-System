FROM jenkins/jenkins:lts-jdk21

USER root

# Node 20 LTS (Angular 15 build), Chromium (Karma ChromeHeadless) and Git
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates gnupg git chromium \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*

ENV CHROME_BIN=/usr/bin/chromium

USER jenkins

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt
