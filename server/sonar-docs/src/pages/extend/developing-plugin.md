---
title: Plugin basics
url: /extend/developing-plugin/
---

## Building your plugin

### Prerequisites
To build a plugin, you need Java 8 and Maven 3.1 (or greater). Gradle can also be used thanks to https://github.com/iwarapter/gradle-sonar-packaging-plugin. Note that this Gradle plugin is not officially supported by SonarSource.

### Create a Maven Project
The recommended way to start is by duplicating the plugin example project: https://github.com/SonarSource/sonar-custom-plugin-example.

If you want to start the project from scratch, use the following Maven pom.xml template:

[[collapse]]
| ## pom.xml
| ```
| <?xml version="1.0" encoding="UTF-8"?>
| <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
|   <modelVersion>4.0.0</modelVersion>
|   <groupId>YOUR_GROUP_ID</groupId>
|   <!-- it's recommended to follow the pattern "sonar-{key}-plugin", for example "sonar-myphp-plugin" -->
|   <artifactId>YOUR_ARTIFACT_ID</artifactId>
|   <version>YOUR_VERSION</version>
|   
|   <!-- this is important for sonar-packaging-maven-plugin -->
|   <packaging>sonar-plugin</packaging>
|  
|   <dependencies>
|     <dependency>
|       <groupId>org.sonarsource.sonarqube</groupId>
|       <artifactId>sonar-plugin-api</artifactId>
|       <!-- minimal version of SonarQube to support. -->
|       <version>6.7</version>
|       <!-- mandatory scope -->
|       <scope>provided</scope>
|     </dependency>
|   </dependencies>
|  
|   <build>
|     <plugins>
|       <plugin>
|         <groupId>org.sonarsource.sonar-packaging-maven-plugin</groupId>
|         <artifactId>sonar-packaging-maven-plugin</artifactId>
|         <version>1.18.0.372</version>
|         <extensions>true</extensions>
|         <configuration>
|           <!-- the entry-point class that extends org.sonar.api.SonarPlugin -->
|           <pluginClass>com.mycompany.sonar.reference.ExamplePlugin</pluginClass>
|            
|           <!-- advanced properties can be set here. See paragraph "Advanced Build Properties". -->
|         </configuration>
|       </plugin>
|     </plugins>
|   </build>
| </project>
| ```

### Build
To build your plugin project, execute this command from the project root directory:  
`mvn clean package`  
The plugin jar file is generated in the project's `target/` directory.

### Deploy
**"Cold" Deploy**  
The standard way to install the plugin for regular users is to copy the JAR artifact, from the `target/` directory  to the `extensions/plugins/` directory of your SonarQube installation then start the server. The file `logs/sonar.log` will then contain a log line similar to:  
`Deploy plugin Example Plugin / 0.1-SNAPSHOT`  
Scanner extensions such as sensors are immediately retrieved and loaded when scanning source code. 

### Debug
**Debugging web server extensions**  

1. Edit conf/sonar.properties and set: `sonar.web.javaAdditionalOpts=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8000`
1. Install your plugin by copying its JAR file to extensions/plugins
1. Start the server. The line `Listening for transport dt_socket at address: 5005` is logged in  `logs/sonar.log`.
1. Attach your IDE to the debug process (listening on port 8000 in the example)

**Debugging compute engine extensions**  
Same procedure as for web server extensions (see previous paragraph), but with the property: `sonar.ce.javaAdditionalOpts=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8000`

**Debugging scanner extensions**  
```
export SONAR_SCANNER_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8000"
cd /path/to/project
sonar-scanner 
```
When using the Scanner for Maven, then simply execute:
```
cd /path/to/project
mvnDebug sonar:sonar
# debug port is 8000
```

### Advanced Build Properties
Plugin properties are defined in the file `META-INF/MANIFEST.MF` of the plugin .jar file.

