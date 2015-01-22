/* globals casper: false */

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
          test.assertExists('.js-toggle-filters');
          test.assertExists('.js-new-search');
          test.assertExists('.js-filter-save-as');

          // Workspace header
          test.assertSelectorContains('#issues-total', '4623');
          test.assertExists('.js-prev');
          test.assertExists('.js-next');
          test.assertExists('.js-reload');
          test.assertExists('.js-bulk-change');
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
        test.assertExists('.issue.selected .js-issue-tags');
        test.assertSelectorContains('.issue.selected .js-issue-tags', 'issue.no_tag');
        test.assertExists('.issue.selected .js-issue-set-severity');
        test.assertSelectorContains('.issue.selected .js-issue-set-severity', 'MAJOR');
        test.assertSelectorContains('.issue.selected', 'CONFIRMED');
        test.assertElementCount('.issue.selected .js-issue-transition', 1);
        test.assertExists('.issue.selected .js-issue-transition');
        test.assertExists('.issue.selected .js-issue-assign');
        test.assertSelectorContains('.issue.selected .js-issue-assign', 'unassigned');
        test.assertExists('.issue.selected .js-issue-plan');
        test.assertSelectorContains('.issue.selected .js-issue-plan', 'unplanned');
        test.assertSelectorContains('.issue.selected', '20min');
        test.assertExists('.issue.selected .js-issue-comment');
        test.assertExists('.issue.selected .js-issue-show-changelog');
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Issue Box', 'Tags'), function (test) {
  casper
      .start(lib.buildUrl('issues'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/issue_filters/app', 'app.json');
        lib.mockRequestFromFile('/api/issues/search', 'search-with-tags.json');
        lib.mockRequestFromFile('/api/issues/tags', 'tags.json');
        lib.mockRequestFromFile('/api/issues/set_tags', 'tags-modified.json');
      })

      .then(function () {
        casper.waitForSelector('.issue.selected .js-issue-tags');
      })

      .then(function () {
        test.assertSelectorContains('.issue.selected .js-issue-tags', 'security, cwe');
        casper.click('.issue.selected .js-issue-edit-tags');
      })

      .then(function () {
        casper.waitForSelector('.issue-action-option[data-value=design]');
      })

      .then(function () {
        casper.click('.issue-action-option[data-value=design]');
        test.assertSelectorContains('.issue.selected .js-issue-tags', 'security, cwe, design');
      })

      .then(function () {
        casper.click('.issue-action-option[data-value=cwe]');
        test.assertSelectorContains('.issue.selected .js-issue-tags', 'security, design');
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
        lib.mockRequestFromFile('/api/issues/show*', 'show.json');
        lib.mockRequest('/api/issues/do_transition', '{}');
      })

      .then(function () {
        casper.waitForSelector('.issue.selected .js-issue-transition');
      })

      .then(function () {
        casper.click('.issue.selected .js-issue-transition');
        casper.waitForSelector('.issue-action-option');
      })

      .then(function () {
        test.assertExists('.issue-action-option[data-value=unconfirm]');
        test.assertExists('.issue-action-option[data-value=resolve]');
        test.assertExists('.issue-action-option[data-value=falsepositive]');
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('File-Level Issues'), function (test) {
  var issueKey = '200d4a8b-9666-4e70-9953-7bab57933f97',
      issueSelector = '.issue[data-key="' + issueKey + '"]';

  casper
      .start(lib.buildUrl('issues'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/issue_filters/app', 'app.json');
        lib.mockRequestFromFile('/api/issues/search', 'file-level/search.json');
        lib.mockRequestFromFile('/api/components/app', 'file-level/components-app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'file-level/lines.json');
      })

      .then(function () {
        casper.waitForSelector(issueSelector, function () {
          casper.click(issueSelector + ' .js-issue-navigate');
        });
      })

      .then(function () {
        casper.waitForSelector('.source-viewer ' + issueSelector, function () {
          test.assertSelectorContains('.source-viewer ' + issueSelector, '1 duplicated blocks of code');
        });
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Status Facet'), function (test) {
  casper
      .start(lib.buildUrl('issues'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/issue_filters/app', 'app.json');
        lib.mockRequestFromFile('/api/issues/search', 'search-reopened.json', { data: { statuses: 'REOPENED' } });
        lib.mockRequestFromFile('/api/issues/search', 'search.json');
      })

      .then(function () {
        casper.waitForSelector('.facet[data-value=REOPENED]', function () {
          casper.click('.facet[data-value=REOPENED]');
        });
      })

      .then(function () {
        lib.waitForElementCount('.issue', 4, function () {
          test.assertElementCount('.issue .icon-status-reopened', 4);
        });
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Bulk Change'), function (test) {
  casper
      .start(lib.buildUrl('issues'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/issue_filters/app', 'app.json');
        lib.mockRequestFromFile('/api/issues/search', 'search.json');
        lib.mockRequest('/issues/bulk_change_form?resolved=false',
            '<div id="bulk-change-form">bulk change form</div>', { contentType: 'text/plain' });
      })

      .then(function () {
        casper.waitForSelector('.issue', function () {
          casper.waitForSelector('#issues-bulk-change');
        });
      })

      .then(function () {
        casper.click('#issues-bulk-change');
        casper.waitForSelector('#bulk-change-form', function () {
          test.assertSelectorContains('#bulk-change-form', 'bulk change form');
        });
      })

      .run(function () {
        test.done();
      });
});
