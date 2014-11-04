var lib = require('../lib'),
    testName = lib.testName('Issues');


lib.initMessages();
lib.changeWorkingDirectory('issues-spec');


casper.test.begin(testName('Base'), function (test) {
  casper
      .start(lib.buildUrl('issues'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/issue_filters/app', 'app.json');
        lib.mockRequestFromFile('/api/issues/search', 'search.json');
      })

      .then(function () {
        casper.waitForSelector('.facet[data-value=BLOCKER]', function () {
          // Facets
          test.assertExists('.facet[data-value=BLOCKER]');
          test.assertExists('.facet[data-value=CRITICAL]');
          test.assertExists('.facet[data-value=MAJOR]');
          test.assertExists('.facet[data-value=MINOR]');
          test.assertExists('.facet[data-value=INFO]');

          test.assertExists('.facet[data-value=OPEN]');
          test.assertExists('.facet[data-value=REOPENED]');
          test.assertExists('.facet[data-value=CONFIRMED]');
          test.assertExists('.facet[data-value=RESOLVED]');
          test.assertExists('.facet[data-value=CLOSED]');

          test.assertExists('.facet[data-unresolved]');
          test.assertExists('.facet[data-value=REMOVED]');
          test.assertExists('.facet[data-value=FIXED]');
          test.assertExists('.facet[data-value=FALSE-POSITIVE]');

          // Issues
          test.assertElementCount('.issue-box', 50);
          test.assertElementCount('.issue-box.selected', 1);
          test.assertSelectorContains('.issue-box', '1 more branches need to be covered by unit tests to reach');

          // Filters
          test.assertExists('.js-issues-toggle-filters');
          test.assertExists('#issues-new-search');
          test.assertExists('#issues-filter-save-as');

          // Workspace header
          test.assertSelectorContains('#issues-total', '4623');
          test.assertExists('.js-issues-prev');
          test.assertExists('.js-issues-next');
          test.assertExists('#issues-reload');
          test.assertExists('#issues-bulk-change');
        });
      })

      .run(function () {
        test.done();
      });
});
