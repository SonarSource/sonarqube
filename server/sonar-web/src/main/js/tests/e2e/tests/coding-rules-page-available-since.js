/* global casper:false */

var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules-page-available-since');


casper.test.begin('coding-rules-page-available-since', 2, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search-limited.json',
            { data: { available_since: '2014-12-01' } });
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
      })

      .then(function () {
        casper.waitForSelector('.coding-rule');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '609');
        casper.click('[data-property="available_since"] .js-facet-toggle');
        casper.evaluate(function () {
          jQuery('[data-property="available_since"] input').val('2014-12-01').change();
        });
      })

      .then(function () {
        lib.capture();
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '101');
      })

      .run(function () {
        test.done();
      });
});
