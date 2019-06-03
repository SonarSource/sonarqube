---
title: Python
url: /analysis/languages/python/
---

<!-- static -->
[[info]]
| <iframe src="http://update.sonarsource.org/plugins/python-confluence-include.html" height="125px">Your browser does not support iframes.</iframe>
<!-- /static -->


## Supported Versions
* Python 3.X
* Python 2.X

## Language-Specific Properties

Discover and update the Python-specific [properties](/analysis/analysis-parameters/) in: <!-- sonarcloud -->Project <!-- /sonarcloud --> **[Administration > General Settings > Python](/#sonarqube-admin#/admin/settings?category=python)**.

## Pylint
[Pylint](http://www.pylint.org/) is an external static source code analyzer used to augment Python analysis. To include Pylint issues, first generate an issues report:
```
pylint <module_or_package> -r n --msg-template="{path}:{line}: [{msg_id}({symbol}), {obj}] {msg}" > <report_file>
```
Then pass it in to analysis with the `sonar.python.pylint.reportPath` property.

The analyzer will execute Pylint for you if you haven't specified the path to a Pylint report. The path to your installation of `pylint` can be tuned using the `sonar.python.pylint` property, and non-default a properties file can be specified with `sonar.python.pylint_config`.


## Related Pages
* [Importing External Issues](/analysis/external-issues/) ([Pylint](http://www.pylint.org/), [Bandit](https://github.com/PyCQA/bandit/blob/master/README.rst))
* [Test Coverage & Execution](/analysis/coverage/) (the [Coverage Tool](http://nedbatchelder.com/code/coverage/) provided by [Ned Batchelder](http://nedbatchelder.com/), [Nose](https://nose.readthedocs.org/en/latest/), [pytest](https://docs.pytest.org/en/latest/))
