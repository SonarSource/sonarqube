---
title: Security Engine Custom Configuration
url: /analysis/security_configuration/
---
*Security Engine Custom Configuration is available as part of the [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html) and [above](https://www.sonarsource.com/plans-and-pricing/).*

The security engine tracks the path that data follows through your code. It detects when data that's potentially manipulated by a malicious user reaches a sensitive piece of code where an attack can occur.

Those potentially malicious data are also called tainted data because they are tainted by user inputs. 

SonarQube's security engine already knows a lot of APIs that are potential sources or targets of attack. While we do our best to identify publicly available APIs, we can't know everything about your homemade frameworks particularly when it comes to sanitizing your data. Because of this, SonarQube allows you to customize the security engine to add your own sources, sanitizers, passthroughs, and sinks (see the **Elements** section below for more on these elements).

For example, you may want to:

* add a source to add support for a framework that SonarQube doesn't cover out of the box
* use a custom sanitizer to tell the security engine that all data going through sanitizers should be considered safe. This allows you to remove false positives and tailor the security engine to your company.

## Rules
You can customize elements for Java, PHP, C#, and Python rules in the security engine. Click the languages below to expand a list of customizable rules for that language:

[[collapse]]
| ## Java
| * [S3649](https://rules.sonarsource.com/java/RSPEC-3649): SQL Injection
| * [S5131](https://rules.sonarsource.com/java/RSPEC-5131): XSS
| * [S5146](https://rules.sonarsource.com/java/RSPEC-5146): Open Redirect
| * [S5167](https://rules.sonarsource.com/java/RSPEC-5167): HTTP Response Splitting
| * [S2083](https://rules.sonarsource.com/java/RSPEC-2083): Path Traversal Injection
| * [S2078](https://rules.sonarsource.com/java/RSPEC-2078): LDAP Injection
| * [S5145](https://rules.sonarsource.com/java/RSPEC-5145): Log Injection
| * [S2076](https://rules.sonarsource.com/java/RSPEC-2076): OS Command Injection
| * [S2631](https://rules.sonarsource.com/java/RSPEC-2631): RegExp Injection
| * [S5144](https://rules.sonarsource.com/java/RSPEC-5144): Server-Side Request Forgery (SSRF)
| * [S2091](https://rules.sonarsource.com/java/RSPEC-2091): XPath Injection
| * [S5135](https://rules.sonarsource.com/java/RSPEC-5135): Deserialization Injection
| * [S5334](https://rules.sonarsource.com/java/RSPEC-5334): Code Injection
| * [S6096](https://rules.sonarsource.com/java/RSPEC-6096): Zip Slip

[[collapse]]
| ## PHP
| * [S3649](https://rules.sonarsource.com/php/RSPEC-3649): SQL Injection
| * [S5131](https://rules.sonarsource.com/php/RSPEC-5131): XSS
| * [S5146](https://rules.sonarsource.com/php/RSPEC-5146): Open Redirect
| * [S5167](https://rules.sonarsource.com/php/RSPEC-5167): HTTP Response Splitting
| * [S2083](https://rules.sonarsource.com/php/RSPEC-2083): Path Traversal Injection
| * [S2078](https://rules.sonarsource.com/php/RSPEC-2078): LDAP Injection
| * [S5145](https://rules.sonarsource.com/php/RSPEC-5145): Log Injection
| * [S2076](https://rules.sonarsource.com/php/RSPEC-2076): OS Command Injection
| * [S2631](https://rules.sonarsource.com/php/RSPEC-2631): RegExp Injection
| * [S5144](https://rules.sonarsource.com/php/RSPEC-5144): Server-Side Request Forgery (SSRF)
| * [S2091](https://rules.sonarsource.com/php/RSPEC-2091): XPath Injection
| * [S5135](https://rules.sonarsource.com/php/RSPEC-5135): Deserialization Injection
| * [S5334](https://rules.sonarsource.com/php/RSPEC-5334): Code Injection
| * [S5335](https://rules.sonarsource.com/php/RSPEC-5335): Include Injection

[[collapse]]
| ## C&#35;
| * [S3649](https://rules.sonarsource.com/csharp/RSPEC-3649): SQL Injection
| * [S5131](https://rules.sonarsource.com/csharp/RSPEC-5131): XSS
| * [S5146](https://rules.sonarsource.com/csharp/RSPEC-5146): Open Redirect
| * [S5167](https://rules.sonarsource.com/csharp/RSPEC-5167): HTTP Response Splitting
| * [S2083](https://rules.sonarsource.com/csharp/RSPEC-2083): Path Traversal Injection
| * [S2078](https://rules.sonarsource.com/csharp/RSPEC-2078): LDAP Injection
| * [S5145](https://rules.sonarsource.com/csharp/RSPEC-5145): Log Injection
| * [S2076](https://rules.sonarsource.com/csharp/RSPEC-2076): OS Command Injection
| * [S2631](https://rules.sonarsource.com/csharp/RSPEC-2631): RegExp Injection
| * [S5144](https://rules.sonarsource.com/csharp/RSPEC-5144): Server-Side Request Forgery (SSRF)
| * [S2091](https://rules.sonarsource.com/csharp/RSPEC-2091): XPath Injection
| * [S5334](https://rules.sonarsource.com/csharp/RSPEC-5334): Code Injection
| * [S6096](https://rules.sonarsource.com/csharp/RSPEC-6096): Zip Slip

[[collapse]]
| ## Python
| * [S3649](https://rules.sonarsource.com/python/RSPEC-3649): SQL Injection
| * [S5131](https://rules.sonarsource.com/python/RSPEC-5131): XSS
| * [S5146](https://rules.sonarsource.com/python/RSPEC-5146): Open Redirect
| * [S5167](https://rules.sonarsource.com/python/RSPEC-5167): HTTP Response Splitting
| * [S2083](https://rules.sonarsource.com/python/RSPEC-2083): Path Traversal Injection
| * [S2078](https://rules.sonarsource.com/python/RSPEC-2078): LDAP Injection
| * [S5145](https://rules.sonarsource.com/python/RSPEC-5145): Log Injection
| * [S2076](https://rules.sonarsource.com/python/RSPEC-2076): OS Command Injection
| * [S2631](https://rules.sonarsource.com/python/RSPEC-2631): RegExp Injection
| * [S5144](https://rules.sonarsource.com/python/RSPEC-5144): Server-Side Request Forgery (SSRF)
| * [S2091](https://rules.sonarsource.com/python/RSPEC-2091): XPath Injection
| * [S5135](https://rules.sonarsource.com/python/RSPEC-5135): Object Injection
| * [S5334](https://rules.sonarsource.com/python/RSPEC-5334): Code Injection

## Elements

You can add the following elements to your custom configuration:

* **Source** – Where you get user data. You should always consider user data tainted and vulnerable to injection attacks.
  Example: Calling `HttpServletRequest#getParam("foo")` will return tainted content
* **Sanitizer** – Finds and removes malicious content from one or more potentially tainted arguments.
  Example: `DatabaseUtils#sqlEscapeString(String str)` returns a modified version of `str` where characters used in an SQL injection attack are removed.    
* **Validator** - Marks one or more arguments as safe from malicious content.
  Example: `String#matches(String str)` can be used to verify that `str` does not contain any content which may be used in an injection attack. 
* **Passthrough** – Allows you to keep track of tainted data sent to a library outside the current function. When you pass a tainted value to a library function outside the current function, SonarQube automatically assumes it's being passed to a sanitizer. If the tainted data isn't being passed to a sanitizer, you can set up a passthrough to keep track of the data.
* **Sink** – A piece of code that can perform a security-sensitive task. Data should not contain any malicious content once it reaches a sink.
  Example: Running an SQL query with `java.sql.Statement#execute`

## MethodId

All custom configurations rely on the accuracy of the `methodIds` provided. The `methodId` format differs for each language. Click the language you're using below for more information on the format for that language.

[[collapse]]
| ## Java methodId
|
| The `methodId` format is inspired by the bytecode. The easiest way to get a `methodId` is to write a simple piece of Java code, compile it, and then look at the bytecode generated using the `javap -c path_to.class` file, and transform it a little. Looking at the following real-life example will help you understand the format.
| 
| Let's imagine you want to declare `org.rapidoid.jdbc.JdbcClient.execute(String sql, Object... args)` as a new sink (you don't need to do this because Rapidoid is part of what is covered out of the box).
| 
| Write a simple piece of code calling the JdbcClient.execute(...) method. The code doesn't need to actually do anything.
| 
| ```
| import org.rapidoid.http.Req;
| import org.rapidoid.jdbc.JdbcClient;
|
| public static void callJDBCMethods(Req req) {
|   String tainted = req.param("TAINTED");
|   JdbcClient jdbc = JDBC.api();
|   jdbc.execute(tainted, req); // Noncompliant
| }      
| ```
| 
| Run the `javap -c` and locate the piece of bytecode corresponding to the call to `JdbcClient.execute`
| 
| ```
| [...]
| org/rapidoid/jdbc/JdbcClient.execute:(Ljava/lang/String;[Ljava/lang/Object;)I
| [...]
| ````
| 
| * Replace the `/` in the package name with  `.`
| * Remove the `:`
| * Replace the `.` separating the Class name and the Method name with a `#`
| 
| The resulting `methodId` is:
| ```
| org.rapidoid.jdbc.JdbcClient#execute(Ljava/lang/String;[Ljava/lang/Object;)I
| ```

[[collapse]]
| ## PHP methodId
| 
| The `methodId` can be:
| * the name of a PHP function 
| * the fully qualified name of a method following this format: `namespace\\ClassName::methodName`
| 
| Example: `Symfony\\Component\\HttpFoundation\\Request::getUser` for the `getUser()` method of the `Request` object provided by `Symfony`
| 
| Note: the `methodId` should be related to methods or functions that are part of the analysis scope. Because we recommended to not analyze the code of frameworks at the same time that you scan your own source code, defining methods or functions from frameworks will have no effect.
| This is linked to the fact that the SonarQube security engine needs to know the runtime type of each variable. The type can't be guessed when objects are created by frameworks' factories. Out of the box, the SonarQube security engine supports the main Symfony and Laravel types.

[[collapse]]
| ## C&#35; methodId 
| 
| If you want to declare the constructor `SqlCommand` belonging to the namespace `System.Data.SqlClient` as a sink, the `methodId` should be:
| 
| ```
| System.Data.SqlClient.SqlCommand.SqlCommand(string, System.Data.SqlClient.SqlConnection)
| ```
| 
| You simply need to provide the fully qualified name of the method or constructor plus the types of the arguments.

[[collapse]]
| ## Python methodId
| 
| Python `methodIds` can be defined as either of the following:
| * the name of a global Python function.
| * the fully qualified name of a method following this format: `namespace.ClassName.methodName`. Ex: `ldap.ldapobject.SimpleLDAPObject.search`, `str.isidentifier`.
  
## Creating your custom configuration JSON file

You need to add your custom configurations to SonarQube using a JSON file. You can apply your custom configuration to a specific project or to all of your projects at the global level in SonarQube: 

* **Project level** – go to **Project Settings > General Settings > SAST Engine** and add your JSON file to the **JAVA/PHP/C#/Python custom configuration** field.

* **Global level** – go to **Administration > General Settings > SAST Engine** and add your JSON file to the **JAVA/PHP/C#/Python custom configuration** field.  

See the following section for more information on formatting your JSON file.

### Configuration file format
Your JSON file should include the rule you're adding a custom element to, the element you are customizing, and the `methodId` for each element. Each language needs a separate JSON file but can contain multiple rules. Click your language below to expand an example of a JSON file to help you understand the expected format.

[[collapse]]
| ## Java JSON file example
|
| ```
| {
|   "S3649": {
|     "sources": [
|       {
|         "methodId": "my.package.ServerRequest#getQuery()Ljava/lang/String;"
|       }
|     ],
|     "sanitizers": [
|       {
|         "methodId": "my.package.StringUtils#stringReplace(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
|         "args": [
|           2 
|         ]
|       }
|     ],
|     "validators": [
|       {
|         "methodId": "my.package.StringUtils#equals(Ljava/lang/String;)Z",
|         "args": [
|           1
|         ]
|       }
|     ],
|     "passthroughs": [
|       {
|         "methodId": "my.package.RawUrl#<init>(Ljava/lang/String;)V",
|         "isWhitelist": true,
|         "args": [
|           1
|         ]
|       }
|     ],
|     "sinks": [
|       {
|         "methodId": "my.package.MySql#query(Ljava/lang/String;)V",
|         "args": [
|           1
|         ]
|       },
|       {
|         "methodId": "my.package.SqlStatement#execute",
|         "isMethodPrefix": true,
|         "args": [
|           0,
|           1
|         ]
|       },
|       {
|         "methodId": "my.package.SqlStatement#run(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
|         "interval": {
|           "fromIndex": 1
|         }
|       }
|     ]
|   },
|   "S5131": {
|     "sources": [
|       {
|         "methodId": "my.package.ServerRequest#getQueryString()Ljava/lang/String;"
|       }
|     ],
|     "sinks": [
|       {
|         "methodId": "my.package.Server#write(",
|         "isMethodPrefix": true,
|         "interval": {
|           "fromIndex": 1
|         }
|       }
|     ]
|   }
| }
|```
|
| The `args` is the index of the parameter that can receive a tainted variable. Index starts:
| * `1` for a function call. 
| * `0` for a method call, index `0` being the current instance (`this`).
| The `args` field must be a non-empty array of non-negative integers, and it is a mandatory field for sanitizers and validators.

[[collapse]]
| ## PHP JSON file example
|
| ```
| {
|   "S3649": {
|     "sources": [
|       {
|         "methodId": "My\\Namespace\\ClassName\\ServerRequest::getQuery"
|       }
|     ],
|     "sanitizers": [
|       {
|         "methodId": "str_replace",
|         "args": [
|           3
|         ]
|       }
|     ],
|     "validators": [
|       {
|         "methodId": "My\\Namespace\\Validator\\inArray::isValid",
|         "args": [
|           1
|         ]
|       }
|     ],
|     "passthroughs": [
|       {
|         "methodId": "My\\Namespace\\RawUrl::RawUrl",
|         "isWhitelist": true,
|         "args": [
|           1
|         ]
|       }
|     ],
|     "sinks": [
|       {
|         "methodId": "mysql_query",
|         "args": [
|           1
|         ]
|       },
|      {
|         "methodId": "My\\Namespace\\SqlStatement::execute",
|         "isMethodPrefix": true,
|         "args": [
|           0,
|           1
|         ]
|       },
|       {
|         "methodId": "My\\Namespace\\SqlStatement::run",
|         "interval": {
|           "fromIndex": 1
|         }
|       }
|     ]
|   },
|   "S5131": {
|     "sources": [
|       {
|         "methodId": "My\\Namespace\\ClassName\\ServerRequest::getQueryString"
|       }
|     ],
|     "sinks": [
|       {
|         "methodId": "My\\Namespace\\ClassName\\Server::write",
|         "isMethodPrefix": true,
|         "interval": {
|           "fromIndex": 1
|         }
|       }
|     ]
|   }
| }
|```
|
| The `args` is the index of the parameter that can receive a tainted variable. Index starts:
| * `1` for a function call. 
| * `0` for a method call, index `0` being the current instance (`this`).
| The `args` field must be a non-empty array of non-negative integers, and it is a mandatory field for sanitizers and validators.

[[collapse]]
| ## C&#35; JSON file example
|
| ```
| {
|   "S3649": {
|     "sources": [
|       {
|         "methodId": "My.Namespace.ServerRequest.GetQuery()"
|       }
|     ],
|     "sanitizers": [
|       {
|         "methodId": "My.Namespace.StringUtils.StringReplace(string, string)",
|         "args": [
|           0
|         ]
|       }
|     ],
|     "validators": [
|       {
|         "methodId": "My.Namespace.StringUtils.Regex.Matches(string)",
|         "args": [
|           0
|         ]
|       }
|     ],
|     "passthroughs": [
|       {
|         "methodId": "My.Namespace.RawUrl.RawUrl(string)",
|         "isWhitelist": true,
|         "args": [
|           1
|         ]
|       }
|     ],
|     "sinks": [
|       {
|         "methodId": "My.Namespace.MySql.Query(string)",
|         "args": [
|           1
|         ]
|       },
|       {
|         "methodId": "My.Namespace.SqlStatement.Execute",
|         "isMethodPrefix": true,
|         "args": [
|           0,
|           1
|         ]
|       },
|       {
|         "methodId": "My.Namespace.SqlStatement.Run(string, string, string)",
|         "interval": {
|           "fromIndex": 1
|         }
|       }
|     ]
|   },
|   "S5131": {
|     "sources": [
|       {
|         "$comment": "The following method id is a getter on the 'QueryString' property",
|         "methodId": "My.Namespace.ServerRequest.QueryString.get"
|       }
|     ],
|     "sinks": [
|       {
|         "methodId": "My.Namespace.Server.Write(",
|         "isMethodPrefix": true,
|         "interval": {
|           "fromIndex": 1
|         }
|       }
|     ]
|   }
| }
|```
|
| The `args` is the index of the parameter that can receive a tainted variable. Index starts:
| * `1` for a function call. 
| * `0` for a method call, index `0` being the current instance (`this`).
| The `args` field must be a non-empty array of non-negative integers, and it is a mandatory field for sanitizers and validators.

[[collapse]]
| ## Python JSON file example
|
|```
| {
|   "S3649": {
|     "sources": [
|       {
|         "methodId": "my.namespace.ServerRequest.get_query"
|       }
|     ],
|     "sanitizers": [
|       {
|         "methodId": "str_replace",
|         "args": [
|           1
|         ]
|       }
|     ],
|     "validators": [
|       {
|         "methodId": "my.namespace.regex.matches",
|         "args": [
|           1
|         ]
|       }
|     ],
|     "passthroughs": [
|       {
|         "methodId": "my.namespace.RawUrl",
|         "isWhitelist": true,
|         "args": [
|           1
|         ]
|       }
|     ],
|     "sinks": [
|       {
|         "methodId": "mysql_query",
|         "args": [
|           1
|         ]
|       },
|       {
|         "methodId": "my.namespace.SqlStatement.execute",
|         "isMethodPrefix": true,
|         "args": [
|           0,
|           1
|         ]
|       },
|       {
|         "methodId": "my.namespace.SqlStatement.run",
|         "interval": {
|           "fromIndex": 1
|         }
|       }
|     ]
|   },
|   "S5131": {
|     "sources": [
|       {
|         "methodId": "my.namespace.ServerRequest.get_query_string"
|       }
|     ],
|     "sinks": [
|       {
|         "methodId": "my.namespace.Server.write(",
|         "isMethodPrefix": true,
|         "interval": {
|           "fromIndex": 1
|         }
|       }
|     ]
|   }
| }
|
|```
|
| The `args` is the index of the parameter that can receive a tainted variable. Index starts:
| * `1` for a function call. 
| * `0` for a method call, index `0` being the current instance (`this`).
| The `args` field must be a non-empty array of non-negative integers, and it is a mandatory field for sanitizers and validators.

### (Deprecated) Customizing through analysis parameters

[[warning]]
| Customizing the security engine through analysis parameters is deprecated. We recommend adding your custom configuration in SonarQube as shown above. This allows you to create a single configuration file for each language and to easily apply it to multiple projects or globally.

To customize the SonarQube security engine, you can feed security configuration data through parameters given to the SonarScanners. To do this, you should provide JSON files with the value of the new analysis parameters. 

[[info]]
|The configuration works per rule. You can't share a configuration between rules.

The parameters should use the following syntax:

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
* `pythonsecurity`: if you want to customize the Python Security Engine

The `RuleKey` value should be one of the values shown in the **Rules** section above.
  
#### **JSON formatting example**

Configuration is provided using JSON files. Click the heading below to expand an example PHP JSON file to help you understand the expected format.

[[collapse]]
| ## JSON File Format Example for PHP
|
| [[info]]
| | You need to create a configuration for each rule. There is no way to share a configuration between rules.
|
| ```
| {
|   "sources": [
|     {
|       "methodId": "My\\Namespace\\ClassName\\ServerRequest::getQuery"
|     }
|   ],
|   "sanitizers": [
|     {
|       "methodId": "str_replace",
|       "args": [
|         3
|       ]
|     }
|   ],
|   "validators": [
|     {
|      "methodId": "My\\Namespace\\Validator\\inArray::isValid",
|      "args": [
|         1
|       ]
|     }
|   ],
|  "passthroughs": [
|     {
|       "methodId": "rawurldecode",
|       "args": [
|         1
|       ]
|     }
|   ],
|   "sinks": [
|     {
|       "methodId": "mysql_query",
|       "args": [
|         1
|       ]
|     },
|     {
|       "methodId": "My\\Namespace\\SqlStatement::execute",
|       "isMethodPrefix": true, // this is to say that all the methods starting with execute on the SqlStatement object will be considered
|       "args": [
|         0,
|         1
|       ]
|     },
|     {
|       "methodId": "My\\Namespace\\SqlStatement::run",
|       "interval": {
|         "fromIndex": 1 // every parameter from the number 1 will be considered
|       }
|     }
|   ]  
| }
| ```
|
| The `args` is the index of the parameter that can receive a tainted variable. Index starts:
| * `1` for a function call. 
| * `0` for a method call, index `0` being the current instance (`this`)  .
| The `args` field must be a non-empty array of non-negative integers, and it is a mandatory field for sanitizers and validators.

## Deactivating the core configuration

You can disable the core configuration per language or per rule using the following:

```
sonar.security.[ConfigType].[RuleRepository].noDefaultConfig=[true|false]
sonar.security.[ConfigType].[RuleRepository].[RuleKey].noDefaultConfig=[true|false]
```
