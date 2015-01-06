/* global casper:false */

var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules-page-rule-permalink');


casper.test.begin('coding-rules-page-rule-permalink', 1, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
        lib.mockRequestFromFile('/api/rules/show', 'show.json');
        lib.mockRequest('/api/issues/search', '{}');
      })

      .then(function () {
        casper.waitForSelector('.coding-rule.selected');
      })

      .then(function () {
        casper.click('.coding-rule.selected .js-rule');
        casper.waitForSelector('.coding-rules-detail-header');
      })

      .then(function () {
        test.assertExists('a[href="/coding_rules/show?key=squid%3AS1181"]');
      })

      .run(function () {
        test.done();
      });
});
