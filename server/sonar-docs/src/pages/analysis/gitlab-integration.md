---
title: GitLab Integration
url: /analysis/gitlab-integration/
---

SonarQube's integration with GitLab Self-Managed and GitLab.com allows you to maintain code quality and security in your GitLab projects.

Once you've set up your integration, you'll be able to:

- **Authenticate with GitLab** - (starting in Community Edition) Sign in to SonarQube with your GitLab credentials.
- **Import your GitLab projects** - (starting in [Developer Edition](https://redirect.sonarsource.com/editions/developer.html)) Import your GitLab Projects into SonarQube to easily set up SonarQube projects.  
- **Add merge request decoration** - (starting in Developer Edition) See your Quality Gate and code metric results right in GitLab so you know if it's safe to merge your changes.
- **Analyze projects with GitLab CI/CD** - SonarScanners running in GitLab CI/CD jobs can automatically detect branches or merge requests being built so you don't need to specifically pass them as parameters to the scanner.

## Prerequisites
- Integration with GitLab Self-Managed requires at least GitLab Self-Managed version 11.7.

## Authenticating with GitLab
You can delegate authentication to GitLab using a dedicated GitLab OAuth application.

### Creating a GitLab OAuth app
You can find general instructions for creating a GitLab OAuth app [here](https://docs.gitlab.com/ee/integration/oauth_provider.html).

Specify the following settings in your OAuth app:

- **Name** – your app's name, such as SonarQube.
- **Redirect URI** – enter your SonarQube URL with the path `/oauth2/callback/gitlab`. For example, `https://sonarqube.mycompany.com/oauth2/callback/gitlab`.
- **Scopes** – select **api** if you plan to enable group synchronization. Select **read_user** if you only plan to delegate authentication.

After saving your application, GitLab takes you to the app's page. Here you find your **Application ID** and **Secret**. Keep these handy, open your SonarQube instance, and navigate to **Administration > Configuration > General Settings > ALM Integrations > GitLab > Authentication**. Set the following settings to finish setting up GitLab authentication:

- **Enabled** – set to `true`.
- **Application ID** – the Application ID is found on your GitLab app's page.
- **Secret** – the Secret is found on your GitLab app's page.

On the login form, the new "Log in with GitLab" button allows users to connect with their GitLab accounts.

### GitLab group synchronization
Enable **Synchronize user groups** at **Administration > Configuration > General Settings > ALM Integrations > GitLab** to associate GitLab groups with existing SonarQube groups of the same name. GitLab users inherit membership to subgroups from parent groups. 

To synchronize a GitLab group or subgroup with a SonarQube group, name the SonarQube group with the full path of the GitLab group or subgroup URL. 

For example, with the following GitLab group setup:

- GitLab group = My Group
- GitLab subgroup = My Subgroup
- GitLab subgroup URL = `https://YourGitLabURL.com/my-group/my-subgroup`

You should name your SonarQube group `my-group` to synchronize it with your GitLab group and `my-group/my-subgroup` to synchronize it with your GitLab subgroup.

## Importing your GitLab projects into SonarQube
Setting up project import with GitLab allows you to easily create SonarQube projects from your GitLab projects. This is also the first step in adding merge request decoration.

To import your GitLab projects into SonarQube, you need to first set your global SonarQube settings. Navigate to **Administration > Configuration > General Settings > ALM Integrations**, select the **GitLab** tab, and specify the following settings:
 
- **Configuration Name** (Enterprise and Data Center Edition only) – The name used to identify your GitLab configuration at the project level. Use something succinct and easily recognizable.
- **GitLab URL** – The GitLab API URL.
- **Personal Access Token** – A GitLab user account is used to decorate Merge Requests. We recommend using a dedicated GitLab account with at least **Reporter** [permissions](https://docs.gitlab.com/ee/user/permissions.html) (the account needs permission to leave comments). You need a personal access token from this account with the scope authorized for **api** for the repositories that will be analyzed.

## Adding merge request decoration to GitLab

Merge request decoration shows your Quality Gate and analysis metrics directly in GitLab:

![pull request decoration](/images/github-branch-decoration.png)

[[info]]
| To decorate merge requests, a SonarQube analysis needs to be run on your code. You can find the additional parameters required for merge request analysis on the [Pull Request Analysis](/analysis/pull-request/) page.

After you've set up SonarQube to import your GitLab projects as shown in the previous section, the simplest way to add merge request decoration is by importing a project from GitLab:

![import a GitLab project](/images/add-gitlab-project.png)

Follow the steps in SonarQube to automatically set your project settings for merge request decoration. When creating your project, you'll need to provide a personal access token from your user account with the **read_api** scope. This personal access token will be stored in SonarQube until you revoke it on the GitLab side.

### Adding merge request decoration to a manually created or existing project
To add merge request decoration to a manually created or existing project, after you've set your global ALM Integration settings as shown above, set your project-level settings at **Project Settings > General Settings > Pull Request Decoration**. 

From here, set your: 
- **Configuration name** – The configuration name that corresponds to your GitHub instance. 
- **Repository identifier** – The path of your repository URL.

### Advanced merge request decoration configuration

[[collapse]]
| ## **Configuring multiple ALM instances**
|You can decorate merge requests from multiple ALM instances by creating a configuration for each ALM instance and then assigning that instance configuration to the appropriate projects. 
|
|- As part of [Developer Edition](https://redirect.sonarsource.com/editions/developer.html), you can create one configuration for each ALM. 
|
|- Starting in [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html), you can create multiple configurations for each ALM. If you have multiple configurations of the same ALM connected to SonarQube, you have to create projects manually.

[[collapse]]
| ## **Linking issues**
| During pull request decoration, individual issues will be linked to their SonarQube counterparts automatically. For this to work correctly, you need to set the instance's **Server base URL** (**[Administration > Configuration > General Settings > General > General](/#sonarqube-admin#/admin/settings/)**) correctly. Otherwise, the links will default to `localhost`.

## Analyzing projects with GitLab CI/CD
SonarScanners running in GitLab CI/CD jobs can automatically detect branches or merge requests being built so you don't need to specifically pass them as parameters to the scanner.

[[warning]]
| You need to disable git shallow clone to make sure the scanner has access to all of your history when running analysis with GitLab CI/CD. For more information, see [Git shallow clone](https://docs.gitlab.com/ee/user/project/pipelines/settings.html#git-shallow-clone).

### Activating builds  
Set up your build according to your SonarQube edition:

- **Community Edition** – Community Edition doesn't support multiple branches, so you should only analyze your main branch. You can restrict analysis to your main branch by adding the branch name to the `only` parameter in your .yml file.

- **Developer Edition and above** By default, GitLab will build all branches but not Merge Requests. To build Merge Requests, you need to update the `.gitlab-ci.yml` file by adding `merge_requests` to the `only` parameter in your .yml. See the example configurations below for more information.

### Setting environment variables 
You can set environment variables securely for all pipelines in GitLab's settings. See [Creating a Custom Environment Variable](https://docs.gitlab.com/ee/ci/variables/#creating-a-custom-environment-variable) for more information.
 
You need to set the following environment variables in GitLab for analysis:
 
- `SONAR_TOKEN` – Generate a SonarQube [token](/user-guide/user-token/) for GitLab and create a custom environment variable in GitLab with `SONAR_TOKEN` as the **Key** and the token you generated as the **Value**. 

- `SONAR_HOST_URL` – Create a custom environment variable with `SONAR_HOST_URL` as the **Key** and your SonarQube server URL as the **Value**.

### Configuring your gitlab-ci.yml file
The following examples show you how to configure your GitLab CI/CD `gitlab-ci.yml` file. The `allow_failure` parameter in the examples allows a job to fail without impacting the rest of the CI suite.

Click the scanner you're using below to expand the example configuration:

[[collapse]]
| ## SonarScanner for Gradle
| ```
| sonarqube-check:
|   image: gradle:jre11-slim
|   variables:
|     SONAR_USER_HOME: "${CI_PROJECT_DIR}/.sonar"  # Defines the location of the analysis task cache
|     GIT_DEPTH: "0"  # Tells git to fetch all the branches of the project, required by the analysis task
|   cache:
|     key: "${CI_JOB_NAME}"
|     paths:
|       - .sonar/cache
|   script: gradle sonarqube -Dsonar.qualitygate.wait=true
|   allow_failure: true
|   only:
|     - merge_requests
|     - master
|     - develop
| ```
 
[[collapse]]
| ## SonarScanner for Maven
| 
| ```
| sonarqube-check:
|   image: maven:3.6.3-jdk-11
|   variables:
|     SONAR_USER_HOME: "${CI_PROJECT_DIR}/.sonar"  # Defines the location of the analysis task cache
|     GIT_DEPTH: "0"  # Tells git to fetch all the branches of the project, required by the analysis task
|   cache:
|     key: "${CI_JOB_NAME}"
|     paths:
|       - .sonar/cache
|   script:
|     - mvn verify sonar:sonar -Dsonar.qualitygate.wait=true
|   allow_failure: true
|   only:
|     - merge_requests
|     - master
|     - develop
| ```

[[collapse]]
| ## SonarScanner CLI
| 
| ```
| sonarqube-check:
|   image:
|     name: sonarsource/sonar-scanner-cli:latest
|     entrypoint: [""]
|   variables:
|     SONAR_USER_HOME: "${CI_PROJECT_DIR}/.sonar"  # Defines the location of the analysis task cache
|     GIT_DEPTH: "0"  # Tells git to fetch all the branches of the project, required by the analysis task
|   cache:
|     key: "${CI_JOB_NAME}"
|     paths:
|       - .sonar/cache
|   script:
|     - sonar-scanner -Dsonar.qualitygate.wait=true
|   allow_failure: true
|   only:
|     - merge_requests
|     - master
|     - develop
| ```
|
| **Note:** A project key has to be provided through `sonar-project.properties` or through the command line parameter. For more information, see the [SonarScanner](/analysis/scan/sonarscanner/) documentation.

#### **Failing the pipeline job when the Quality Gate fails**
In order for the Quality Gate to fail on the GitLab side when it fails on the SonarQube side, the scanner needs to wait for the SonarQube Quality Gate status. To enable this, set the `sonar.qualitygate.wait=true` parameter in the `.gitlab-ci.yml` file. 

You can set the `sonar.qualitygate.timeout` property to an amount of time (in seconds) that the scanner should wait for a report to be processed. The default is 300 seconds. 

### For more information
For more information on configuring your build with GitLab CI/CD, see the [GitLab CI/CD Pipeline Configuration Reference](https://gitlab.com/help/ci/yaml/README.md).