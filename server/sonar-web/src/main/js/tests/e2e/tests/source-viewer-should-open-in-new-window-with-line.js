/* global casper:false */

var lib = require('../lib'),
    testName = lib.testName('Source Viewer');

lib.initMessages();
lib.changeWorkingDirectory('source-viewer-should-open-in-new-window');


casper.test.begin(testName('source-viewer-should-open-in-new-window-with-line'), function (test) {
  casper
      .start(lib.buildUrl('source-viewer'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'api-components-app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'api-sources-lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'api-issues-search.json');
      })

      .then(function () {
        casper.waitForSelector('.source-line');
      })

      .then(function () {
        casper.click('.source-line-number[data-line-number="6"]');
        casper.waitForSelector('.bubble-popup');
      })

      .then(function () {
        casper.click('.js-actions');
        casper.waitForSelector('.js-new-window', function () {
          casper.click('.js-new-window');
        });
      })

      .then(function () {
        casper.withPopup(/Simplest\.java/, function () {
          this.test.assertUrlMatch('test:fake-project-for-tests:src/main/java/foo/Simplest.java');
          this.test.assertUrlMatch('line=6');
        });
      })

      .run(function () {
        test.done();
      });
});