Most of them are defined through the `<configuration>` section of the [sonar-packaging-maven-plugin](https://jira.sonarsource.com/browse/PACKMP). Some are taken from standard pom nodes Effective values are listed at the end of the build log:
```
[INFO] --- sonar-packaging-maven-plugin:1.15:sonar-plugin (default-sonar-plugin) @ sonar-widget-lab-plugin ---
[INFO] -------------------------------------------------------
[INFO] Plugin definition in Marketplace
[INFO]     Key: widgetlab
[INFO]     Name: Widget Lab
[INFO]     Description: Additional widgets
[INFO]     Version: 1.9-SNAPSHOT
[INFO]     Entry-point Class: org.codehaus.sonar.plugins.widgetlab.WidgetLabPlugin
[INFO]     Required Plugins:
[INFO]     Use Child-first ClassLoader: false
[INFO]     Base Plugin:
[INFO]     Homepage URL: https://redirect.sonarsource.com/plugins/widgetlab.html
[INFO]     Minimal SonarQube Version: 4.5.1
[INFO]     Licensing: GNU LGPL 3
[INFO]     Organization: Shaw Industries
[INFO]     Organization URL: http://shawfloors.com
[INFO]     Terms and Conditions:
[INFO]     Issue Tracker URL: http://jira.codehaus.org/browse/SONARWIDLB
[INFO]     Build date: 2015-12-15T18:28:54+0100
[INFO]     Sources URL: https://github.com/SonarCommunity/sonar-widget-lab
[INFO]     Developers: G. Ann Campbell,Patroklos Papapetrou
[INFO] -------------------------------------------------------
[INFO] Building jar: /dev/sonar-widget-lab/target/sonar-widget-lab-plugin-1.9-SNAPSHOT.jar 
```

Supported standard pom node properties:

Maven Property|Manifest Key|Notes
---|---|---
`version` | Plugin-Version | (required) Plugin version as displayed in page "Marketplace". Default: ${project.version}
- | Sonar-Version | (required) Minimal version of supported SonarQube at runtime. For example if value is 5.2, then deploying the plugin on versions 5.1 and lower will fail. Default value is given by the version of sonar-plugin-api dependency. It can be overridden with the Maven property sonarQubeMinVersion (since sonar-packaging-maven-plugin 1.16). That allows in some cases to use new features of recent API and to still be compatible at runtime with older versions of SonarQube. Default: version of dependency sonar-plugin-api
`license` | Plugin-License | Plugin license as displayed in page "Marketplace". Default `${project.licenses}`
`developers` | Plugin-Developers | List of developers displayed in page "Marketplace". Default: `${project.developers}`

Supported `<configuration>` properties:

Maven Property|Manifest Key|Notes
---|---|---
`pluginKey` | Plugin-Key | (required) Contains only letters/digits and is unique among all plugins. Examples: groovy, widgetlab. Constructed from `${project.artifactId}.` Given an artifactId of: `sonar-widget-lab-plugin`, your pluginKey will be: `widgetlab`
`pluginClass` | Plugin-Class | (required) Name of the entry-point class that extends `org.sonar.api.SonarPlugin`. Example: `org.codehaus.sonar.plugins.widgetlab.WidgetLabPlugin` 
`pluginName` | Plugin-Name | (required) Displayed in the page "Marketplace". Default: `${project.name}`
`pluginDescription` | Plugin-Description | Displayed in the page "Marketplace". Default: `${project.description}`
`pluginUrl` |  Plugin-Homepage | Homepage of website, for example https://github.com/SonarQubeCommunity/sonar-widget-lab `${project.url}`
`pluginIssueTrackerUrl` |  Plugin-IssueTrackerUrl | Example: https://github.com/SonarQubeCommunity/sonar-widget-lab/issues. Default: `${project.issueManagement.url}`
`pluginTermsConditionsUrl`  |  Plugin-TermsConditionsUrl | Users must read this document when installing the plugin from Marketplace. Default: `${sonar.pluginTermsConditionsUrl}`
`useChildFirstClassLoader` | Plugin-ChildFirstClassLoader | Each plugin is executed in an isolated classloader, which inherits a shared classloader that contains API and some other classes. By default the loading strategy of classes is parent-first (look up in shared classloader then in plugin classloader). If the property is true, then the strategy is child-first. This property is mainly used when building plugin against API < 5.2, as the shared classloader contained many 3rd party libraries (guava 10, commons-lang, ...) false
`basePlugin` | Plugin-Base | If specified, then the plugin is executed in the same classloader as basePlugin.
`pluginSourcesUrl` | Plugin-SourcesUrl | URL of SCM repository for open-source plugins. Displayed in page "Marketplace". Default: `${project.scm.url}`
`pluginOrganizationName` | Plugin-Organization | Organization which develops the plugin, displayed in the page "Marketplace". Default: `${project.organization.name}`
`pluginOrganizationUrl` | Plugin-OrganizationUrl | URL of the organization, displayed in the page "Marketplace". Default: `${project.organization.url}`
`sonarLintSupported` | SonarLint-Supported | Whether the (language) plugin supports SonarLint or not. Only SonarSource analyzers (SonarJava, SonarJS, ...) and custom rules plugins for SonarSource analyzers should set this to true. 
`pluginDisplayVersion` | Plugin-Display-Version | The version as displayed in SonarQube administration console. By default it's the raw version, for example "1.2", but can be overridden to "1.2 (build 12345)" for instance. Supported in sonar-packaging-maven-plugin 1.18.0.372. Default: `${project.version}`


The Maven sonar-packaging-maven-plugin supports also these properties:

Maven Property|Manifest Key|Notes
---|---|---
`addMavenDescriptor` |Copy pom file inside the directory META-INF of generated JAR file? | Boolean. Default: `${sonar.addMavenDescriptor}` / `true`.
`skipDependenciesPackaging` | Do not copy Maven dependencies into JAR file. | Default: `${sonar.skipDependenciesPackaging} / `false`.

Other Manifest fields:  

* `Implementation-Build` - Identifier of build or commit, for example the Git sha1 "94638028f0099de59f769cdca776e506684235d6". It is displayed for debugging purpose in logs when SonarQube server starts.

## API basics

### Extension points
SonarQube provides extension points for its three technical stacks:

* Scanner, which runs the source code analysis
* Compute Engine, which consolidates the output of scanners, for example by 
   * computing 2nd-level measures such as ratings
   * aggregating measures (for example number of lines of code of project = sum of lines of code of all files)
   * assigning new issues to developers
   * persisting everything in data stores
* Web application

Extension points are not designed to add new features but to complete existing features. Technically they are contracts defined by a Java interface or an abstract class annotated with @ExtensionPoint. The exhaustive list of extension points is available in the javadoc.

The implementations of extension points (named "extensions") provided by a plugin must be declared in its entry point class, which implements org.sonar.api.Plugin and which is referenced in pom.xml:

ExamplePlugin.java
```
package org.sonarqube.plugins.example;
import org.sonar.api.Plugin;
 
public class ExamplePlugin implements Plugin {
  @Override
  public void define(Context context) {
    // implementations of extension points
    context.addExtensions(FooLanguage.class, ExampleProperties.class);
  }
}
```
pom.xml
```
<?xml version="1.0" encoding="UTF-8"?>
<project>
  ...
  <build>
    <plugins>
      <plugin>
        <groupId>org.sonarsource.sonar-packaging-maven-plugin</groupId>
        <artifactId>sonar-packaging-maven-plugin</artifactId>
        <extensions>true</extensions>
        <configuration>
          <pluginClass>org.sonarqube.plugins.example.ExamplePlugin</pluginClass>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```
### Lifecycle
A plugin extension exists only in its associated technical stacks. A scanner sensor is for example instantiated and executed only in a scanner runtime, but not in the web server nor in Compute Engine. The stack is defined by the annotations [@ScannerSide](http://javadocs.sonarsource.org/latest/apidocs/org/sonar/api/batch/ScannerSide.html), [@ServerSide](http://javadocs.sonarsource.org/latest/apidocs/index.html?org/sonar/api/server/ServerSide.html) (for web server) and [@ComputeEngineSide](http://javadocs.sonarsource.org/latest/apidocs/index.html?org/sonar/api/ce/ComputeEngineSide.html). 

An extension can call core components or another extension of the same stack. These dependencies are defined by constructor injection:

```
@ScannerSide
public class Foo {
  public void call() {}
}
 
// Sensor is a scanner extension point 
public class MySensor implements Sensor {
  private final Foo foo;
  private final Languages languages;
  
  // Languages is core component which lists all the supported programming languages.
  public MySensor(Foo foo, Languages languages) {   
    this.foo = foo;
    this.languages = languages;
  }
  
  @Override
  public void execute(SensorContext context) {
    System.out.println(this.languages.all());
    foo.call();
  }
}
 
  
public class ExamplePlugin implements Plugin {
  @Override
  public void define(Context context) {
    // Languages is a core component. It must not be declared by plugins.
    context.addExtensions(Foo.class, MySensor.class);
  }
}
```

It is recommended not to call other components in constructors. Indeed, they may not be initialized at that time. Constructors should only be used for dependency injection.

[[warning]]
| ![](/images/exclamation.svg) Compilation does not fail if incorrect dependencies are defined, such as a scanner extension trying to call a web server extension. Still it will fail at runtime when plugin is loaded.

### Third-party Libraries
Plugins are executed in their own isolated classloaders. That allows the packaging and use of 3rd-party libraries without runtime conflicts with core internal libraries or other plugins. Note that since version 5.2, the SonarQube API does not bring transitive dependencies, except SLF4J. The libraries just have to be declared in the pom.xml with default scope "compile":

pom.xml
```
<?xml version="1.0" encoding="UTF-8"?>
<project>
  ...
  <dependencies>
    ...
    <dependency>
      <groupId>commons-codec</groupId>
      <artifactId>commons-codec</artifactId>
      <version>1.10</version>
    </dependency>
 </dependencies>
</project>
```
Technically the libraries are packaged in the directory META-INF/lib of the generated JAR file. An alternative is to shade libraries, for example with maven-shade-plugin. That minimizes the size of the plugin .jar file by copying only the effective used classes.

[[info]]
| ![](/images/info.svg) The command `mvn dependency:tree` gives the list of all dependencies, including transitive ones.

### Configuration
The core component [`org.sonar.api.config.Configuration`](http://javadocs.sonarsource.org/latest/apidocs/index.html?org/sonar/api/config/Configuration.html) provides access to configuration. It deals with default values and decryption of values. It is available in all stacks (scanner, web server, Compute Engine). As recommended earlier, it must not be called from constructors.

MyExtension.java
```
public class MyRules implements RulesDefinition {
  private final Configuration config;
  
  public MyRules(Configuration config) {   
    this.config = config; 
  }
  
  @Override
  public void define(Context context) {
    int value = config.getInt("sonar.property").orElse(0);
  }
}
```
Scanner sensors can get config directly from SensorContext, without using constructor injection:

MySensor.java
```
public class MySensor extends Sensor {
  @Override
  public void execute(SensorContext context) {
    int value = context.config().getInt("sonar.property").orElse(0);
  }
}
```

In the scanner stack, properties are checked in the following order, and the first non-blank value is the one that is used:

1. System property
1. Scanner command-line (-Dsonar.property=foo for instance)
1. Scanner tool (<properties> of scanner for Maven for instance) 
1. Project configuration defined in the web UI 
1. Global configuration defined in the web UI 
1. Default value

Plugins can define their own properties so that they can be configured from web administration console. The extension point org.sonar.api.config.PropertyDefinition must be used :
```
public class ExamplePlugin implements Plugin {
  @Override
  public void define(Context context) {
    context.addExtension(
      PropertyDefinition.builder("sonar.my.property")
       .name("My Property")
       .description("This is the description displayed in web admin console")
       .defaultValue("42")
       .build()
    );
  }
}
```

[[info]]
| ![](/images/info.svg) Values of the properties suffixed with `.secured` are not available to non-authorized users (anonymous and users without project or global administration rights). `.secured` is needed for passwords, for instance.

The annotation [`@org.sonar.api.Property`](http://javadocs.sonarsource.org/latest/apidocs/index.html?org/sonar/api/Property.html) can also be used on an extension to declare a property, but org.sonar.api.config.PropertyDefinition is preferred.
```
@Properties(
    @Property(key="sonar.my.property", name="My Property", defaultValue="42")
)
public class MySensor implements Sensor {
  // ...
}
  
public class ExamplePlugin implements Plugin {
  @Override
  public void define(Context context) {
    context.addExtension(MySensor.class);
  }
}
```

### Logging
The class [`org.sonar.api.utils.log.Logger`](http://javadocs.sonarsource.org/latest/apidocs/index.html?org/sonar/api/utils/log/Logger.html) is used to log messages to scanner output, web server logs/sonar.log, or Compute Engine logs (available from administration web console). It's convenient for unit testing (see class [`LogTester`](http://javadocs.sonarsource.org/latest/apidocs/index.html?org/sonar/api/utils/log/LogTester.html)).
```
import org.sonar.api.utils.log.*;
public class MyClass {
  private static final Logger LOGGER = Loggers.get(MyClass.class);
 
  public void doSomething() {
    LOGGER.info("foo");
  }
}
```
Internally [SLF4J](http://www.slf4j.org/) is used as a facade of various logging frameworks (log4j, commons-log, logback, java.util.logging). That allows all these frameworks to work at runtime, such as when they are required for a 3rd party library. SLF4J loggers can also be used instead of org.sonar.api.utils.log.Logger. Read the [SLF4J manual](http://www.slf4j.org/manual.html) for more details.

As an exception, plugins must not package logging libraries. Dependencies like SLF4J or log4j must be declared with scope "provided".

### Exposing APIs to Other Plugins
The common use case is to write a language plugin that will allow some other plugins to contribute additional rules (see for example how it is done in SonarJava). The main plugin will expose some APIs that will be implemented/used by the "rule" plugins.

Plugins are loaded in isolated classloaders. It means a plugin can't access another plugin's classes. There is an exception for package names following pattern `org.sonar.plugins.<pluginKey>.api`. For example all classes in a plugin with the key myplugin that are located in `org.sonar.plugins.myplugin.api` are visible to other plugins.

### Serving Static Resources
If you need to serve static resources from your plugin such as images or JavaScript files, place them in a directory under `resources` named `static` (`myplugin/src/main/resources/static`). At runtime they'll be available from `http://{server}/static/{pluginKey}/{file}`. 


## Versioning and API Deprecation
### Versioning Strategy
The goal of this versioning strategy is both to:

* Release often, release early in order to get quick feedback from the SonarQube community
* Release stable versions of the SonarQube platform for companies whose main priority is to set up a very stable environment. Even if the price for such stable environments is missing out on the latest, sexy SonarQube features
* Support the API deprecation strategy (see next section)

The rules are:

* Each ~two months a new version of SonarQube is released. This version should increment the minor digit of the previous version (ex: 4.2 -> 4.3)
* After three (or more) releases, a bug-fix version is released, and becomes the new LTS. The major digit of the subsequent version is incremented to start a new cycle (ex: 5.6 -> 6.0)

And here is the strategy in action:
```
4.4 -> 4.5 -> 5.0 -> 5.1 -> 5.2 -> ... -> 5.5 -> 6.0 -> ...     <- New release every ~2 months
        |                                  |
      4.5.1 -> 4.5.2 -> ...              5.5.1 -> 5.5.2 -> ...  <- New LTS
```

### API Deprecation Strategy
The goal of this deprecation strategy is to make sure that deprecated APIs will be dropped without side-effects at a given planned date. The expected consequence of such strategy is to ease the evolution of the SonarQube API by making such refactoring painless.

The rules are:

* An API must be deprecated before being dropped
* A deprecated API must be fully supported until its drop (For instance the implementation of a deprecated method can't be replaced by `throw new UnsupportedOperationException())`
* If an API is deprecated in version X.Y, this API will be dropped in version (X+2).0. Example: an API deprecated in 4.1 is supported in 4.2, 4.3, 5.0, 5.1, 5.2, 5.3 and is dropped in version 6.0.
* According to the versioning strategy, that means that an API can remain deprecated before being dropped during 6 to 12 months.
* Any release of a SonarQube plugin must at least depend on the latest LTS version of the SonarQube API
* For each SonarQube plugin there must at least one release on each LTS version of SonarQube, which means at least one release each 6 months.
* No use of deprecated APIs is accepted when releasing a plugin. It raises a critical issue in SonarQube analysis. This issue can't be postponed.
* No deprecated API introduced 2 major versions ago is accepted when releasing SonarQube. It raises a critical issue in SonarQube analysis. This issue can't be postponed.
* An API is marked as deprecated with both:
   * the annotation @Deprecated
   * the javadoc tag @deprecated whose message must start with "in x.y", for example:
    ```
    /**
     * @deprecated in 4.2. Replaced by {@link #newMethod()}.
     */
    @Deprecated
    public void foo() {
    ```

## API Changes

### Release 7.9
No changes

### Release 7.8

![](/images/check.svg) Added
* `org.sonar.api.web.WebAnalytics`

![](/images/exclamation.svg) Deprecated
* `org.sonar.api.i18n.I18`
* `org.sonar.api.SonarQubeVersion` use `org.sonar.api.SonarRuntime` instead
* `org.sonar.api.profiles.XMLProfileParser`
* `org.sonar.api.notifications.NotificationChannel`

![](/images/cross.svg) Removed
* Pico components relying on reflection to have their `start` or `stop` method called. Make your component implements `org.sonar.api.Startable` instead.

### Release 7.7

![](/images/check.svg) Added
* ` org.sonar.api.batch.scm.ScmProvider#ignoreCommand`

![](/images/exclamation.svg) Deprecated
* `org.sonar.api.batch.fs.InputFile::status`
* `org.sonar.api.resources.Qualifiers#BRC`

![](/images/cross.svg) Removed
* The preview/issues mode of scanner has been removed

### Release 7.6

![](/images/info.svg) Changed

* `PostJob` moved to project level IoC container
* `InputFileFilter` moved to project level IoC container

![](/images/check.svg) Added

* New annotation `org.sonar.api.scanner.ScannerSide` to mark (project level) scanner components
* `org.sonar.api.batch.fs.InputProject` to create issues on project
* `org.sonar.api.scanner.ProjectSensor` to declare Sensors that only run at project level

![](/images/exclamation.svg) Deprecated

* `org.sonar.scanner.issue.IssueFilter` deprecated
* `org.sonar.api.batch.InstantiationStrategy` deprecated
* `org.sonar.api.batch.ScannerSide` deprecated
* `org.sonar.api.batch.fs.InputModule` deprecated
* Concept of global Sensor is deprecated (use `ProjectSensor` instead)

![](/images/cross.svg) Removed

* Support of scanner tasks was removed
* `RulesProfile` is no longer available for scanner side components (use `ActiveRules` instead)

### Release 7.5
No changes

### Release 7.4
![](/images/info.svg) Changed

* Allow identity provider to not provide login

![](/images/check.svg) Added

* Allow sensors to report adhoc rules metadata

![](/images/cross.svg) Removed

* `org.sonar.api.rules.RuleFinder` removed from scanner side
* `sonar-channel` removed from plugin classloader
* stop support of plugins compiled with API < 5.2

### Release 7.3

![](/images/check.svg) Added

* `RulesDefinitions` supports HotSpots and security standards

![](/images/exclamation.svg) Deprecated
* `org.sonar.api.batch.AnalysisMode` and `org.sonar.api.issue.ProjectIssues` since preview mode is already deprecated for a while

### Release 7.2
![](/images/check.svg) Added
* `org.sonar.api.batch.sensor.SensorContext#newExternalIssue` to report external issues
* `org.sonar.api.batch.sensor.SensorContext#newSignificantCode` to report part of the source file that should be used for issue tracking
* `org.sonar.api.scan.issue.filter.FilterableIssue#textRange`

![](/images/exclamation.svg) Deprecated
* org.sonar.api.scan.issue.filter.FilterableIssue#line

### Release 7.1
![](/images/check.svg) Added
* `org.sonar.api.Plugin.Context#getBootConfiguration`
* `org.sonar.api.server.rule.RulesDefinition.NewRule#addDeprecatedRuleKey` to support deprecated rule keys

### Release 7.0
![](/images/check.svg) Added
* `org.sonar.api.batch.scm.ScmProvider#relativePathFromScmRoot`, `org.sonar.api.batch.scm.ScmProvider#branchChangedFiles` and `org.sonar.api.batch.scm.ScmProvider#revisionId` to improve branch and PR support

### Release 6.7
No changes
