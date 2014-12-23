/* global casper:false */

var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules');


casper.test.begin('coding-rules-page-should-show-rules', 4, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
      })

      .then(function () {
        casper.waitForSelector('.coding-rule');
      })

      .then(function () {
        test.assertElementCount('.coding-rule', 25);
        test.assertSelectorContains('.coding-rule', 'Values passed to SQL commands should be sanitized');
        test.assertSelectorContains('.coding-rule', 'An open curly brace should be located at the beginning of a line');
        test.assertSelectorContains('#coding-rules-total', '609');
      })

      .run(function () {
        test.done();
      });
});
