/* global casper:false */

var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules-page-should-show-empty-list');


casper.test.begin('coding-rules-page-should-show-empty-list', 3, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
      })

      .then(function () {
        casper.waitForSelector('.search-navigator-facet-box');
      })

      .then(function () {
        test.assertDoesntExist('.coding-rule');
        test.assertSelectorContains('#coding-rules-total', 0);
        test.assertExists('.search-navigator-no-results');
      })

      .run(function () {
        test.done();
      });
});
