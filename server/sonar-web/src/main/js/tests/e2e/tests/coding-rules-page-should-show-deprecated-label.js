/* global casper:false */

var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules-page-should-show-deprecated-label');


casper.test.begin('coding-rules-page-should-show-deprecated-label', 1, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
      })

      .then(function () {
        casper.waitForSelector('.coding-rule.selected');
      })

      .then(function () {
        test.assertSelectorContains('.coding-rule.selected', 'DEPRECATED');
      })

      .run(function () {
        test.done();
      });
});
