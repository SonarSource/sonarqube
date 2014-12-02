/* global casper:false */

var lib = require('../lib'),
    testName = lib.testName('Source Viewer');

lib.initMessages();
lib.changeWorkingDirectory('source-viewer-should-show-measures');


casper.test.begin(testName('Should Show Measures'), function (test) {
  casper
      .start(lib.buildUrl('source-viewer'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'api-components-app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'api-sources-lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'api-issues-search.json');
        lib.mockRequestFromFile('/api/resources', 'api-resources.json');
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
          'accessors',
          'classes',
          'functions',
          'statements',
          'ncloc',
          'lines',
          'generated_ncloc',
          'generated_lines',
          'complexity',
          'function_complexity',
          'comment_lines',
          'comment_lines_density',
          'public_api',
          'public_undocumented_api',
          'public_documented_api_density',

          'coverage',
          'line_coverage',
          'lines_to_cover',
          'uncovered_lines',
          'branch_coverage',
          'conditions_to_cover',
          'uncovered_conditions',
          'it_coverage',
          'it_line_coverage',
          'it_lines_to_cover',
          'it_uncovered_lines',
          'it_branch_coverage',
          'it_conditions_to_cover',
          'it_uncovered_conditions',
          'overall_coverage',
          'overall_line_coverage',
          'overall_lines_to_cover',
          'overall_uncovered_lines',
          'overall_branch_coverage',
          'overall_conditions_to_cover',
          'overall_uncovered_conditions',

          'violations',
          'sqale_index',
          'sqale_debt_ratio',
          'blocker_violations',
          'critical_violations',
          'major_violations',
          'minor_violations',
          'info_violations',

          'duplicated_lines_density',
          'duplicated_blocks',
          'duplicated_lines'
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
