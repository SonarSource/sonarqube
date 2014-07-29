var lib = require('../lib'),
    testName = lib.testName('Component Viewer');

lib.initMessages();
lib.changeWorkingDirectory('component-viewer-spec');


casper.test.begin(testName('Lines Filters'), function (test) {
  casper
      .start(lib.buildUrl('component-viewer#component=component'), function () {
        lib.setDefaultViewport();
        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/show', 'source.json');
        lib.mockRequestFromFile('/api/resources', 'resources.json');
      })

      .then(function () {
        casper.waitForSelector('.component-viewer-source .row');
      })

      .then(function () {
        casper.click('.js-header-tab-basic');
        casper.waitForSelector('.js-filter-ncloc');
      })

      .then(function () {
        casper.click('.js-filter-ncloc');
        test.assertElementCount('.component-viewer-source .row', 451);
      })

      .then(function () {
        casper.click('.js-filter-ncloc');
        test.assertElementCount('.component-viewer-source .row', 520);
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Do Not Show Ncloc Filter If No Data'), function (test) {
  casper
      .start(lib.buildUrl('component-viewer#component=component'), function () {
        lib.setDefaultViewport();
        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/show', 'source.json');
        lib.mockRequestFromFile('/api/resources', 'resources-without-ncloc-data.json');
      })

      .then(function () {
        casper.waitForSelector('.component-viewer-source .row');
      })

      .then(function () {
        casper.click('.js-header-tab-basic');
        casper.waitForSelector('[data-metric="ncloc"]');
      })

      .then(function () {
        test.assertDoesntExist('.js-filter-ncloc');
      })

      .run(function () {
        test.done();
      });
});
