---
title: Analysis Parameters
url: /analysis/analysis-parameters/
---

Project analysis settings can be configured in multiple places. Here is the hierarchy:

* Global properties, defined in the UI, apply to all projects (From the top bar, go to **[Administration > Configuration > General Settings](/#sonarqube-admin#/admin/settings)**)
* Project properties, defined in the UI, override global property values (At a project level, go to **Project Settings > General Settings**)
* Project analysis parameters, defined in a project analysis configuration file or scanner configuration file, override the ones defined in the UI
* Analysis / Command line parameters, defined when launching an analysis (with `-D` on the command line), override project analysis parameters

Note that only parameters set through the UI are stored in the database.
For example, if you override the `sonar.exclusions` parameter via command line for a specific project, it will not be stored in the database. Subsequent analyses, or analyses in SonarLint with connected mode, would still be executed with the exclusions defined in the UI and therefore stored in the DB.

Most of the property keys shown in the interface at both global and project levels can also be set as analysis parameters, but the parameters listed below can _only_ be set at analysis time. 

For language-specific parameters related to test coverage and execution, see [Test Coverage](/analysis/test-coverage/overview/).
For language-specific parameters related to external issue reports, see [External Issues](/analysis/external-issues/).

[[info]]
| Analysis parameters are case-sensitive.

## Mandatory Parameters

### Server
Key | Description | Default
---|----|---
`sonar.host.url`| the server URL | http://localhost:9000

### Project Configuration
Key | Description | Default
---|----|---
`sonar.projectKey`|The project's unique key. Allowed characters are: letters, numbers, `-`, `_`, `.` and `:`, with at least one non-digit. | For Maven projects, this defaults to `<groupId>:<artifactId>`

## Optional Parameters

### Project Identity
Key | Description | Default
---|----|---
`sonar.projectName`|Name of the project that will be displayed on the web interface.|`<name>` for Maven projects, otherwise project key. If not provided and there is already a name in the DB, it won't be overwritten
`sonar.projectVersion` | The project version. | `<version>` for Maven projects, otherwise "not provided"

### Authentication
By default, user authentication is required to prevent anonymous users from browsing and analyzing projects on your instance, and you need to pass these parameters when running analyses. Authentication is enforced in the global Security(/instance-administration/security/) settings.

When authentication is required or the "Anyone" pseudo-group does not have permission to perform analyses, you'll need to supply the credentials of a user with Execute Analysis permissions for the analysis to run under.

Key | Description                                                                                                                                                                                   | Default
---|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---
`sonar.login` | The [authentication token](/user-guide/user-token/) or login of a SonarQube user with either Execute Analysis permission on the project or Global Execute Analysis permission. |
`sonar.password` | If you're using an authentication token, leave this blank. If you're using a login, this is the password that goes with your `sonar.login` username.                                          |

### Web Services
Key | Description | Default
---|----|---
`sonar.ws.timeout` | Maximum time to wait for the response of a Web Service call (in seconds). Modifying this value from the default is useful only when you're experiencing timeouts during analysis while waiting for the server to respond to Web Service calls. |  60

### Project Configuration
Key | Description | Default
---|----|---
`sonar.projectDescription` | The project description. | `<description>` for Maven projects
`sonar.links.homepage` | Project home page. | `<url>` for Maven projects
`sonar.links.ci` | Continuous integration. | `<ciManagement><url>` for Maven projects
`sonar.links.issue` | Issue tracker. | `<issueManagement><url>` for Maven projects
`sonar.links.scm` | Project source repository. | `<scm><url>` for Maven projects
`sonar.sources` | Comma-separated paths to directories containing main source files. | Read from build system for Maven, Gradle, MSBuild projects. Defaults to project base directory when neither `sonar.sources` nor `sonar.tests` is provided.
`sonar.tests` | Comma-separated paths to directories containing test source files. | Read from build system for Maven, Gradle, MSBuild projects. Else default to empty.
`sonar.sourceEncoding` | Encoding of the source files. Ex: `UTF-8`, `MacRoman`, `Shift_JIS`. This property can be replaced by the standard property `project.build.sourceEncoding` in Maven projects. The list of available encodings depends on your JVM. | System encoding
`sonar.externalIssuesReportPaths` | Comma-delimited list of paths to Generic Issue reports. | 
`sonar.projectDate` | Assign a date to the analysis. This parameter is only useful when you need to retroactively create the history of a not-analyzed-before project. The format is `yyyy-MM-dd`, for example: 2010-12-01. Since you cannot perform an analysis dated prior to the most recent one in the database, you must analyze recreate your project history in chronological order, oldest first. ![](/images/exclamation.svg) Note: You may need to adjust your housekeeping settings if you wish to create a long-running history. | Current date
`sonar.projectBaseDir` | Use this property when you need analysis to take place in a directory other than the one from which it was launched. E.G. analysis begins from `jenkins/jobs/myjob/workspace` but the files to be analyzed are in `ftpdrop/cobol/project1`. The path may be relative or absolute. Specify not the the source directory, but some parent of the source directory. The value specified here becomes the new "analysis directory", and other paths are then specified as though the analysis were starting from the specified value of `sonar.projectBaseDir`. Note that the analysis process will need write permissions in this directory; it is where the `sonar.working.directory` will be created. |
`sonar.working.directory` | Set the working directory for an analysis triggered with the SonarScanner or the SonarScanner for Ant (versions greater than 2.0). This property is not compatible with the SonarScanner for MSBuild. Path must be relative, and unique for each project. ![](/images/exclamation.svg) Beware: the specified folder is deleted before each analysis. | `.scannerwork`
`sonar.scm.provider` | This property can be used to explicitly tell SonarQube which SCM you're using on the project (in case auto-detection doesn't work). The value of this property is always lowercase and depends on the SCM (ex. "git" if you're using Git). Check the [SCM integration](/analysis/scm-integration/) documentation for more. |  
`sonar.scm.forceReloadAll` | By default, blame information is only retrieved for changed files. Set this property to `true` to load blame information for all files. This can be useful is you feel that some SCM data is outdated but SonarQube does not get the latest information from the SCM engine. | 
`sonar.scm.exclusions.disabled`| For supported engines, files ignored by the SCM, i.e. files listed in `.gitignore`, will automatically be ignored by analysis too. Set this property to `true` to disable that feature. SCM exclusions are always disabled if `sonar.scm.disabled` is set to `true`. |
`sonar.scm.revision`| Overrides the revision, for instance the Git sha1, displayed in analysis results. By default value is provided by the CI environment or guessed by the checked-out sources.| 
`sonar.buildString`| The string passed with this property will be stored with the analysis and available in the results of `api/project_analyses/search`, thus allowing you to later identify a specific analysis and obtain its ID for use with `api/project_analyses/set_baseline`. | |
`sonar.analysis.[yourKey]`| This property stub allows you to insert custom key/value pairs into the analysis context, which will also be passed forward to [webhooks](/project-administration/webhooks/). | |
`sonar.newCode.referenceBranch`| Defines the new code period for this branch to use the given branch as a reference branch, overriding the server's setting. See [New Code Period](/project-administration/new-code-period/). 

### Duplications
Key | Description | Default
---|----|---
`sonar.cpd.${language}.minimumtokens` | A piece of code is considered duplicated as soon as there are at least 100 duplicated tokens in a row (override with `sonar.cpd.${language}.minimumTokens`) spread across at least 10 lines of code (override with `sonar.cpd.${language}.minimumLines`). For Java projects, a piece of code is considered duplicated when there is a series of at least 10 statements in a row, regardless of the number of tokens and lines. This threshold cannot be overridden.  | 100
`sonar.cpd.${language}.minimumLines` | (see above) | 10


### Analysis Logging
Key | Description | Default
---|----|---
`sonar.log.level` | Control the quantity / level of logs produced during an analysis. `DEBUG`: Display `INFO` logs + more details at `DEBUG` level. Similar to `sonar.verbose=true`. `TRACE`: Display `DEBUG` logs + the timings of all ElasticSearch queries and Web API calls executed by the SonarScanner. | `INFO`
`sonar.verbose` | Add more detail to both client and server-side analysis logs. Activates `DEBUG` mode for the scanner, and adds client-side environment variables and system properties to server-side log of analysis report processing. ![](/images/exclamation.svg)NOTE: There is the potential for this setting to expose sensitive information such as passwords if they are stored as server-side environment variables. | false
`sonar.scanner.dumpToFile` | Outputs to the specified file the full list of properties passed to the scanner API as a means to debug analysis. |  
`sonar.scanner.metadataFilePath` | Set the location where the scanner writes the `report-task.txt` file containing among other things the `ceTaskId`. | value of `sonar.working.directory`

### Quality Gate 
Key | Description | Default
---|----|---
`sonar.qualitygate.wait` | Forces the analysis step to poll the SonarQube instance and wait for the Quality Gate status. If there are no other options, you can use this to fail a pipeline build when the Quality Gate is failing. See the [CI Integration](/analysis/ci-integration-overview/) page for more information. | 
`sonar.qualitygate.timeout` | Sets the number of seconds that the scanner should wait for a report to be processed. | 300

### Deprecated
[[danger]]
| These parameters are listed for completeness, but are deprecated and should not be used in new analyses.

Key | Description
---|----|--- 
`sonar.links.scm_dev` **![](/images/cross.svg)Deprecated since SQ 7.1** | Developer connection. | `<scm><developerConnection>` for Maven projects
