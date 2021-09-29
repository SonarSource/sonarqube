---
title: Codemagic Integration
url: /analysis/codemagic/
---

SonarScanners running in Codemagic can automatically detect branches and merge or pull requests in certain jobs. You don't need to explicitly pass the branch or pull request details.

## Adding SonarQube scripts to your Codemagic .yml file
To analyze your code when using Codemagic, you need to add the following scripts to your existing `codemagic.yaml` file:

```
    scripts:   
      - |
        # download and install SonarQube
        wget -O $FCI_BUILD_DIR/sonar-scanner.zip https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-4.4.0.2170-macosx.zip
        unzip $FCI_BUILD_DIR/sonar-scanner.zip
        mv sonar-scanner-* sonar-scanner
      - |
        # Generate and upload code analysis report
        export PATH=$PATH:$FCI_BUILD_DIR/sonar-scanner/bin
        sonar-scanner \
        -Dsonar.projectKey=YOUR_PROJECT_KEY \
        -Dsonar.host.url=SONARQUBE_URL \
```

You also need to define `SONAR_TOKEN` as a Codemagic environment variable.

## Automatically detecting pull requests
For SonarQube to automatically detect pull requests when using Codemagic, you need to add an event in the triggering section of your `codemagic.yaml` file as shown in the following snippet:
```
    triggering:
      events:
        - pull_request
```

For triggering to work, you also need to set up a link between Codemagic and your DevOps platform (Bitbucket, Github, etc.). See the [Codemagic documentation](https://docs.codemagic.io/configuration/webhooks/) for more information.

## Caching the .sonar folder

Caching the `.sonar` folder saves time on subsequent analyses. To do this, add the following snippet to your `codemagic.yaml` file:

```
    cache:
      cache_paths:
        - ~/.sonar
```

