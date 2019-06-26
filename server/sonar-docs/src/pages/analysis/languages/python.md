---
title: Python
url: /analysis/languages/python/
---

<!-- static -->
<!-- update_center:python -->
<!-- /static -->


## Supported Versions
* Python 3.X
* Python 2.X

## Language-Specific Properties

Discover and update the Python-specific [properties](/analysis/analysis-parameters/) in: <!-- sonarcloud -->Project <!-- /sonarcloud --> **[Administration > General Settings > Python](/#sonarqube-admin#/admin/settings?category=python)**.

## Pylint
[Pylint](http://www.pylint.org/) is an external static source code analyzer, it can be used in conjunction with SonarPython.

You can enable Pylint rules directly in your Python Quality Profile. Their rule keys start with "*Pylint:*".

Once the rules are activated you should run Pylint and import its report:
```
pylint <module_or_package> -r n --msg-template="{path}:{line}: [{msg_id}({symbol}), {obj}] {msg}" > <report_file>
```
Then pass the generated report path to analysis via the `sonar.python.pylint.reportPath` property.

## Related Pages
* [Importing External Issues](/analysis/external-issues/) ([Pylint](http://www.pylint.org/), [Bandit](https://github.com/PyCQA/bandit/blob/master/README.rst))
* [Test Coverage & Execution](/analysis/coverage/) (the [Coverage Tool](http://nedbatchelder.com/code/coverage/) provided by [Ned Batchelder](http://nedbatchelder.com/), [Nose](https://nose.readthedocs.org/en/latest/), [pytest](https://docs.pytest.org/en/latest/))
