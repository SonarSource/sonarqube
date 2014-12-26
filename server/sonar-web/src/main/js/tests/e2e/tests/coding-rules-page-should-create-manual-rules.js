/* global casper:false */

var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules-page-should-create-manual-rules');


casper.test.begin('coding-rules-page-should-delete-manual-rules', 3, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
        lib.mockRequestFromFile('/api/rules/create', 'show.json');
        lib.mockRequestFromFile('/api/rules/show', 'show.json');
      })

      .then(function () {
        casper.waitForSelector('.js-create-manual-rule', function () {
          casper.click('.js-create-manual-rule');
        });
      })

      .then(function () {
        casper.waitForSelector('.modal');
      })

      .then(function () {
        casper.evaluate(function () {
          jQuery('.modal [name="name"]').val('Manual Rule');
          jQuery('.modal [name="key"]').val('manual:Manual_Rule');
          jQuery('.modal [name="markdown_description"]').val('Manual Rule Description');
          jQuery('.modal #coding-rules-manual-rule-creation-create').click();
        });
        casper.waitForSelector('.coding-rules-detail-header');
      })

      .then(function () {
        test.assertSelectorContains('.coding-rules-detail-header', 'Manual Rule');
        test.assertSelectorContains('.coding-rule-details .subtitle', 'manual:Manual_Rule');
        test.assertSelectorContains('.coding-rules-detail-description', 'Manual Rule Description');
      })

      .run(function () {
        test.done();
      });
});
