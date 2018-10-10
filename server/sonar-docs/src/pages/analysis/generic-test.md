---
title: Generic Test Data
url: /analysis/generic-test/
---

Out of the box, {instance} supports generic formats for test coverage and test execution import. If your coverage engines' native output formats aren't supported by your language plugins, simply covert them to these formats.

## Generic Coverage
Report paths should be passed in a comma-delimited list to:

 * `sonar.coverageReportPaths`

The supported format is described by the `sonar-generic-coverage.xsd`:

	<xs:schema>
	  <xs:element name="coverage">
		<xs:complexType>
		  <xs:sequence>
			<xs:element name="file" minOccurs="0" maxOccurs="unbounded">
			  <xs:complexType>
				<xs:sequence>
				  <xs:element name="lineToCover" minOccurs="0" maxOccurs="unbounded">
					<xs:complexType>
					  <xs:attribute name="lineNumber" type="xs:positiveInteger" use="required"/>
					  <xs:attribute name="covered" type="xs:boolean" use="required"/>
					  <xs:attribute name="branchesToCover" type="xs:nonNegativeInteger"/>
					  <xs:attribute name="coveredBranches" type="xs:nonNegativeInteger"/>
					</xs:complexType>
				  </xs:element>
				</xs:sequence>
			  <xs:attribute name="path" type="xs:string" use="required"/>
			  </xs:complexType>
			</xs:element>
		  </xs:sequence>
		  <xs:attribute name="version" type="xs:positiveInteger" use="required"/>
		</xs:complexType>
	  </xs:element>
	</xs:schema>

and looks like this:

	<coverage version="1">
	  <file path="xources/hello/NoConditions.xoo">
		<lineToCover lineNumber="6" covered="true"/>
		<lineToCover lineNumber="7" covered="false"/>
	  </file>
	  <file path="xources/hello/WithConditions.xoo">
		<lineToCover lineNumber="3" covered="true" branchesToCover="2" coveredBranches="1"/>
	  </file>
	</coverage>

The root node should be named `coverage`. Its version attribute should be set to `1`.

Insert a `file` element for each file which can be covered by tests. Its `path` attribute can be either absolute or relative to the root of the module.
Inside a `file` element, insert a `lineToCover` for each line which can be covered by unit tests. It can have the following attributes:
* `lineNumber` (mandatory)
* `covered` (mandatory): boolean value indicating whether tests actually hit that line
* `branchesToCover` (optional): number of branches which can be covered
* `coveredBranches` (optional): number of branches which are actually covered by tests

## Generic Execution
Report paths should be passed in a comma-delimited list to:

* `sonar.testExecutionReportPaths`

The supported format looks like this:

	<testExecutions version="1">
	  <file path="testx/ClassOneTest.xoo">
		<testCase name="test1" duration="5"/>
		<testCase name="test2" duration="500">
		  <skipped message="short message">other</skipped>
		</testCase>
		<testCase name="test3" duration="100">
		  <failure message="short">stacktrace</failure>
		</testCase>
		<testCase name="test4" duration="500">
		  <error message="short">stacktrace</error>
		</testCase>
	  </file>
	</testExecutions>
	
The root node should be named `testExecutions`. Its version attribute should be set to `1`.

Insert a `file` element for each test file. Its `path` attribute can be either absolute or relative to the root of the module.

**Note** unlike for coverage reports, the files present in the report must be test file names, not source code files covered by tests.

Inside a `file` element, insert a `testCase` for each test run by unit tests. It can have the following attributes/children:

* `testCase` (mandatory)
  * `name` (mandatory): name of the test case
  * `duration` (mandatory): long value in milliseconds
 
  * `failure|error|skipped` (optional): if the test is not OK, report the cause with a message and a long description
    * `message` (mandatory): short message describing the cause
    * `stacktrace` (optional): long message containing details about `failure|error|skipped` status
