/* global casper:false */

var lib = require('../lib'),
    testName = lib.testName('Source Viewer');

lib.initMessages();
lib.changeWorkingDirectory('source-viewer-should-show-measures-for-test-file');

casper.test.begin(testName('Should Show Measures For Test File'), function (test) {
  casper
      .start(lib.buildUrl('source-viewer'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'api-components-app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'api-sources-lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'api-issues-search.json');
        lib.mockRequestFromFile('/api/resources', 'api-resources.json');
        lib.mockRequestFromFile('/api/tests/show', 'api-tests-show.json');
      })

      .then(function () {
        casper.waitForSelector('.source-line');
      })

      .then(function () {
        casper.click('.js-actions');
        casper.waitForSelector('.js-measures', function () {
          casper.click('.js-measures');
          casper.waitForSelector('.source-viewer-measures');
        });
      })

      .then(function () {
        // The test data is built the specific way when the formatted value
        // of a measure is equal to the measure name.
        var metrics = [
          'tests',
          'test_success_density',
          'test_failures',
          'test_errors',
          'skipped_tests',
          'test_execution_time'
        ];
        metrics.forEach(function (metric) {
          test.assertSelectorContains('.measure[data-metric=' + metric + ']', metric);
        });
      })

      .then(function () {
        casper.click('.overlay-popup-close');
        test.assertDoesntExist('.source-viewer-measures');
      })

      .run(function () {
        test.done();
      });
});
