/* global casper:false */

var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules-page-quality-profile-facet');


casper.test.begin('coding-rules-page-quality-profile-facet', 6, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search-qprofile-active.json',
            { data: { activation: true } });
        lib.mockRequestFromFile('/api/rules/search', 'search-qprofile-inactive.json',
            { data: { activation: 'false' } });
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
      })

      .then(function () {
        casper.waitForSelector('.coding-rule');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '609');
        casper.click('[data-property="qprofile"] .js-facet-toggle');
        casper.waitForSelector('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
      })

      .then(function () {
        casper.click('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '407');
        test.assertExists('.js-facet[data-value="java-default-with-mojo-conventions-49307"] .js-active.facet-toggle-active');
        casper.click('.js-facet[data-value="java-default-with-mojo-conventions-49307"] .js-inactive');
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '408');
        test.assertExists('.js-facet[data-value="java-default-with-mojo-conventions-49307"] .js-inactive.facet-toggle-active');
        casper.click('[data-property="qprofile"] .js-facet-toggle');
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '609');
      })

      .run(function () {
        test.done();
      });
});
