---
title: Security Engine Custom Configuration
url: /analysis/security_configuration/
---
*Security Engine Custom Configuration is available as part of the [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html) and [above](https://www.sonarsource.com/plans-and-pricing/).*

The security engine tracks the path that data follows through your code. It detects when data that's potentially manipulated by a malicious user reaches a sensitive piece of code where an attack can occur.

Those potentially malicious data are also called **tainted data**, because they are tainted by user inputs. 

SonarQube's security engine already knows a lot of APIs that are potential sources of attack and APIs that are potential targets of attack. While we do our best to identify publicly available APIs, we can't know everything about your homemade frameworks particularly when it comes to sanitizing your data. Because of this, SonarQube allows you to customize the security engine to add your own "sources", "sanitizers", "passthroughs", and "sinks" (see the **Elements** section below for more on these elements).

For example, you may want to:

* add a source to add support for a framework that SonarQube doesn't cover out of the box
* use a custom sanitizer to tell to the security engine that all data going through sanitizers should be considered as safe. This allows you to remove false positives and tailor the security engine to your company.

## Elements

You can add the following elements to your custom configuration:

* **Source** – Where you get user data. You should always consider user data tainted and vulnerable to injection attacks.
  Example: Calling `HttpServletRequest#getParam("foo")` will return tainted content
* **Sanitizer** – Finds and removes malicious content from tainted data.
* **Passthrough** – Allows you to keep track of tainted data sent to a library outside of the current function. When you pass a tainted value to a library functions outside of the current function, SonarQube automatically assumes it's being passed to a sanitizer. If the tainted data isn't being passed to a sanitizer, you can set up a passthrough to keep track of the data.
* **Sink** – A piece of code that can perform a security sensitive task. Data should not contain any malicious content once it reaches a sink.
  Example: Running an SQL query with `java.sql.Statement#execute`

## Analysis Parameters

To customize the SonarQube security engine, you need to feed security configuration data through parameters given to the SonarScanners. To do this, you should provide JSON files with the value of the new analysis parameters. The parameters should use the following syntax:

```
sonar.security.[ConfigType].[RuleRepository].[RuleKey]=[FileName]
```
The `ConfigType` value can be one of the following:

* `sources`
* `sanitizers`
* `passthroughs`
* `sinks`

The `RuleRepository` value can be one of the following:

* `javasecurity`: if you want to customize the Java Security Engine
* `phpsecurity`: if you want to customize the PHP Security Engine
* `roslyn.sonaranalyzer.security.cs`: if you want to customize the C# Security Engine

The `RuleKey` value can be one of the following:
* For Java
  * [S3649](https://rules.sonarsource.com/java/RSPEC-3649): SQL Injection
  * [S5131](https://rules.sonarsource.com/java/RSPEC-5131): XSS
  * [S5146](https://rules.sonarsource.com/java/RSPEC-5146): Open Redirect
  * [S5167](https://rules.sonarsource.com/java/RSPEC-5167): HTTP Response Splitting
  * [S2083](https://rules.sonarsource.com/java/RSPEC-2083): Path Traversal Injection
  * [S2078](https://rules.sonarsource.com/java/RSPEC-2078): LDAP Injection
  * [S5145](https://rules.sonarsource.com/java/RSPEC-5145): Log Injection
  * [S2076](https://rules.sonarsource.com/java/RSPEC-2076): OS Command Injection
  * [S2631](https://rules.sonarsource.com/java/RSPEC-2631): RegExp Injection
  * [S5144](https://rules.sonarsource.com/java/RSPEC-5144): Server-Side Request Forgery (SSRF)
  * [S2091](https://rules.sonarsource.com/java/RSPEC-2091): XPath Injection
  * [S5135](https://rules.sonarsource.com/java/RSPEC-5135): Deserialization Injection
  * [S5334](https://rules.sonarsource.com/java/RSPEC-5334): Code Injection
* For PHP
  * [S3649](https://rules.sonarsource.com/php/RSPEC-3649): SQL Injection
  * [S5131](https://rules.sonarsource.com/php/RSPEC-5131): XSS
  * [S5146](https://rules.sonarsource.com/php/RSPEC-5146): Open Redirect
  * [S5167](https://rules.sonarsource.com/php/RSPEC-5167): HTTP Response Splitting
  * [S2083](https://rules.sonarsource.com/php/RSPEC-2083): Path Traversal Injection
  * [S2078](https://rules.sonarsource.com/php/RSPEC-2078): LDAP Injection
  * [S5145](https://rules.sonarsource.com/php/RSPEC-5145): Log Injection
  * [S2076](https://rules.sonarsource.com/php/RSPEC-2076): OS Command Injection
  * [S2631](https://rules.sonarsource.com/php/RSPEC-2631): RegExp Injection
  * [S5144](https://rules.sonarsource.com/php/RSPEC-5144): Server-Side Request Forgery (SSRF)
  * [S2091](https://rules.sonarsource.com/php/RSPEC-2091): XPath Injection
  * [S5135](https://rules.sonarsource.com/php/RSPEC-5135): Deserialization Injection
  * [S5334](https://rules.sonarsource.com/php/RSPEC-5334): Code Injection
  * [S5335](https://rules.sonarsource.com/php/RSPEC-5335): Include Injection
* For C#
  * [S3649](https://rules.sonarsource.com/csharp/RSPEC-3649): SQL Injection
  * [S5131](https://rules.sonarsource.com/csharp/RSPEC-5131): XSS
  * [S5146](https://rules.sonarsource.com/csharp/RSPEC-5146): Open Redirect
  * [S5167](https://rules.sonarsource.com/csharp/RSPEC-5167): HTTP Response Splitting
  * [S2083](https://rules.sonarsource.com/csharp/RSPEC-2083): Path Traversal Injection
  * [S2078](https://rules.sonarsource.com/csharp/RSPEC-2078): LDAP Injection
  * [S5145](https://rules.sonarsource.com/csharp/RSPEC-5145): Log Injection
  * [S2076](https://rules.sonarsource.com/csharp/RSPEC-2076): OS Command Injection
  * [S2631](https://rules.sonarsource.com/csharp/RSPEC-2631): RegExp Injection
  * [S5144](https://rules.sonarsource.com/csharp/RSPEC-5144): Server-Side Request Forgery (SSRF)
  * [S2091](https://rules.sonarsource.com/csharp/RSPEC-2091): XPath Injection

[[info]]
| ![](/images/info.svg) The configuration works per rule. There is no way to share configuration between rules. 

## File Format

The configuration is provided through JSON files. Here is a sample JSON file that help to understand the expected JSON format.

**JSON File Format Example for PHP**

```
{
  "sources": [
    {
      "methodId": "My\\Namespace\\ClassName\\ServerRequest::getQuery"
    }
  ],
  "sanitizers": [
    {
      "methodId": "str_replace"
    }
  ],
  "passthroughs": [
    {
      "methodId": "rawurldecode",
      "args": [
        1
      ]
    }
  ],
  "sinks": [
    {
      "methodId": "mysql_query",
      "args": [
        1
      ]
    },
    {
      "methodId": "My\\Namespace\\SqlStatement::execute",
      "isMethodPrefix": true, // this is to say that all the methods starting with execute on the SqlStatement object will be considered
      "args": [
        0,
        1
      ]
    },
    {
      "methodId": "My\\Namespace\\SqlStatement::run",
      "interval": {
        "fromIndex": 1 // every parameter from the number 1 will be considered
      }
    }
  ]  
}
```

The `args` is the index of the parameter that can receive a tainted variable. Index starts:
* `1` for a fonction call. 
* `0` for a method call, index `0` beeing the current instance (`this`) 

## MethodId

All the custom configurations rely on the accuracy of the `methodIds` provided. For each language, the format of the `methodId` is different.

### MethodId for Java

The `methodId` format is inspired by the bytecode. The easiest way to get a `methodId` is to write a simple piece of Java code, compile it and then look at the bytecode generated using the `javap -c path_to.class` file, and transform it a little. Looking at the following real-life example will help you understand the format.

Let's imagine you want to declare `org.rapidoid.jdbc.JdbcClient.execute(String sql, Object... args)` as a new sink (you don't need to do this because Rapidoid is part of what is covered out of the box).

Write a simple piece of code calling the JdbcClient.execute(...) method. The code doesn't need to actually do anything.

```
import org.rapidoid.http.Req;
import org.rapidoid.jdbc.JdbcClient;

public static void callJDBCMethods(Req req) {
  String tainted = req.param("TAINTED");
  JdbcClient jdbc = JDBC.api();
  dbc.execute(tainted, req); // Noncompliant
}      
```

Run the `javap -c` and locate the piece of bytecode corresponding to the call to `JdbcClient.execute`

```
[...]
org/rapidoid/jdbc/JdbcClient.execute:(Ljava/lang/String;[Ljava/lang/Object;)I
[...]
````

* Replace the `/` in the package name with  `.`
* Remove the `:`
* Replace the `.` separating the Class name and the Method name with a `#`

The resulting `methodId` is:
```
org.rapidoid.jdbc.JdbcClient#execute(Ljava/lang/String;[Ljava/lang/Object;)I
```

### MethodId for PHP

The `methodId` can be:
* the name of a PHP function 
* the fully qualified name of a method following this format: `namespace\\ClassName::methodName`

Example: `Symfony\\Component\\HttpFoundation\\Request::getUser` for the `getUser()` method of the `Request` object provided by `Symfony`

Note: the `methodId` should be related to methods or functions that are part of the analysis scope. Because we recommended to not analyze code of frameworks at the same time that you scan your own source code, defining methods or functions from frameworks will have no effect.
This is linked to the fact that the SonarQube security engine needs to know the runtime type of each variable. The type can't be guessed when objects are created by frameworks' factories. Out of the box, the SonarQube security engine supports the main Symfony and Laravel types.

### MethodId for C&#35;

If you want to declare the constructor `SqlCommand` belonging to the namespace `System.Data.SqlClient` as a sink, the `methodId` should be:

```
System.Data.SqlClient.SqlCommand.SqlCommand(string, System.Data.SqlClient.SqlConnection)
```

You simply need to provide the fully qualified name of the method or constructor plus the types of the arguments.

## Deactivate Core Configuration

You can disable the core configuration per language or per rule using the following:

```
sonar.security.[ConfigType].[RuleRepository].noDefaultConfig=[true|false]
sonar.security.[ConfigType].[RuleRepository].[RuleKey].noDefaultConfig=[true|false]
```
