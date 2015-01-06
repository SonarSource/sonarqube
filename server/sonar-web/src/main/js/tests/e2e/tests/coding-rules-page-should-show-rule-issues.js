/* global casper:false */

var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules-page-should-show-rule-issues');


casper.test.begin('coding-rules-page-should-show-rule-issues', 5, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
        lib.mockRequestFromFile('/api/rules/show', 'show.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues-search.json');
      })

      .then(function () {
        casper.waitForSelector('.coding-rule.selected', function () {
          casper.click('.coding-rule.selected .js-rule');
        });
      })

      .then(function () {
        casper.waitForSelector('.coding-rules-most-violated-projects');
      })

      .then(function () {
        test.assertSelectorContains('.js-rule-issues', '7');
        test.assertSelectorContains('.coding-rules-most-violated-projects', 'SonarQube');
        test.assertSelectorContains('.coding-rules-most-violated-projects', '2');
        test.assertSelectorContains('.coding-rules-most-violated-projects', 'SonarQube Runner');
        test.assertSelectorContains('.coding-rules-most-violated-projects', '1');
      })

      .run(function () {
        test.done();
      });
});
