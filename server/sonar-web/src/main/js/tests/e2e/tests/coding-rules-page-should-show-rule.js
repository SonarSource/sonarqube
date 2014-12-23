/* global casper:false */

var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules');


casper.test.begin('coding-rules-page-should-show-rule', 7, function (test) {
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
        test.assertSelectorContains('.coding-rule.selected', 'Values passed to SQL commands should be sanitized');
        test.assertSelectorContains('.coding-rule.selected', 'Java');
        test.assertSelectorContains('.coding-rule.selected', 'cwe');
        test.assertSelectorContains('.coding-rule.selected', 'owasp-top10');
        test.assertSelectorContains('.coding-rule.selected', 'security');
        test.assertSelectorContains('.coding-rule.selected', 'sql');
        test.assertSelectorContains('.coding-rule.selected', 'custom-tag');
      })

      .run(function () {
        test.done();
      });
});
