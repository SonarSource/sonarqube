var lib = require('../lib'),
    testName = lib.testName('Component Viewer');

lib.initMessages();
lib.changeWorkingDirectory('component-viewer-spec');


casper.test.begin(testName('Mark as Favorite'), function (test) {
  casper
      .start(lib.buildUrl('component-viewer#component=component'), function () {
        lib.setDefaultViewport();
        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequest('/api/favourites', '{}', { type: 'POST' });
        lib.mockRequest('/api/favourites/*', '{}', { type: 'DELETE' });
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/show', 'source.json');
      })

      .then(function () {
        casper.waitForSelector('.component-viewer-source .row');
      })

      .then(function () {
        test.assertExists('.js-favorite');
        test.assertExists('.icon-not-favorite');
        casper.click('.js-favorite');
        casper.waitForSelector('.icon-favorite', function () {
          test.assertExists('.icon-favorite');
          casper.click('.js-favorite');
          casper.waitForSelector('.icon-not-favorite', function () {
            test.assertExists('.icon-not-favorite');
          });
        });
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Don\'t Show Favorite If Not Logged In'), function (test) {
  casper
      .start(lib.buildUrl('component-viewer#component=component'), function () {
        lib.setDefaultViewport();
        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app-not-logged-in.json');
        lib.mockRequestFromFile('/api/sources/show', 'source.json');
      })

      .then(function () {
        casper.waitForSelector('.component-viewer-source .row');
      })

      .then(function () {
        test.assertDoesntExist('.js-favorite');
        test.assertDoesntExist('.icon-favorite');
        test.assertDoesntExist('.icon-not-favorite');
      })

      .run(function () {
        test.done();
      });
});
