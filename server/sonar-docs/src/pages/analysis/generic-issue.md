---
title: Generic Issue Data
url: /analysis/generic-issue/
---

{instance} supports a generic import format for raising "external" issues in code. It is intended to allow you to import the issues from your favorite linter even if no plugin exists for it.

External issues suffer from two important limitations:

* they cannot be managed within {instance}; for instance, there is no ability to mark them False Positive.
* the activation of the rules that raise these issues cannot be managed within {instance}. In fact, external rules are not visible in the Rules page or reflected in any Quality Profile.

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
