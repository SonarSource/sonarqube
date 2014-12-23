/* global casper:false */

var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules-page-should-show-details');


casper.test.begin('coding-rules-page-should-show-details', 20, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
        lib.mockRequestFromFile('/api/rules/show', 'show.json');
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
        test.assertSelectorContains('.search-navigator-workspace-details',
            'Throwable and Error classes should not be caught');

        test.assertSelectorContains('.search-navigator-workspace-details', 'squid:S1181');
        test.assertExists('.coding-rules-detail-properties .icon-severity-blocker');
        test.assertSelectorContains('.coding-rules-detail-properties', 'error-handling');
        test.assertSelectorContains('.coding-rules-detail-properties', '2013');
        test.assertSelectorContains('.coding-rules-detail-properties', 'SonarQube (Java)');
        test.assertSelectorContains('.coding-rules-detail-properties', 'Reliability > Exception handling');
        test.assertSelectorContains('.coding-rules-detail-properties', 'LINEAR');
        test.assertSelectorContains('.coding-rules-detail-properties', '20min');

        test.assertSelectorContains('.coding-rules-detail-description', 'is the superclass of all errors and');
        test.assertSelectorContains('.coding-rules-detail-description', 'its subclasses should be caught.');
        test.assertSelectorContains('.coding-rules-detail-description', 'Noncompliant Code Example');
        test.assertSelectorContains('.coding-rules-detail-description', 'Compliant Solution');

        test.assertSelectorContains('.coding-rules-detail-parameters', 'max');
        test.assertSelectorContains('.coding-rules-detail-parameters', 'Maximum authorized number of parameters');
        test.assertSelectorContains('.coding-rules-detail-parameters', '7');

        test.assertElementCount('.coding-rules-detail-quality-profile-name', 6);
        test.assertSelectorContains('.coding-rules-detail-quality-profile-name', 'Default - Top');
        test.assertElementCount('.coding-rules-detail-quality-profile-inheritance', 4);
        test.assertSelectorContains('.coding-rules-detail-quality-profile-inheritance', 'Default - Top');
      })

      .run(function () {
        test.done();
      });
});
