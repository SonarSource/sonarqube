/* global casper:false */

var lib = require('../lib'),
    testName = lib.testName('Source Viewer');

lib.initMessages();
lib.changeWorkingDirectory('source-viewer-should-not-show-source-if-no-permission');


casper.test.begin(testName('source-viewer-should-not-show-source-if-no-permission'), function (test) {
  casper
      .start(lib.buildUrl('source-viewer'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'api-components-app.json');
        lib.mockRequest('/api/sources/lines', '{}', { status: 403 });
        lib.mockRequestFromFile('/api/issues/search', 'api-issues-search.json');
      })

      .then(function () {
        casper.waitForSelector('.message-error', function () {
          test.assertDoesntExist('.source-line');
        });
      })

      .run(function () {
        test.done();
      });
});
