var lib = require('../lib'),
    testName = lib.testName('Component Viewer');

lib.initMessages();
lib.changeWorkingDirectory('component-viewer-spec');


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
