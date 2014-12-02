/* global casper:false */

var lib = require('../lib'),
    testName = lib.testName('Source Viewer');

lib.initMessages();
lib.changeWorkingDirectory('source-viewer-spec');


casper.test.begin(testName('Base'), function (test) {
  casper
      .start(lib.buildUrl('source-viewer'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
      })

      .then(function () {
        casper.waitForSelector('.source-line', function () {
          // Check header elements
          test.assertExists('.source-viewer-header');
          test.assertSelectorContains('.source-viewer-header-component-project', 'SonarQube');
          test.assertSelectorContains('.source-viewer-header-component-project', 'SonarQube :: Batch');
          test.assertSelectorContains('.source-viewer-header-component-name',
              'src/main/java/org/sonar/batch/index/Cache.java');
          test.assertExists('.source-viewer-header-favorite');
          test.assertExists('.source-viewer-header-actions');

          // Check main measures
          // FIXME enable lines check
          //test.assertSelectorContains('.source-viewer-header-measure', '379');
          test.assertSelectorContains('.source-viewer-header-measure', 'A');
          test.assertSelectorContains('.source-viewer-header-measure', '2h 10min');
          test.assertSelectorContains('.source-viewer-header-measure', '6');
          test.assertSelectorContains('.source-viewer-header-measure', '74.3%');
          test.assertSelectorContains('.source-viewer-header-measure', '5.8%');

          // Check source
          // FIXME enable source lines count check
          //test.assertElementCount('.source-line', 518);
          test.assertSelectorContains('.source-viewer', 'public class Cache');
        });
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Decoration'), function (test) {
  casper
      .start(lib.buildUrl('source-viewer'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
      })

      .then(function () {
        casper.waitForSelector('.source-line');
      })

      .then(function () {
        // Check issues decoration
        test.assertElementCount('.has-issues', 6);
      })

      .then(function () {
        // Check coverage decoration
        test.assertElementCount('.source-line-covered', 142);
        test.assertElementCount('.source-line-uncovered', 50);
        test.assertElementCount('.source-line-partially-covered', 2);
      })

      .then(function () {
        // Check duplications decoration
        test.assertElementCount('.source-line-duplicated', 30);
      })

      .then(function () {
        // Check scm decoration
        test.assertElementCount('.source-line-scm-inner', 186);
        test.assertExists('.source-line-scm-inner[data-author="simon.brandhof@gmail.com"]');
        test.assertExists('.source-line-scm-inner[data-author="julien.henry@sonarsource.com"]');
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Test File'), function (test) {
  casper
      .start(lib.buildUrl('source-viewer'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'tests/app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'tests/lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
      })

      .then(function () {
        casper.waitForSelector('.source-line');
      })

      .then(function () {
        test.assertSelectorContains('.source-viewer-header-measure', '6');
      })

      .run(function () {
        test.done();
      });
});


// FIXME enable test
//casper.test.begin(testName('Go From Coverage to Test File'), function (test) {
//  casper
//      .start(lib.buildUrl('source-viewer'), function () {
//        lib.setDefaultViewport();
//        lib.mockRequest('/api/l10n/index', '{}');
//        lib.mockRequestFromFile('/api/components/app', 'app.json');
//        lib.mockRequestFromFile('/api/sources/lines', 'lines.json');
//        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
//        lib.mockRequestFromFile('/api/tests/test_cases', 'test-cases.json');
//      })
//
//      .then(function () {
//        casper.waitForSelector('.component-viewer-source .source-line');
//      })
//
//      .then(function () {
//        casper.click('.js-toggle-coverage');
//        casper.waitForSelector('.source-line-covered', function () {
//          casper.click('.source-line-covered');
//          casper.waitForSelector('.bubble-popup', function () {
//            test.assertSelectorContains('.bubble-popup', 'src/test/java/org/sonar/batch/issue/IssueCacheTest.java');
//            test.assertSelectorContains('.bubble-popup', 'should_update_existing_issue');
//            test.assertSelectorContains('.bubble-popup li[title="should_update_existing_issue"]', '293');
//
//            lib.clearRequestMocks();
//            lib.mockRequestFromFile('/api/components/app', 'tests/app.json');
//            lib.mockRequestFromFile('/api/sources/show', 'tests/source.json');
//            lib.mockRequestFromFile('/api/resources', 'tests/resources.json');
//            lib.mockRequest('/api/coverage/show', '{}');
//            lib.mockRequestFromFile('/api/tests/show', 'tests/tests.json');
//            casper.click('.component-viewer-popup-test-file[data-key]');
//
//            casper.waitForSelector('.js-unit-test', function () {
//              test.assertElementCount('.js-unit-test', 2);
//            });
//          });
//        });
//      })
//
//      .run(function () {
//        test.done();
//      });
//});


casper.test.begin(testName('Create Manual Issue'), function (test) {
  casper
      .start(lib.buildUrl('source-viewer'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
        lib.mockRequestFromFile('/api/issues/create', 'create-issue.json');
        lib.mockRequestFromFile('/api/issues/show', 'create-issue.json');
      })

      .then(function () {
        casper.waitForSelector('.source-line-number[data-line-number="3"]');
      })

      .then(function () {
        casper.click('.source-line-number[data-line-number="3"]');
        casper.waitForSelector('.js-add-manual-issue');
      })

      .then(function () {
        casper.click('.js-add-manual-issue');
        casper.waitForSelector('.js-manual-issue-form');
      })

      .then(function () {
        casper.fill('.js-manual-issue-form', {
          rule: 'manual:api',
          message: 'An issue message'
        }, true);
      })

      .then(function () {
        casper.waitForSelector('.source-line-code.has-issues[data-line-number="3"]', function () {
          test.assertExists('.source-line-code.has-issues[data-line-number="3"]');
        });
      })

      .run(function () {
        test.done();
      });
});
