/* global casper:false */

var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules-page-inheritance-facet');


casper.test.begin('coding-rules-page-inheritance-facet', 11, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search-not-inherited.json', { data: { inheritance: 'NONE' } });
        lib.mockRequestFromFile('/api/rules/search', 'search-inherited.json', { data: { inheritance: 'INHERITED' } });
        lib.mockRequestFromFile('/api/rules/search', 'search-overriden.json', { data: { inheritance: 'OVERRIDES' } });
        lib.mockRequestFromFile('/api/rules/search', 'search-qprofile.json',
            { data: { qprofile: 'java-default-with-mojo-conventions-49307' } });
        lib.mockRequestFromFile('/api/rules/search', 'search-qprofile2.json',
            { data: { qprofile: 'java-top-profile-without-formatting-conventions-50037' } });
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
      })

      .then(function () {
        casper.waitForSelector('.coding-rule');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '609');
        test.assertExists('.search-navigator-facet-box-forbidden[data-property="inheritance"]');
        casper.click('[data-property="qprofile"] .js-facet-toggle');
        casper.waitForSelector('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
      })

      .then(function () {
        casper.click('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '407');
        test.assertDoesntExist('.search-navigator-facet-box-forbidden[data-property="inheritance"]');
        casper.click('[data-property="inheritance"] .js-facet-toggle');
        casper.waitForSelector('[data-property="inheritance"] [data-value="NONE"]');
      })

      .then(function () {
        casper.click('[data-property="inheritance"] [data-value="NONE"]');
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '103');
        casper.click('[data-property="inheritance"] [data-value="INHERITED"]');
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '101');
        casper.click('[data-property="inheritance"] [data-value="OVERRIDES"]');
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '102');
        casper.click('.js-facet[data-value="java-top-profile-without-formatting-conventions-50037"]');
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '408');
        test.assertExists('.search-navigator-facet-box-forbidden[data-property="inheritance"]');
        casper.click('[data-property="qprofile"] .js-facet-toggle');
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '609');
        test.assertExists('.search-navigator-facet-box-forbidden[data-property="inheritance"]');
      })

      .run(function () {
        test.done();
      });
});
