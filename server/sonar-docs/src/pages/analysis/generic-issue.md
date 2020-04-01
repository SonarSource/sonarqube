---
title: Generic Issue Import Format
url: /analysis/generic-issue/
---

SonarQube supports a generic import format for raising external issues in code. You can use this format to import issues from your favorite linter even if there's no plugin for it. SonarQube also supports many third-party issue report formats, see [Importing Third-Party Issues](/analysis/external-issues/) for more information.

There are a couple of limitations with importing external issues:

* you can't manage them within SonarQube; for instance, there is no ability to mark them False Positive.
* you can't manage the activation of the rules that raise these issues within SonarQube. External rules aren't visible on the Rules page or reflected in Quality Profiles.

External issues and the rules that raise them must be managed in the configuration of your linter. 

## Import 
The analysis parameter `sonar.externalIssuesReportPaths` accepts a comma-delimited list of paths to reports.

Each report must contain, at top-level, an array of `Issue` objects named `issues`.

#### Issue fields:

* `engineId` - string
* `ruleId` - string
* `primaryLocation` - Location object 
* `type` - string. One of BUG, VULNERABILITY, CODE_SMELL
* `severity` - string. One of BLOCKER, CRITICAL, MAJOR, MINOR, INFO
* `effortMinutes` - integer, optional. Defaults to 0
* `secondaryLocations` - array of Location objects, optional

#### Location fields:

* `message` - string
* `filePath` - string
* `textRange` - TextRange object, optional for secondary locations only

#### TextRange fields:

* `startLine` - integer. 1-indexed
* `endLine` - integer, optional. 1-indexed
* `startColumn` - integer, optional. 0-indexed
* `endColumn` - integer, optional. 0-indexed

## Example
Here is an example of the expected format:

	{ "issues": [
		{
		  "engineId": "test",
		  "ruleId": "rule1",
		  "severity":"BLOCKER",
		  "type":"CODE_SMELL",
		  "primaryLocation": {
			"message": "fully-fleshed issue",
			"filePath": "sources/A.java",
			"textRange": {
			  "startLine": 30,
			  "endLine": 30,
			  "startColumn": 9,
			  "endColumn": 14
			}
		  },
		  "effortMinutes": 90,
		  "secondaryLocations": [
			{
			  "message": "cross-file 2ndary location",
			  "filePath": "sources/B.java",
			  "textRange": {
				"startLine": 10,
				"endLine": 10,
				"startColumn": 6,
				"endColumn": 38
			  }
			}
		  ]
		},
		{
		  "engineId": "test",
		  "ruleId": "rule2",
		  "severity": "INFO",
		  "type": "BUG",
		  "primaryLocation": {
			"message": "minimal issue raised at file level",
			"filePath": "sources/Measure.java"
		  }
		}
	]}
