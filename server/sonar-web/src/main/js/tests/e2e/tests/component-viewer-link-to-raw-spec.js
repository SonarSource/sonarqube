var lib = require('../lib'),
    testName = lib.testName('Component Viewer');

lib.initMessages();
lib.changeWorkingDirectory('component-viewer-spec');


casper.test.begin(testName('Link to Raw'), function (test) {
  casper
      .start(lib.buildUrl('component-viewer#component=component'), function () {
        lib.setDefaultViewport();
        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/show', 'source.json');
      })

      .then(function () {
        casper.waitForSelector('.component-viewer-source .row');
      })

      .then(function () {
        casper.waitForSelector('.js-actions');
        casper.click('.js-actions');
        casper.waitForSelector('.js-raw-source', function () {
          casper.click('.js-raw-source');
        });
      })

      .then(function () {
        test.assertUrlMatch('org.codehaus.sonar:sonar-batch:src/main/java/org/sonar/batch/index/Cache.java');
      })

      .run(function () {
        test.done();
      });
});
