/* global casper:false */

var lib = require('../lib'),
    testName = lib.testName('Source Viewer');

lib.initMessages();
lib.changeWorkingDirectory('source-viewer-spec');


casper.test.begin(testName('Mark as Favorite'), function (test) {
  casper
      .start(lib.buildUrl('source-viewer'), function () {
        lib.setDefaultViewport();
        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequest('/api/favourites', '{}', { type: 'POST' });
        lib.mockRequest('/api/favourites/*', '{}', { type: 'DELETE' });
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
      })

      .then(function () {
        casper.waitForSelector('.source-line');
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
      .start(lib.buildUrl('source-viewer'), function () {
        lib.setDefaultViewport();
        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app-not-logged-in.json');
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
      })

      .then(function () {
        casper.waitForSelector('.source-line');
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
