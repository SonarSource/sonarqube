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
          test.assertElementCount('.issue', 50);
          test.assertElementCount('.issue.selected', 1);
          test.assertSelectorContains('.issue', '1 more branches need to be covered by unit tests to reach');

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


casper.test.begin(testName('Issue Box', 'Check Elements'), function (test) {
  casper
      .start(lib.buildUrl('issues'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/issue_filters/app', 'app.json');
        lib.mockRequestFromFile('/api/issues/search', 'search.json');
      })

      .then(function () {
        casper.waitForSelector('.issue.selected');
      })

      .then(function () {
        test.assertSelectorContains('.issue.selected', "Add a 'package-info.java' file to document the");
        test.assertExists('.issue.selected .js-issue-set-severity');
        test.assertSelectorContains('.issue.selected .js-issue-set-severity', 'MAJOR');
        test.assertSelectorContains('.issue.selected', 'CONFIRMED');
        test.assertElementCount('.issue.selected .js-issue-transition', 3);
        test.assertExists('.issue.selected [data-transition=unconfirm]');
        test.assertExists('.issue.selected [data-transition=resolve]');
        test.assertExists('.issue.selected [data-transition=falsepositive]');
        test.assertExists('.issue.selected .js-issue-assign');
        test.assertSelectorContains('.issue.selected .js-issue-assign', 'unassigned');
        test.assertExists('.issue.selected .js-issue-plan');
        test.assertSelectorContains('.issue.selected .js-issue-plan', 'unplanned');
        test.assertSelectorContains('.issue.selected', '20min');
        test.assertExists('.issue.selected .js-issue-comment');
        test.assertExists('.issue.selected .js-issue-more');
        test.assertExists('.issue.selected .js-issue-show-changelog');
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Issue Box', 'Transitions'), function (test) {
  casper
      .start(lib.buildUrl('issues'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/issue_filters/app', 'app.json');
        lib.mockRequestFromFile('/api/issues/search', 'search.json');
        this.showMock = lib.mockRequestFromFile('/api/issues/show*', 'show.json');
        lib.mockRequest('/api/issues/do_transition', '{}');
      })

      .then(function () {
        casper.waitForSelector('.issue.selected [data-transition=unconfirm]', function () {
          test.assertExists('.issue.selected [data-transition=unconfirm]');
          test.assertExists('.issue.selected [data-transition=resolve]');
          test.assertExists('.issue.selected [data-transition=falsepositive]');
          lib.clearRequestMock(this.showMock);
          this.showMock = lib.mockRequestFromFile('/api/issues/show*', 'show-open.json');
          casper.click('.issue.selected [data-transition=unconfirm]');
        });
      })

      .then(function () {
        casper.waitForSelector('.issue.selected [data-transition=confirm]', function () {
          test.assertExists('.issue.selected [data-transition=resolve]');
          test.assertExists('.issue.selected [data-transition=falsepositive]');
          lib.clearRequestMock(this.showMock);
          this.showMock = lib.mockRequestFromFile('/api/issues/show*', 'show-resolved.json');
          casper.click('.issue.selected [data-transition=resolve]');
        });
      })

      .then(function () {
        casper.waitForSelector('.issue.selected [data-transition=reopen]', function () {
          lib.clearRequestMock(this.showMock);
          this.showMock = lib.mockRequestFromFile('/api/issues/show*', 'show-open.json');
          casper.click('.issue.selected [data-transition=reopen]');
        });
      })

      .then(function () {
        casper.waitForSelector('.issue.selected [data-transition=confirm]', function () {
          test.assertExists('.issue.selected [data-transition=confirm]');
          test.assertExists('.issue.selected [data-transition=resolve]');
          test.assertExists('.issue.selected [data-transition=falsepositive]');
          lib.clearRequestMock(this.showMock);
          this.showMock = lib.mockRequestFromFile('/api/issues/show*', 'show-resolved.json');
          casper.click('.issue.selected [data-transition=falsepositive]');
        });
      })

      .then(function () {
        casper.waitForSelector('.issue.selected [data-transition=reopen]', function () {
          test.assertExists('.issue.selected [data-transition=reopen]');
        });
      })

      .run(function () {
        test.done();
      });
});
