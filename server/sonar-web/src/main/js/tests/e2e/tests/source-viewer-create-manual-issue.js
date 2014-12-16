/* global casper:false */

var lib = require('../lib'),
    testName = lib.testName('Source Viewer');

lib.initMessages();
lib.changeWorkingDirectory('source-viewer-create-manual-issue');


casper.test.begin(testName('source-viewer-create-manual-issue'), function (test) {
  casper
      .start(lib.buildUrl('source-viewer'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
        lib.mockRequestFromFile('/api/issues/create', 'create-issue.json');
        lib.mockRequestFromFile('/api/issues/show', 'create-issue.json');
        lib.mockRequestFromFile('/api/rules/search*', 'api-rules-search.json');
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
