/* global casper:false */

var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules-page-should-activate-profile');


casper.test.begin('coding-rules-page-should-activate-profile', 5, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
        this.showMock = lib.mockRequestFromFile('/api/rules/show', 'show.json');
        lib.mockRequest('/api/qualityprofiles/activate_rule', '{}');
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
        test.assertDoesntExist('.coding-rules-detail-quality-profile-name');
        test.assertExist('#coding-rules-quality-profile-activate');
        casper.click('#coding-rules-quality-profile-activate');
        casper.waitForSelector('.modal');
      })

      .then(function () {
        lib.clearRequestMock(this.showMock);
        lib.mockRequestFromFile('/api/rules/show', 'show-with-profile.json');
        casper.click('#coding-rules-quality-profile-activation-activate');
        casper.waitForSelector('.coding-rules-detail-quality-profile-name');
      })

      .then(function () {
        test.assertExists('.coding-rules-detail-quality-profile-name');
        test.assertExists('.coding-rules-detail-quality-profile-severity');
        test.assertExists('.coding-rules-detail-quality-profile-deactivate');
      })

      .run(function () {
        test.done();
      });
});
