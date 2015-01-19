/* global casper:false */

var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules-page-no-available-profiles-for-activation');


casper.test.begin('coding-rules-page-no-available-profiles-for-activation', 2, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
        lib.mockRequestFromFile('/api/rules/show', 'show.json');
        lib.mockRequest('/api/issues/search', '{}');
      })

      .then(function () {
        casper.waitForSelector('.coding-rule.selected', function () {
          casper.click('.coding-rule.selected .js-rule');
        });
      })

      .then(function () {
        casper.waitForSelector('.coding-rules-detail-header');
      })

      .then(function () {
        test.assertExist('#coding-rules-quality-profile-activate');
        casper.click('#coding-rules-quality-profile-activate');
        casper.waitForSelector('.modal');
      })

      .then(function () {
        test.assertExists('.modal .message-notice');
      })

      .run(function () {
        test.done();
      });
});
