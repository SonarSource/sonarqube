#!/bin/bash

set -euo pipefail

#==============================================================================
# JVM Random Number Generation Fix
#==============================================================================
# Avoiding JVM Delays Caused by Random Number Generation
# https://docs.oracle.com/cd/E13209_01/wlcp/wlss30/configwlss/jvmrand.html
sudo sed -i 's|securerandom.source=file:/dev/random|securerandom.source=file:/dev/urandom|g' $JAVA_HOME/conf/security/java.security

#==============================================================================
# Add Required Repositories
#==============================================================================
# Install prerequisites
sudo apt-get update
sudo apt-get install -qqy wget gpg ca-certificates apt-transport-https

# Add Adoptium (Temurin) repository for Java 8
wget --max-redirect=0 -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo gpg --dearmor -o /usr/share/keyrings/adoptium.gpg
echo "deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list

# Add Google Chrome repository
wget --max-redirect=0 -q -O - https://dl.google.com/linux/linux_signing_key.pub | sudo gpg --dearmor -o /usr/share/keyrings/google-chrome.gpg
echo "deb [arch=amd64 signed-by=/usr/share/keyrings/google-chrome.gpg] https://dl.google.com/linux/chrome/deb/ stable main" | sudo tee /etc/apt/sources.list.d/google-chrome.list

#==============================================================================
# Install Required Packages
#==============================================================================
# https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=863199#23
sudo mkdir -p /usr/share/man/man1

sudo apt-get update

sudo apt-get install -qqy \
    jq \
    temurin-8-jdk \
    expect \
    xvfb \
    google-chrome-stable \
    xmlstarlet

# Clean up
sudo apt-get clean
sudo rm -rf /var/lib/apt/lists/*

#==============================================================================
# Chrome Launch Script Wrapper
#==============================================================================
sudo mkdir -p /opt/bin
sudo cp private/docker/wrap_chrome_binary /opt/bin/wrap_chrome_binary
sudo chmod +x /opt/bin/wrap_chrome_binary
sudo /opt/bin/wrap_chrome_binary

#==============================================================================
# Chrome webdriver
#==============================================================================
CHROME_DRIVER_URL=$(curl --proto '=https' "https://googlechromelabs.github.io/chrome-for-testing/last-known-good-versions-with-downloads.json" \
    | jq -r '.channels.Stable.downloads.chromedriver[] | select(.platform == "linux64").url')

wget --max-redirect=0 --no-verbose -O /tmp/chromedriver_linux64.zip $CHROME_DRIVER_URL
sudo rm -rf /opt/selenium/chromedriver
sudo mkdir -p /opt/selenium
sudo unzip /tmp/chromedriver_linux64.zip -d /opt/selenium
rm /tmp/chromedriver_linux64.zip
sudo mv /opt/selenium/chromedriver-linux64/chromedriver /usr/bin/chromedriver
sudo chmod 755 /usr/bin/chromedriver

#==============================================================================
# Atlassian Plugin SDK for Bitbucket-related integration tests
#==============================================================================
# https://community.developer.atlassian.com/t/atlassian-plugin-sdk-installation-with-apt-get-is-broken/83610
TAR_PATH=$(curl --proto '=https' -L https://marketplace.atlassian.com/download/plugins/atlassian-plugin-sdk-tgz -OJ -sw '%{filename_effective}')
sudo tar -xvzf ${TAR_PATH} -C /opt
rm $TAR_PATH
SDK_DIR=$(ls -d /opt/atlassian-plugin-sdk*)
sudo mv $SDK_DIR /opt/atlassian-plugin-sdk
sudo chmod -R 755 /opt/atlassian-plugin-sdk

# Export PATH for Atlassian Plugin SDK
export PATH="$PATH:/opt/atlassian-plugin-sdk/bin:/opt/atlassian-plugin-sdk/apache-maven-3.9.8/bin"
echo 'export PATH="$PATH:/opt/atlassian-plugin-sdk/bin:/opt/atlassian-plugin-sdk/apache-maven-3.9.8/bin"' | sudo tee -a /etc/environment
