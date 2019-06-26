---
title: PL/SQL
url: /analysis/languages/plsql/
---

<!-- static -->
<!-- update_center:plsql -->
<!-- /static -->


## Language-Specific Properties

Discover and update the PL/SQL-specific [properties](/analysis/analysis-parameters/) in: <!-- sonarcloud -->Project <!-- /sonarcloud --> **[Administration > General Settings > PL/SQL](/#sonarqube-admin#/admin/settings?category=pl%2Fsql)**

## Advanced parameters

### Default Schema
Parameter | Description
--- | ---
`sonar.plsql.defaultSchema` | When a schema object (table, view, index, synonym) is referenced in SQL code without a schema prefix, the analyzer will assume that it belongs to this schema.


### Data Dictionary
Some rules raise issues only when a data dictionary is provided during analysis. To provide a data dictionary, you must define the following properties in the `sonar-project.properties` file or on the scanner command line using the  `-D` prefix:


|Parameter|Description|
| --- | --- | 
|`sonar.plsql.jdbc.url`|URL of the JDBC connection. **Required for data dictionary lookup**. For example: `jdbc:oracle:thin:@my-oracle-server:1521/my-db`
|`sonar.plsql.jdbc.user`|JDBC user to authenticate the connection.
|`sonar.plsql.jdbc.password`|JDBC password provided to authenticate the connection.
|`sonar.plsql.jdbc.driver.path`|Path or URL of the Oracle jdbc driver jar.
|`sonar.plsql.jdbc.driver.class`|Java class name of the Oracle Driver. For example: `oracle.jdbc.OracleDriver`

Providing this configuration allows SonarPLSQL to query data dictionary views such as `SYS.ALL_TAB_COLUMNS` in order to to better analyze your SQL.


<!-- sonarqube -->
## Related Pages
* [Adding Coding Rules](/extend/adding-coding-rules/)
<!-- /sonarqube -->

