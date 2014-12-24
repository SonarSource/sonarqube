/* global casper:false */

var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules-page-active-severity-facet');


casper.test.begin('coding-rules-page-active-severity-facet', 7, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search-BLOCKER.json', { data: { active_severities: 'BLOCKER' } });
        lib.mockRequestFromFile('/api/rules/search', 'search-qprofile.json',
            { data: { qprofile: 'java-default-with-mojo-conventions-49307' } });
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
      })

      .then(function () {
        casper.waitForSelector('.coding-rule');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '609');
        test.assertExists('.search-navigator-facet-box-forbidden[data-property="active_severities"]');
        casper.click('[data-property="qprofile"] .js-facet-toggle');
        casper.waitForSelector('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
      })

      .then(function () {
        casper.click('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '407');
        test.assertDoesntExist('.search-navigator-facet-box-forbidden[data-property="active_severities"]');
        casper.click('[data-property="active_severities"] .js-facet-toggle');
        casper.waitForSelector('[data-property="active_severities"] [data-value="BLOCKER"]');
      })

      .then(function () {
        casper.click('[data-property="active_severities"] [data-value="BLOCKER"]');
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '4');
        casper.click('[data-property="qprofile"] .js-facet-toggle');
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '609');
        test.assertExists('.search-navigator-facet-box-forbidden[data-property="active_severities"]');
      })

      .run(function () {
        test.done();
      });
});
