/* global casper:false */

var lib = require('../lib'),
    testName = lib.testName('Source Viewer');

lib.initMessages();
lib.changeWorkingDirectory('source-viewer-spec');


casper.test.begin(testName('Link to Raw'), function (test) {
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
        casper.click('.js-actions');
        casper.waitForSelector('.js-raw-source', function () {
          casper.click('.js-raw-source');
        });
      })

      .then(function () {
        casper.withPopup(/Cache\.java/, function () {
          this.test.assertUrlMatch('org.codehaus.sonar:sonar-batch:src/main/java/org/sonar/batch/index/Cache.java');
        });
      })

      .run(function () {
        test.done();
      });
});
