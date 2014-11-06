var lib = require('../lib'),
    testName = lib.testName('Component Viewer');

lib.initMessages();
lib.changeWorkingDirectory('component-viewer-spec');


casper.test.begin(testName('Base'), function (test) {
  casper
      .start(lib.buildUrl('component-viewer#component=component'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/show', 'source.json');
      })

      .then(function () {
        casper.waitForSelector('.component-viewer-source .row', function () {
          // Check header elements
          test.assertExists('.component-viewer-header');
          test.assertSelectorContains('.component-viewer-header-component-project', 'SonarQube');
          test.assertSelectorContains('.component-viewer-header-component-project', 'SonarQube :: Batch');
          test.assertSelectorContains('.component-viewer-header-component-name',
              'src/main/java/org/sonar/batch/index/Cache.java');
          test.assertExists('.component-viewer-header-favorite');
          test.assertExists('.component-viewer-header-actions');

          // Check main measures
          test.assertSelectorContains('.js-header-tab-basic', '379');
          test.assertSelectorContains('.js-header-tab-issues', 'A');
          test.assertSelectorContains('.js-header-tab-issues', '3h 30min');
          test.assertSelectorContains('.js-header-tab-issues', '6');
          test.assertSelectorContains('.js-header-tab-coverage', '74.3%');
          test.assertExists('.js-header-tab-scm');

          // Check source
          test.assertElementCount('.component-viewer-source .row', 520);
          test.assertSelectorContains('.component-viewer-source', 'public class Cache');

          // Check workspace
          test.assertExists('.component-viewer-workspace');
        });
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Decoration'), function (test) {
  casper
      .start(lib.buildUrl('component-viewer#component=component'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/show', 'source.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
        lib.mockRequestFromFile('/api/coverage/show', 'coverage.json');
        lib.mockRequestFromFile('/api/duplications/show', 'duplications.json');
        lib.mockRequestFromFile('/api/sources/scm', 'scm.json');
      })

      .then(function () {
        casper.waitForSelector('.component-viewer-source .row');
      })

      .then(function () {
        // Check issues decoration
        casper.click('.js-toggle-issues');
        casper.waitForSelector('.code-issue', function () {
          test.assertElementCount('.code-issue', 6);
          test.assertSelectorContains('.code-issue', '2 duplicated blocks of code.');

          casper.click('.js-toggle-issues');
          casper.waitWhileSelector('.code-issue', function () {
            test.assertDoesntExist('.code-issue');
          });
        });
      })

      .then(function () {
        // Check coverage decoration
        casper.click('.js-toggle-coverage');
        casper.waitForSelector('.coverage-green', function () {
          test.assertElementCount('.coverage-green', 149);
          test.assertSelectorContains('.coverage-green', '27');
          test.assertElementCount('.coverage-red', 51);
          test.assertElementCount('.coverage-orange', 2);
          test.assertSelectorContains('.coverage-orange', '1/2');

          casper.click('.js-toggle-coverage');
          casper.waitWhileSelector('.coverage-green', function () {
            test.assertDoesntExist('.coverage-green');
          });
        });
      })

      .then(function () {
        // Check duplications decoration
        casper.click('.js-toggle-duplications');
        casper.waitForSelector('.duplication-exists', function () {
          test.assertElementCount('.duplication-exists', 32);

          casper.click('.js-toggle-duplications');
          casper.waitWhileSelector('.duplication-exists', function () {
            test.assertDoesntExist('.duplication-exists');
          });
        });
      })

      .then(function () {
        // Check scm decoration
        casper.click('.js-toggle-scm');
        casper.waitForSelector('.scm-author', function () {
          test.assertElementCount('.scm-author', 182);
          test.assertElementCount('.scm-date', 182);
          test.assertSelectorContains('.scm-author', 'simon.brandhof@gmail.com');
          test.assertSelectorContains('.scm-author', 'julien.henry@sonarsource.com');
          test.assertSelectorContains('.scm-date', '2014-02-20');

          casper.click('.js-toggle-scm');
          casper.waitWhileSelector('.scm-author', function () {
            test.assertDoesntExist('.scm-author');
            test.assertDoesntExist('.scm-date');
          });
        });
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Header'), function (test) {
  casper
      .start(lib.buildUrl('component-viewer#component=component'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/show', 'source.json');
        lib.mockRequestFromFile('/api/resources', 'resources.json');
      })

      .then(function () {
        casper.waitForSelector('.component-viewer-source .row');
      })

      .then(function () {
        // Check issues header and filters
        casper.click('.js-header-tab-issues');
        casper.waitForSelector('.js-filter-unresolved-issues', function () {
          test.assertExists('.js-filter-open-issues');
          test.assertExists('.js-filter-fixed-issues');
          test.assertExists('.js-filter-false-positive-issues');
          test.assertSelectorContains('.js-filter-MAJOR-issues', '1');
          test.assertSelectorContains('.js-filter-MINOR-issues', '1');
          test.assertSelectorContains('.js-filter-INFO-issues', '4');
          test.assertSelectorContains('.js-filter-rule[data-rule="common-java:DuplicatedBlocks"]', '1');
          test.assertSelectorContains('.js-filter-rule[data-rule="squid:S1192"]', '1');
          test.assertSelectorContains('.js-filter-rule[data-rule="squid:S1135"]', '4');
          test.assertExists('.js-issues-time-changes');

          casper.click('.js-header-tab-issues');
          casper.waitWhileSelector('.js-filter-unresolved-issues', function () {
            test.assertDoesntExist('.js-filter-open-issues');
            test.assertDoesntExist('.js-filter-MAJOR-issues');
            test.assertDoesntExist('.js-filter-rule');
          });
        });
      })

      .then(function () {
        // Check coverage header and filters
        casper.click('.js-header-tab-coverage');
        casper.waitForSelector('.js-filter-lines-to-cover', function () {
          test.assertExists('.js-filter-uncovered-lines');
          test.assertExists('.js-filter-branches-to-cover');
          test.assertExists('.js-filter-uncovered-branches');
          test.assertSelectorContains('[data-metric="coverage"]', '74.3%');
          test.assertSelectorContains('[data-metric="line_coverage"]', '74.2%');
          test.assertSelectorContains('[data-metric="lines_to_cover"]', '194');
          test.assertSelectorContains('[data-metric="uncovered_lines"]', '50');
          test.assertSelectorContains('[data-metric="branch_coverage"]', '75.0%');
          test.assertSelectorContains('[data-metric="conditions_to_cover"]', '16');
          test.assertSelectorContains('[data-metric="uncovered_conditions"]', '4');
          test.assertExists('.js-coverage-time-changes');

          casper.click('.js-header-tab-coverage');
          casper.waitWhileSelector('.js-filter-lines-to-cover', function () {
            test.assertDoesntExist('.js-filter-uncovered-lines');
            test.assertDoesntExist('[data-metric="coverage"]');
            test.assertDoesntExist('[data-metric="branch_coverage"]');
          });
        });
      })

      .then(function () {
        // Check duplications header and filters
        casper.click('.js-header-tab-duplications');
        casper.waitForSelector('.js-filter-duplications', function () {
          test.assertSelectorContains('[data-metric="duplicated_blocks"]', '2');
          test.assertSelectorContains('[data-metric="duplicated_lines"]', '30');

          casper.click('.js-header-tab-duplications');
          casper.waitWhileSelector('.js-filter-duplications', function () {
            test.assertDoesntExist('[data-metric="duplicated_blocks"]');
            test.assertDoesntExist('[data-metric="duplicated_lines"]');
          });
        });
      })

      .then(function () {
        // Check scm header and filters
        casper.click('.js-header-tab-scm');
        casper.waitForSelector('.js-filter-modified-lines', function () {
          test.assertExists('.js-scm-time-changes');

          casper.click('.js-header-tab-scm');
          casper.waitWhileSelector('.js-filter-modified-lines', function () {
            test.assertDoesntExist('.js-scm-time-changes');
          });
        });
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Test File'), function (test) {
  casper
      .start(lib.buildUrl('component-viewer#component=component'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'tests/app.json');
        lib.mockRequestFromFile('/api/sources/show', 'tests/source.json');
        lib.mockRequestFromFile('/api/resources', 'tests/resources.json');
        lib.mockRequestFromFile('/api/tests/show', 'tests/tests.json');
        lib.mockRequestFromFile('/api/tests/covered_files', 'tests/covered-files.json');
      })

      .then(function () {
        casper.waitForSelector('.component-viewer-source .row');
      })

      .then(function () {
        // Check coverage header and filters
        casper.click('.js-header-tab-tests');
        casper.waitForSelector('.js-unit-test', function () {
          test.assertSelectorContains('[data-metric="test_execution_time"]', '12');
          test.assertElementCount('.js-unit-test', 2);
          test.assertSelectorContains('.js-unit-test[data-name="should_return_i"]', 'should_return_i');
          test.assertSelectorContains('.js-unit-test[data-name="should_return_i"]', '5');
          test.assertSelectorContains('.js-unit-test[data-name="should_return_to_string"]', 'should_return_to_string');
          test.assertSelectorContains('.js-unit-test[data-name="should_return_to_string"]', '4');

          casper.click('.js-unit-test[data-name="should_return_to_string"]');
          casper.waitForSelector('.bubble-popup', function () {
            test.assertSelectorContains('.bubble-popup', 'Sample.java');
            test.assertSelectorContains('.bubble-popup', 'src/main/java/sample');
          });
        });
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Go From Coverage to Test File'), function (test) {
  casper
      .start(lib.buildUrl('component-viewer#component=component'), function () {
        lib.setDefaultViewport();
        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/show', 'source.json');
        lib.mockRequestFromFile('/api/coverage/show', 'coverage.json');
        lib.mockRequestFromFile('/api/tests/test_cases', 'test-cases.json');
      })

      .then(function () {
        casper.waitForSelector('.component-viewer-source .row');
      })

      .then(function () {
        casper.click('.js-toggle-coverage');
        casper.waitForSelector('.coverage-green', function () {
          casper.click('.coverage-green .coverage-tests');
          casper.waitForSelector('.bubble-popup', function () {
            test.assertSelectorContains('.bubble-popup', 'src/test/java/org/sonar/batch/issue/IssueCacheTest.java');
            test.assertSelectorContains('.bubble-popup', 'should_update_existing_issue');
            test.assertSelectorContains('.bubble-popup li[title="should_update_existing_issue"]', '293');

            lib.clearRequestMocks();
            lib.mockRequestFromFile('/api/components/app', 'tests/app.json');
            lib.mockRequestFromFile('/api/sources/show', 'tests/source.json');
            lib.mockRequestFromFile('/api/resources', 'tests/resources.json');
            lib.mockRequest('/api/coverage/show', '{}');
            lib.mockRequestFromFile('/api/tests/show', 'tests/tests.json');
            casper.click('.component-viewer-popup-test-file[data-key]');

            casper.waitForSelector('.js-unit-test', function () {
              test.assertElementCount('.js-unit-test', 2);
            });
          });
        });
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Coverage Filters'), function (test) {
  casper
      .start(lib.buildUrl('component-viewer#component=component'), function () {
        lib.setDefaultViewport();
        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/show', 'source.json');
        lib.mockRequestFromFile('/api/resources', 'resources.json');
        lib.mockRequestFromFile('/api/coverage/show', 'coverage.json');
      })

      .then(function () {
        casper.waitForSelector('.component-viewer-source .row');
      })

      .then(function () {
        casper.click('.js-header-tab-coverage');
        casper.waitForSelector('.js-filter-lines-to-cover');
      })

      .then(function () {
        casper.click('.js-filter-lines-to-cover');
        casper.waitForSelector('.coverage-green', function () {
          test.assertElementCount('.coverage-green', 149);
          test.assertElementCount('.coverage-red', 51);
          test.assertElementCount('.coverage-orange', 2);
          test.assertElementCount('.component-viewer-source .row', 369);
        });
      })

      .then(function () {
        casper.click('.js-filter-uncovered-lines');
        casper.waitForSelector('.coverage-green', function () {
          test.assertElementCount('.coverage-green', 18);
          test.assertElementCount('.coverage-red', 51);
          test.assertElementCount('.coverage-orange', 0);
          test.assertElementCount('.component-viewer-source .row', 136);
        });
      })

      .then(function () {
        casper.click('.js-filter-branches-to-cover');
        casper.waitForSelector('.coverage-green', function () {
          test.assertElementCount('.coverage-green', 26);
          test.assertElementCount('.coverage-red', 4);
          test.assertElementCount('.coverage-orange', 2);
          test.assertElementCount('.component-viewer-source .row', 33);
        });
      })

      .then(function () {
        casper.click('.js-filter-uncovered-branches');
        casper.waitForSelector('.coverage-green', function () {
          test.assertElementCount('.coverage-green', 6);
          test.assertElementCount('.coverage-red', 4);
          test.assertElementCount('.coverage-orange', 2);
          test.assertElementCount('.component-viewer-source .row', 13);
        });
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Ability to Deselect Filters'), function (test) {
  casper
      .start(lib.buildUrl('component-viewer#component=component'), function () {
        lib.setDefaultViewport();
        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/show', 'source.json');
        lib.mockRequestFromFile('/api/resources', 'resources.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
        lib.mockRequestFromFile('/api/coverage/show', 'coverage.json');
        lib.mockRequestFromFile('/api/duplications/show', 'duplications.json');
      })

      .then(function () {
        casper.waitForSelector('.component-viewer-source .row');
      })

      .then(function () {
        casper.click('.js-header-tab-issues');
        var testFilter = '.js-filter-unresolved-issues';
        casper.waitForSelector(testFilter + '.active', function () {
          lib.waitForElementCount('.component-viewer-source .row', 56, function () {
            casper.click(testFilter);
            lib.waitForElementCount('.component-viewer-source .row', 520, function () {
              test.assertDoesntExist(testFilter + '.active');
              casper.click(testFilter);
              lib.waitForElementCount('.component-viewer-source .row', 56, function () {
                test.assertExists(testFilter + '.active');
              });
            });
          })
        });
      })

      .then(function () {
        casper.click('.js-header-tab-coverage');
        var testFilter = '.js-filter-lines-to-cover';
        casper.waitForSelector(testFilter + '.active', function () {
          lib.waitForElementCount('.component-viewer-source .row', 369, function () {
            casper.click(testFilter);
            lib.waitForElementCount('.component-viewer-source .row', 520, function () {
              test.assertDoesntExist(testFilter + '.active');
              casper.click(testFilter);
              lib.waitForElementCount('.component-viewer-source .row', 369, function () {
                test.assertExists(testFilter + '.active');
              });
            });
          })
        });
      })

      .then(function () {
        casper.click('.js-header-tab-duplications');
        var testFilter = '.js-filter-duplications';
        casper.waitForSelector(testFilter + '.active', function () {
          lib.waitForElementCount('.component-viewer-source .row', 39, function () {
            casper.click(testFilter);
            lib.waitForElementCount('.component-viewer-source .row', 520, function () {
              test.assertDoesntExist(testFilter + '.active');
              casper.click(testFilter);
              lib.waitForElementCount('.component-viewer-source .row', 39, function () {
                test.assertExists(testFilter + '.active');
              });
            });
          })
        });
      })

      .run(function () {
        test.done();
      });
});
