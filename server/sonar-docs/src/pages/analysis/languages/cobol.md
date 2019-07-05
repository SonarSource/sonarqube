---
title: COBOL
url: /analysis/languages/cobol/
---

<!-- sonarqube -->

_Cobol analysis is available as part of the [Enterprise Edition](https://redirect.sonarsource.com/editions/enterprise.html) and [above](https://redirect.sonarsource.com/editions/editions.html)._

<!-- /sonarqube -->

<!-- static -->
<!-- update_center:cobol -->
<!-- /static -->

## Language-Specific Properties

You can discover and update the COBOL-specific [properties](/analysis/analysis-parameters/) in: <!-- sonarcloud -->Project <!-- /sonarcloud -->**[Administration > General Settings > Cobol](/#sonarqube-admin#/admin/settings?category=cobol)**

## Source Code Extraction

In order to analyze your source code with SonarQube you need to first extract it onto a filesystem. You can use your own tool or an open source tool; SonarSource does not provide any connectors or source code extraction tools.

## Advanced Configuration

### Defining Source Code Format

The supported source code formats are:

- Fixed format
- Free format
- Variable format

To set the format, go to <!-- sonarcloud -->Project <!-- /sonarcloud -->**[Administration > General Settings > Cobol](/#sonarqube-admin#/admin/settings?category=cobol)** and set the "Source format" property.

The fixed format has three main areas:

```
Area1 | Area2                                           | Area3
000100* MY COMMENT
000100 IDENTIFICATION DIVISION.
000200 PROGRAM-ID. HELLOWORLD.                          *xxx
100000 PROCEDURE DIVISION.                              *yyy
100100
100200 START.
100400 DISPLAY "HELLO COBOL !" LINE 42 POSITION 12.
100500 STOP RUN.
```

Areas #1 and #3 contain non-significant characters.
Area #2 contains the source code. The first character of Area #2 is the Indicator Area, which has a special meaning (for instance `*` means that the line is a comment line, `D` means that the line is only taken into account in debug mode, etc.).

The free format:

```
Area1 | Area2
      * MY COMMENT
       IDENTIFICATION DIVISION.
         PROGRAM-ID. HELLOWORLD.
       PROCEDURE DIVISION.
         DISPLAY "HELLO COBOL !" LINE 42 POSITION 12.
         STOP RUN.
```

The Indicator Area that has a special meaning (for instance `*` means that the line is a comment line, `D` means that the line in only taken into account in debug mode, etc.) is located at column 0. The size of the source code area is not limited.

Variable format is also supported: it's similar to the fixed format but without Area #3.

### Defining COBOL Dialect

Go to <!-- sonarcloud -->Project <!-- /sonarcloud -->**[Administration > General Settings > Cobol](/#sonarqube-admin#/admin/settings?category=cobol)** and set the "Dialect" property.

The COBOL analyzer supports the following dialects:

- `bull-gcos-cobol`
- `hp-tandem-cobol`
- `ibm-os/vs-cobol`
- `ibm-ile-cobol`
- `ibm-cobol/ii`
- `ibm-cobol/400`
- `ibm-enterprise-cobol`
- `microfocus-cobol`
- `microfocus-acucobol-gt-cobol`
- `opencobol/cobol-it`

### Making Copybooks Available to the Analysis

Copybooks are, by definition, COBOL files that are not syntactically valid by themselves. However, copybooks are usually needed to properly parse COBOL programs. Thus, paths to the copybooks must be listed through the `sonar.cobol.copy.directories` property.

### Raising Issues Against Copybooks

To have copybooks imported into a project, and issues logged against them, the copybook directories must be added to `sonar.sources` AND the copybook file suffixes must be added to `sonar.cobol.file.suffixes`. E.G.:

```
sonar.sources=cobol,copy1,commonCopy
sonar.cobol.file.suffixes=cbl,cpy
sonar.cobol.copy.suffixes=cpy
sonar.cobol.copy.directories=copy1,commonCopy
```

In the case where a number of projects share a common set of copybooks, it may not be desirable to increment each project’s technical debt with the issues from the common copybooks. In such cases, the directory holding the common copybooks should be listed in `sonar.cobol.copy.directories` (as before) but left out of sonar.sources, E.G.:

```
sonar.sources=cobol,copy1
sonar.cobol.file.suffixes=cbl,cpy
sonar.cobol.copy.suffixes=cpy
sonar.cobol.copy.directories=copy1,commonCopy
```

### Analyzing without file suffixes

Note that it is possible to analyze a COBOL project without file suffixes. To do this, remove the two suffix-related properties from your configuration and substitute the following setting for `sonar.lang.patterns.cobol`:

```
sonar.lang.patterns.cobol=**/*
```

### Switching Off Issues

There are three ways to switch off issues:

- Flagging issues as [false positive](/user-guide/issues/)
- [Ignoring the issues](/project-administration/narrowing-the-focus/)
- Using the `NOSONAR` tag. To switch off an issue, place the `NOSONAR` tag in a comment line located right before the line containing the issue. Example:

```
* NOSONAR, in such case call to GO TO is tolerated, blabla...
 GO TO MY_PARAGRAPH.
```

### ACUCOBOL-GT Source Code Control Directives

The COBOL analyzer supports the ACUCOBOL-GT’s Source Code Control directives. This mechanism allows you to conditionally modify the program at compile time by excluding or including lines. This can be used to maintain different versions of the program, perhaps to support different machine environments.

The `-Si` (include) flag controls the actions of the source code control system. It must be followed by an argument that specifies a pattern that the compiler will search for in the Identification Area of each source line. If the pattern is found, then the line will be included in the source program, even if it is a comment line. However, if the pattern is immediately preceded by an exclamation point, then the line will be excluded from the source (i.e., commented out).

The `-Sx` (exclude) flag works the same way except that its meaning is reversed (lines with the pattern will be commented out and lines with a preceding exclamation point will be included).

For example, suppose a program is being maintained for both the UNIX and VMS environments. The following piece of code is in the program:

```
MOVE "SYS$HELP:HELPFILE" TO FILE-NAME.  VMS
*MOVE "/etc/helpfile" TO FILE-NAME.     UNX
OPEN INPUT HELP-FILE.
```

This program fragment is ready to be compiled for the VMS system. If a UNIX version is desired, then the following flags will correct the source during compilation:

```
-Si UNX -Sx VMS
```

Please consult the ACUCOBOL-GT documentation for more on the mechanism.

There are two ways in SonarQube to specify the list of ACUCOBOL-GT flags to be used in order to preprocess the source code. The first option is to define a list of global flags which will be used to preprocess all source files. This can be done in the **[Administration > General Settings > Cobol](/#sonarqube-admin#/admin/settings?category=cobol) > Preprocessor**.

The second option is to provide a list of relative paths (with help of the ‘sonar.cobol.acucobol.preprocessor.directives.directories’ property) which contain the list of flags to be used for each COBOL source file. Let’s take a simple example. If a file ‘MY_PROGRAM.CBL’ is going to be processed, the SonarQube ACUCOBOL-GT preprocessor, will try to find a file ‘MY_PROGRAM.CMD’. If this file is found, then the flags contained in this file is going to be used to preprocess the program ‘MY_PROGRAM.CBL’. If the file ‘MY_PROGRAM.CMD’ doesn’t exist, then the preprocess will use the content of the file ‘DEFAULT.CMD’ if exists.

### Microfocus Compiler Constants

If your code takes advantage of conditional compilation features provided by Microfocus, you may have to configure compiler constants for your analysis. You can define a compiler constant by setting a property named s`onar.cobol.compilationConstant.[constant name here].`

For example, if your COBOL code looks like this:

```
       IDENTIFICATION DIVISION.
      $IF myconstant DEFINED
       PROGRAM-ID. x.
      $END
      $IF otherconstant DEFINED
       PROGRAM-ID. y.
      $END
```

You can set the value of a compiler constant named "myconstant" by inserting the following line in your sonar-project.properties file:

```
sonar.cobol.compilationConstant.myconstant=myvalue
```

## Database Catalog (DB2)

The COBOL analyzer offers rules which target embedded SQL statements and require the analyzer to have knowledge of the database catalog (E.G. the primary key column(s) of a given table).
These rules will raise issues only if the database catalog is provided to the analysis. For the moment, this is available only for IBM DB2 (z/OS) catalogs, and the catalog must be provided via a set of CSV ("Comma Separated Values") files.

These rules rely on two analysis properties:

| Key                                     | Description                                                                      |
| --------------------------------------- | -------------------------------------------------------------------------------- |
| `sonar.cobol.sql.catalog.csv.path`      | relative path of the directory containing CSV files for the database catalog     |
| `sonar.cobol.sql.catalog.defaultSchema` | comma-separated list of default database schemas used in embedded SQL statements |

`sonar.cobol.sql.catalog.csv.path` should define a directory which contains 8 CSV files. Each of these CSV files contains data for a specific DB2 catalog table and is named after it. The following table lists the required files and their respective mandatory columns. Additional columns may be listed, but will be ignored:

| Table                  | File name           | Required Columns                                                                       |
| ---------------------- | ------------------- | -------------------------------------------------------------------------------------- |
| `SYSIBM.SYSCOLUMNS`    | `SYSCOLUMNS.csv`    | `TBNAME`,`TBCREATOR`,`NAME`,`PARTKEY_COLSEQ`,`DEFAULT`,`NULLS`,`DEFAULTVALUE`          |
| `SYSIBM.SYSINDEXES`    | `SYSINDEXES.csv`    | `NAME`,`CREATOR`,`TBNAME`,`TBCREATOR`,`UNIQUERULE`,`INDEXTYPE`                         |
| `SYSIBM.SYSINDEXPART`  | `SYSINDEXPART.csv`  | `IXNAME`,`IXCREATOR`,`PARTITION`                                                       |
| `SYSIBM.SYSKEYS`       | `SYSKEYS.csv`       | `IXNAME`,`IXCREATOR`,`COLNAME`,`COLSEQ`                                                |
| `SYSIBM.SYSSYNONYMS`   | `SYSSYNONYMS.csv`   | `NAME`,`CREATOR`,`TBNAME`,`TBCREATOR`                                                  |
| `SYSIBM.SYSTABLES`     | `SYSTABLES.csv`     | `NAME`,`CREATOR`,`TYPE`,`PARTKEYCOLNUM`,`TSNAME`,`DBNAME`,`TBNAME`,`TBCREATOR`,`CARDF` |
| `SYSIBM.SYSTABLESPACE` | `SYSTABLESPACE.csv` | `NAME`,`DBNAME`,`PARTITIONS`                                                           |
| `SYSIBM.SYSVIEWS`      | `SYSVIEWS.csv`      | `NAME`,`CREATOR`,`STATEMENT`                                                           |

The CSV format is the following:

- Each file must be named for the table it represents.
- First line must contain the exact names of the columns.
- Order of the columns is not meaningful.
- Fields are comma-delimited.
- If a field contains a comma, then its value must be surrounded by double quotes (").
- If a field which is surrounded by double quotes contains a double quote character ("), then this character must be doubled ("").

Example for `SYSVIEWS.csv`:

```
CREATOR,NAME,STATEMENT
USER1,VIEW1,select x from table1
USER1,VIEW2,"select x, y from table1"
USER1,VIEW3,"select x, ""y"" from table1"
```

The `UNLOAD` DB2 utility with the `DELIMITED` option should produce the required files except for the column names on the first line.

<!-- sonarqube -->

## Custom Rules

### Overview

The COBOL analyzer parses the source code, creates an Abstract Syntax Tree (AST) and then walks through the entire tree. A coding rule can subscribe to be notified every time a node of a certain type is visited.

As soon as the coding rule is notified, it can navigate the tree around the node and raise issues if necessary.

### Writing a Plugin

Writing new COBOL coding rules is a six-step process:

- Create a standard SonarQube plugin.
- Attach this plugin to the SonarQube COBOL plugin (see the `pom.xml` file of the provided sample plugin project).
- Create as many custom COBOL coding rules as required by extending `com.sonarsource.api.ast.CobolCheck` and add them to the previous repository.
- Generate the SonarQube plugin (jar file).
- Place this jar file in the `$SONARQUBE_HOME/extensions/plugins` directory.
- Restart the SonarQube server.

### Plugin Project Sample

To get started, clone the sample plugin project and follow the steps below:

- Install Maven
- Build the plugin by running `mvn install` from the project directory. This will generate a SonarQube plugin jar file in the target directory.
- Add your newly created jar into the `$SONARQUBE_HOME/extensions/plugins` directory
- Restart the SonarQube server

If you now look at the COBOL quality profiles, you will find the new coding rule (“Sample check”). Don’t forget to activate it. Run an analysis of a COBOL project, and you will find that an issue was logged at line 5 on every file.

### Subscribing to a NodeType

Very often when writing a coding rule, you will want to subscribe to a NodeType. A NodeType can be either a rule of the grammar or a keyword of the language. As an example, here is the code of the implementation of the “Avoid using Merge statement” coding rule:

```
public class MergeStatementUsageCheck extends CobolCheck {

  public void init() {
    subscribeTo(getCobolGrammar().mergeStatement);
  }

  public void visitNode(AstNode node) {
    reportIssue("Avoid using MERGE statement.").on(node);
  }
}
```

Note that CICS and SQL grammars can be accessed using `getCicsGrammar()` and `getSqlGrammar()`.

### Coding Rule Lifecycle

A coding rule can optionally override six methods inherited from the CobolCheck class. Those methods are called sequentially in the following order:

- `public void init() {…}`: This method is called only once and should be used to subscribe to one or more NodeType(s).
- `public void visitFile(AstNode astNode) {…}`: This method is called on each file before starting the parsing.
- `public void visitNode(AstNode astNode) {…}`: This method is called when an AstNode matches a subscribed NodeType (see Subscribing to a NodeType) and before analyzing its content.
- `public void leaveNode(AstNode astNode) {…}`: This method is called when an AstNode matches a desired NodeType (see Subscribing to a NodeType) and after analyzing its content.
- `public void leaveFile(AstNode astNode) {…}`: This method is called before exiting a file.
- `public void destroy() {…}`: This method is called before shutting down the coding rule.
- The `reportIssue(…)` method, used to log an issue, should be called only inside the `visitFile(…)`, `visitNode(…)`, `leaveNode(…)` and `leaveFile(…)` methods. Indeed, the file context isn’t known when the `init()` and `destroy()` methods are called, so the issue can’t be associated to a file.

More advanced features are documented in the [API Javadoc](http://javadocs.sonarsource.org/cobol/apidocs/).

### Navigating the AST (Abstract Syntax Tree) with the SSLR COBOL Toolkit

When starting to write a new COBOL coding rule, the main difficulty is to understand the COBOL AST in order to know which NodeType(s) need to be visited. This can be achieved by using the [SSLR COBOL Toolkit](https://binaries.sonarsource.com/CommercialDistribution/sslr-cobol-toolkit/), a Swing application that enables loading a COBOL file and displaying its representation as an Abstract Syntax Tree.

Each node in the AST is a COBOL grammar rule and each leaf in the AST is a COBOL token. Let’s say you want to visit the node `ifStatement`. In this case, the `init()` method of your COBOL coding rule must contain the following statement: `subscribeTo(getCobolGrammar().ifStatement);`

### API Changes

_Since 4.0_
A new API is available to write the rules but also to implement the tests.

Custom rules should now extend `CobolCheck` (`CobolAstCheck` is deprecated) and issues should be logged using the `reportIssue(...)` method.  
Tests on custom rules should now use `CobolCheckVerifier`: the assertions about issues should now be added as comments inside COBOL test files.  
Custom rules should be listed in an implementation of `CobolCheckRepository` (`CobolAstCheckRepository` is now deprecated) and metadata should be loaded by implementing `RulesDefinitionExtension`.  
You can now store your custom rules into a dedicated rule repository by implementing SonarQube's `RulesDefinition`: in that case, you don't need to implement `RulesDefinitionExtension`.  
![](/images/exclamation.svg) For users who already have custom rules in production: existing issues will be closed and re-opened because the internal keys of the rules are changing.
If you wrote a custom plugin against SonarCOBOL 3.x, it should still be compatible at runtime with SonarCOBOL 4.0.

To migrate to the new API ([full example on github](https://github.com/SonarSource/sonar-custom-rules-examples/pull/14)):

- First, migrate tests without modifying rule classes. That mainly requires moving assertions from java test classes to comments inside test cobol files ([see an example on github](https://github.com/SonarSource/sonar-custom-rules-examples/commit/c95b6a84b6fd1efc832a46cd5e1101ee51e6268e)).
- Update check classes to replace the calls to deprecated methods with the new methods which create issues ([see an example on github](https://github.com/SonarSource/sonar-custom-rules-examples/commit/d6f6ef7457d99e31990fa64b5ff9cc566775af96)).
- Implement `CobolRulesDefinitionExtension` and `CobolCheckRepository`, remove the class extending `CobolAstCheckRepository` ([see an example on github](https://github.com/SonarSource/sonar-custom-rules-examples/commit/ea15f07ce79366a08fee5b60e9a93c32a4625918)).
- Update check classes to extend `CobolCheck` instead of `CobolAstCheck` to stop using deprecated APIs ([see an example on github](https://github.com/SonarSource/sonar-custom-rules-examples/commit/8e1d746900f5411e9700fea04700cd804e45e034)).

To move your custom rules to a dedicated rule repository, see [an example on github](https://github.com/SonarSource/sonar-custom-rules-examples/commit/16ad89c4172c259f15bce56edcd09dd5b489eacd).

## Related Pages

- [Adding Coding Rules](/extend/adding-coding-rules/)
  <!-- /sonarqube -->
