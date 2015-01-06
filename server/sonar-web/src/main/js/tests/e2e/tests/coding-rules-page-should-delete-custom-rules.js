/* global casper:false */

var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules-page-should-delete-custom-rules');


casper.test.begin('coding-rules-page-should-delete-custom-rules', 2, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search-custom-rules.json',
            { data: { template_key: 'squid:ArchitecturalConstraint' } });
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
        lib.mockRequestFromFile('/api/rules/show', 'show.json');
        lib.mockRequest('/api/rules/delete', '{}');
        lib.mockRequest('/api/issues/search', '{}');
      })

      .then(function () {
        casper.waitForSelector('.coding-rule.selected', function () {
          casper.click('.coding-rule.selected .js-rule');
        });
      })

      .then(function () {
        casper.waitForSelector('#coding-rules-detail-custom-rules .coding-rules-detail-list-name');
      })

      .then(function () {
        test.assertElementCount('#coding-rules-detail-custom-rules .coding-rules-detail-list-name', 2);
        casper.click('.js-delete-custom-rule');
        casper.click('[data-confirm="yes"]');
        lib.waitForElementCount('#coding-rules-detail-custom-rules .coding-rules-detail-list-name', 1, function () {
          test.assert(true); // put dummy assert into wait statement
        });
      })

      .run(function () {
        test.done();
      });
});
