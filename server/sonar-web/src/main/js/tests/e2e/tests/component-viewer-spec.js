var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('component-viewer-spec');


casper.test.begin('Component Viewer Base Tests', function (test) {
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
        lib.mockRequest('*', '{}'); // Trick to see all ajax requests
      })

      .then(function () {
        casper.waitForSelector('.component-viewer-source .row', function () {

          // Check header elements
          test.assertElementCount('.component-viewer-header', 1);
          test.assertSelectorContains('.component-viewer-header-component-project', 'SonarQube');
          test.assertSelectorContains('.component-viewer-header-component-project', 'SonarQube :: Batch');
          test.assertSelectorContains('.component-viewer-header-component-name',
              'src/main/java/org/sonar/batch/index/Cache.java');
          test.assertElementCount('.component-viewer-header-favorite', 1);
          test.assertElementCount('.component-viewer-header-actions', 1);

          // Check main measures
          test.assertSelectorContains('.js-header-tab-basic', '379');
          test.assertSelectorContains('.js-header-tab-issues', 'A');
          test.assertSelectorContains('.js-header-tab-issues', '3h 30min');
          test.assertSelectorContains('.js-header-tab-issues', '6');
          test.assertSelectorContains('.js-header-tab-coverage', '74.3%');
          test.assertElementCount('.js-header-tab-scm', 1);

          // Check source
          test.assertElementCount('.component-viewer-source .row', 520);
          test.assertSelectorContains('.component-viewer-source', 'public class Cache');

          // Check workspace
          test.assertElementCount('.component-viewer-workspace', 1);
        });
      })

      .then(function() {
        // Check issues decoration
        casper.click('.js-toggle-issues');
        casper.waitForSelector('.code-issue', function () {
          test.assertElementCount('.code-issue', 6);
          test.assertSelectorContains('.code-issue', '2 duplicated blocks of code.');

          casper.click('.js-toggle-issues');
          casper.waitWhileSelector('.code-issue', function () {
            test.assertElementCount('.code-issue', 0);
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
            test.assertElementCount('.coverage-green', 0);
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
            test.assertElementCount('.duplication-exists', 0);
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
            test.assertElementCount('.scm-author', 0);
            test.assertElementCount('.scm-date', 0);
          });
        });
      })

      .run(function () {
        test.done();
      });
});
