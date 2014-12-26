/* global casper:false */

var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules-page-should-create-custom-rules');


casper.test.begin('coding-rules-page-should-delete-create-rules', 2, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        this.customRulesSearchMock = lib.mockRequestFromFile('/api/rules/search', 'search-custom-rules.json',
            { data: { template_key: 'squid:ArchitecturalConstraint' } });
        this.searchMock = lib.mockRequestFromFile('/api/rules/search', 'search.json');
        lib.mockRequestFromFile('/api/rules/show', 'show.json');
        lib.mockRequest('/api/rules/create', '{}');
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
        lib.clearRequestMock(this.customRulesSearchMock);
        lib.clearRequestMock(this.searchMock);
        lib.mockRequestFromFile('/api/rules/search', 'search-custom-rules2.json');
      })

      .then(function () {
        test.assertElementCount('#coding-rules-detail-custom-rules .coding-rules-detail-list-name', 1);
        casper.click('.js-create-custom-rule');
        casper.fillForm('.modal form', {
          name: 'test',
          markdown_description: 'test'
        });
        casper.click('#coding-rules-custom-rule-creation-create');
        lib.waitForElementCount('#coding-rules-detail-custom-rules .coding-rules-detail-list-name', 2, function () {
          test.assert(true); // put dummy assert into wait statement
        });
      })

      .run(function () {
        test.done();
      });
});
