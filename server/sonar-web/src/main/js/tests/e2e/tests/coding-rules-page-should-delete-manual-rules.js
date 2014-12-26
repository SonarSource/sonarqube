/* global casper:false */

var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules-page-should-delete-manual-rules');


casper.test.begin('coding-rules-page-should-delete-manual-rules', 1, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        this.searchMock = lib.mockRequestFromFile('/api/rules/search', 'search-before.json');
        lib.mockRequestFromFile('/api/rules/show', 'show.json');
        lib.mockRequest('/api/rules/delete', '{}');
      })

      .then(function () {
        casper.waitForSelector('.coding-rule.selected', function () {
          casper.click('.coding-rule.selected .js-rule');
        });
      })

      .then(function () {
        casper.waitForSelector('.js-delete');
      })

      .then(function () {
        casper.click('.js-delete');
        casper.waitForSelector('[data-confirm="yes"]');
      })

      .then(function () {
        lib.clearRequestMock(this.searchMock);
        lib.mockRequestFromFile('/api/rules/search', 'search-after.json');
        casper.click('[data-confirm="yes"]');
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', 0);
      })

      .run(function () {
        test.done();
      });
});
