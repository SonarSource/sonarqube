---
title: Java
url: /analysis/languages/java/
---

<!-- static -->
<!-- update_center:java -->
<!-- /static -->


## Language-Specific Properties

You can discover and update the Java-specific [properties](/analysis/analysis-parameters/) in:  <!-- sonarcloud -->Project <!-- /sonarcloud -->[Administration > General Settings > Java](/#sonarqube-admin#/admin/settings?category=java)

## Java Analysis and Bytecode

Compiled `.class` files are required for java projects with more than one java file. If not provided properly, analysis will fail with the message:

    Please provide compiled classes of your project with sonar.java.binaries property.

If only some `.class` files are missing, you'll see warnings like this:

    Class 'XXXXXX' is not accessible through the ClassLoader.

If you are not using Maven or Gradle for analysis, you must manually provide bytecode to the analysis.
You can also analyze test code, and for that you need to provide tests binaires and test libraries properties.

Key | Value
---|---|
`sonar.java.binaries` (required) | Comma-separated paths to directories containing the compiled bytecode files corresponding to your source files. 
`sonar.java.libraries` | Comma-separated paths to files with third-party libraries (JAR or Zip files) used by your project. Wildcards can be used: `sonar.java.libraries=path/to/Library.jar,directory/**/*.jar`
`sonar.java.test.binaries` | Comma-separated paths to directories containing the compiled bytecode files corresponding to your test files
`sonar.java.test.libraries` | Comma-separated paths to files with third-party libraries (JAR or Zip files) used by your tests. (For example, this should include the junit jar). Wildcards can be used: `sonar.java.test.libraries=directory/**/*.jar`

[[warning]]
| ![](/images/exclamation.svg) Android users, Jack doesn't provide the required `.class` files.


## Turning issues off

The best way to deactivate an individual issue you don't intend to fix is to mark it "Won't Fix" or "False Positive" through the SonarQube UI.

If you need to deactivate a rule (or all rules) for an entire file, then [issue exclusions](/project-administration/narrowing-the-focus/) are the way to go. But if you only want to deactivate a rule across a subset of a file - all the lines of a method or a class - you can use `@SuppressWarnings("all")` or `@SuppressWarnings` with rule keys: `@SuppressWarnings("squid:S2078")` or `@SuppressWarnings({"squid:S2078", "squid:S2076"})`. 

## Handling Java Source Version

The Java Analyzer is able to react to the java version used for sources. This feature allows the deactivation of rules that target higher versions of Java than the one in use in the project so that false positives aren't generated from irrelevant rules.

The feature relies entirely on the `sonar.java.source` property, which is automatically filled by most of the scanners used for analyses (Maven, Gradle). Java version-specific rules are not disabled when `sonar.java.source` is not provided. Concretely, rules which are designed to target specific java versions (tagged "java7" or "java8") are activated by default in the Sonar Way Java profile. From a user perspective, the feature is fully automatic, but it means that you probably want your projects to be correctly configured.

When using SonarScanner to perform analyses of project, the property `sonar.java.source` can to be set manually in `sonar-project.properties`. Accepted formats are:
* "1.X" (for instance 1.6 for java 6, 1.7 for java 7, 1.8 for java 8, etc.)
* "X" (for instance 7 for java 7, 8 for java 8, etc. )

Example: `sonar.java.source=1.6`

If the property is provided, the analysis will take the source version into account, and execute related rules accordingly. At run time, each of these rules will be executed – or not – depending of the Java version used by sources within the project. For instance, on a correctly configured project built with Java 6, rules targeting Java 7 and Java 8 will never raise issues, even though they are enabled in the associated rule profile.

<!-- sonarqube -->
## Custom Rules

The tutorial [Writing Custom Java Rules 101](https://redirect.sonarsource.com/doc/java-custom-rules-guide.html) will help to quickly start writing custom rules for Java.

### API changes

#### **5.12**
* **Dropped**
    * `org.sonar.plugins.java.api.JavaFileScannerContext`: Drop deprecated method used to retrieve trees contributing to the complexity of a method from  (deprecated since SonarJava 4.1). 
        ```
        //org.sonar.plugins.java.api.JavaFileScannerContext
        /**
        * Computes the list of syntax nodes which are contributing to increase the complexity for the given methodTree.
        * @deprecated use {@link #getComplexityNodes(Tree)} instead
        * @param enclosingClass not used.
        * @param methodTree the methodTree to compute the complexity.
        * @return the list of syntax nodes incrementing the complexity.
        */
        @Deprecated
        List<Tree> getMethodComplexityNodes(ClassTree enclosingClass, MethodTree methodTree);
        ```
    * `org.sonar.plugins.java.api.JavaResourceLocator`: The following method has been dropped (deprecated since SonarJava 4.1), without replacement.
        ```
        //org.sonar.plugins.java.api.JavaResourceLocator
        /**
        * get source file key by class name.
        * @deprecated since 4.1 : will be dropped with no replacement.
        * @param className fully qualified name of the analyzed class.
        * @return key of the source file for the given class.
        */
        @Deprecated
        String findSourceFileKeyByClassName(String className);
        ```
    * `org.sonar.plugins.surefire.api.SurefireUtils`: Dropping deprecated field with old property (deprecated since SonarJava 4.11)
        ```
        //org.sonar.plugins.surefire.api.SurefireUtils
        /**
        * @deprecated since 4.11
        */
        @Deprecated
        public static final String SUREFIRE_REPORTS_PATH_PROPERTY = "sonar.junit.reportsPath";
        ```
* **Deprecated**  
    * `org.sonar.plugins.java.api.JavaFileScannerContext`: Deprecate usage of File-based methods from API, which will be removed in future release. Starting from this version, methods relying on InputFile has to be preferred.
        ```
        //org.sonar.plugins.java.api.JavaFileScannerContext
        /**
        * Report an issue at a specific line of a given file.
        * This method is used for one
        * @param file File on which to report
        * @param check The check raising the issue.
        * @param line line on which to report the issue
        * @param message Message to display to the user
        * @deprecated since SonarJava 5.12 - File are not supported anymore. Use corresponding 'reportIssue' methods, or directly at project level
        */
        @Deprecated
        void addIssue(File file, JavaCheck check, int line, String message);
        /**
        * FileKey of currently analyzed file.
        * @return the fileKey of the file currently analyzed.
        * @deprecated since SonarJava 5.12 - Rely on the InputFile key instead, using {@link #getInputFile()}
        */
        @Deprecated
        String getFileKey();

        /**
        * File under analysis.
        * @return the currently analyzed file.
        * @deprecated since SonarJava 5.12 - File are not supported anymore. Use {@link #getInputFile()} or {@link #getProject()} instead
        */
        @Deprecated
        File getFile();
        ```
    * Deprecate methods which are not relevant anymore in switch-related trees from API, following introduction of the new Java 12 `switch` expression:
        ```
        //org.sonar.plugins.java.api.tree.CaseLabelTree
        /**
        * @deprecated (since 5.12) use the {@link #expressions()} method.
        */
        @Deprecated
        @Nullable
        ExpressionTree expression();

        /**
        * @deprecated (since 5.12) use the {@link #colonOrArrowToken()} method.
        */
        @Deprecated
        SyntaxToken colonToken();
        ```
* **Added**
    * `org.sonar.plugins.java.api.JavaFileScannerContext`: Following methods have been added in order to provide help reporting issues at project level, and access data through SonarQube's InputFile API, which won't be possible anymore through files:
    ```
        //JavaFileScannerContext: New methods
        /**
        * Report an issue at at the project level.
        * @param check The check raising the issue.
        * @param message Message to display to the user
        */
        void addIssueOnProject(JavaCheck check, String message);
    
        /**
        * InputFile under analysis.
        * @return the currently analyzed inputFile.
        */
        InputFile getInputFile();
        
        /**
        * InputComponent representing the project being analyzed
        * @return the project component
        */
        InputComponent getProject();
        ```
    * In order to cover the Java 12 new switch expression, introduce a new Tree in the SonarJava Syntax Tree API  (Corresponding `Tree.Kind`: `SWITCH_EXPRESSION` ). New methods have also been added to fluently integrate the new switch expression into the SonarJava API.
        ```
        //org.sonar.plugins.java.api.tree.SwitchExpressionTree
        /**
        * 'switch' expression.
        *
        * JLS 14.11
        *
        * <pre>
        *   switch ( {@link #expression()} ) {
        *     {@link #cases()}
        *   }
        * </pre>
        *
        * @since Java 12
        */
        @Beta
        public interface SwitchExpressionTree extends ExpressionTree {
        
        SyntaxToken switchKeyword();
        
        SyntaxToken openParenToken();
        
        ExpressionTree expression();
        
        SyntaxToken closeParenToken();
        
        SyntaxToken openBraceToken();
        
        List<CaseGroupTree> cases();
        
        SyntaxToken closeBraceToken();
        }
        ```
        ```
        //org.sonar.plugins.java.api.tree.SwitchStatementTree
        /**
        * Switch expressions introduced with support Java 12
        * @since SonarJava 5.12
        */
        SwitchExpressionTree asSwitchExpression();
        ```
        ```
        //org.sonar.plugins.java.api.tree.CaseLabelTree
        /**
        * @return true for case with colon: "case 3:" or "default:"
        *         false for case with arrow: "case 3 ->" or "default ->"
        * @since 5.12 (Java 12 new features)
        */
        boolean isFallThrough();
        
        /**
        * @since 5.12 (Java 12 new features)
        */
        SyntaxToken colonOrArrowToken();
        ```
        ```
        //org.sonar.plugins.java.api.tree.BreakStatementTree
        /**
        * @since 5.12 (Java 12 new features)
        */
        @Nullable
        ExpressionTree value();
        ```
        ```
        //org.sonar.plugins.java.api.tree.TreeVisitor
        void visitSwitchExpression(SwitchExpressionTree tree);
        ```

#### **5.7**
* **Breaking**  
    * This change will impact mostly the custom rules relying on semantic API. The type returned by some symbols will change from raw type to parameterized type with identity substitution and this will change how subtyping will answer.

    It is possible to get the previous behavior back by using type erasure on the newly returned type. Note that not all returned types are impacted by this change.

    Example:
    ```
    @Rule(key = "MyFirstCustomRule")
    public class MyFirstCustomCheck extends IssuableSubscriptionVisitor {
    
        @Override
        public List<Kind> nodesToVisit() {
            return ImmutableList.of(Kind.METHOD);
        }
    
        @Override
        public void visitNode(Tree tree) {
            MethodTree method = (MethodTree) tree;
            MethodSymbol symbol = method.symbol();
            
            Type returnType = symbol.returnType().type();
            // When analyzing the code "MyClass<Integer> foo() {return null; }"
            // BEFORE: returnType == ClassJavaType
            // NOW: returnType == ParametrizedTypeJavaType
    
            // Getting back previous type
            Type erasedType = returnType.erasure();
            // erasedType == ClassJavaType
        }
    }
    ```
<!-- /sonarqube -->

## Related Pages

* [Test Coverage & Execution](/analysis/coverage/) ([SpotBugs](https://spotbugs.github.io/), FindBugs, [FindSecBugs](https://github.com/find-sec-bugs/find-sec-bugs/wiki/Maven-configuration), [PMD](http://maven.apache.org/plugins/maven-pmd-plugin/usage.html), [Checkstyle](http://maven.apache.org/plugins/maven-checkstyle-plugin/checkstyle-mojo))
* [Importing External Issues](/analysis/external-issues/) (JaCoCo, Surefire)
<!-- sonarqube -->
* [Adding Coding Rules](/extend/adding-coding-rules/)
<!-- /sonarqube -->
