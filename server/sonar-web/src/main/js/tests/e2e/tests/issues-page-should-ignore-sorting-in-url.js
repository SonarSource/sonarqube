var lib = require('../lib'),
    testName = lib.testName('Issues');


lib.initMessages();
lib.changeWorkingDirectory('issues-page-should-ignore-sorting-in-url');


casper.test.begin('issues-page-should-ignore-sorting-in-url', function (test) {
  casper
      .start(lib.buildUrl('issues#asc=false'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/issue_filters/app', 'app.json');
        lib.mockRequestFromFile('/api/issues/search', 'search-reversed.json', { data: { asc: 'false' } });
        lib.mockRequestFromFile('/api/issues/search', 'search.json');
      })

      .then(function () {
        casper.waitForSelector('.facet[data-value=BLOCKER]');
      })

      .then(function () {
        test.assertSelectorContains('.issue.selected', 'L54');
        test.assertSelectorDoesntContain('.issue.selected', 'L59');
      })

      .run(function () {
        test.done();
      });
});
