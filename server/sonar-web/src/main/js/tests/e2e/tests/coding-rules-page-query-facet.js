/* global casper:false */

var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules-page-query-facet');


casper.test.begin('coding-rules-page-query-facet', 3, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search-query.json', { data: { q: 'query' } });
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
      })

      .then(function () {
        casper.waitForSelector('.coding-rule');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '609');
        casper.evaluate(function () {
          jQuery('[data-property="q"] input').val('query').change();
        });
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '4');
        casper.evaluate(function () {
          jQuery('[data-property="q"] input').val('').change();
        });
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '609');
      })

      .run(function () {
        test.done();
      });
});
