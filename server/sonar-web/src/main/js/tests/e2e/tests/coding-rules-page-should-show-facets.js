/* global casper:false */

var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules');


casper.test.begin('coding-rules-page-should-show-facets', 1, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
      })

      .then(function () {
        casper.waitForSelector('.search-navigator-facet-box');
      })

      .then(function () {
        test.assertElementCount('.search-navigator-facet-box', 13);
      })

      .run(function () {
        test.done();
      });
});
