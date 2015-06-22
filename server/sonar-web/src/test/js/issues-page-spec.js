/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
/* globals casper: false */

var lib = require('../lib'),
    testName = lib.testName('Issues');


lib.initMessages();
lib.changeWorkingDirectory('issues-spec');
lib.configureCasper();


casper.test.begin(testName('Base'), function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issue_filters/app', 'app.json');
        lib.mockRequestFromFile('/api/issues/search', 'search.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/issues/app-new'], function (App) {
            App.start({ el: '#content' });
          });
        });
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

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Context'), function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issue_filters/app', 'app.json');
        lib.mockRequestFromFile('/api/issues/search', 'search.json');
      })

      .then(function () {
        casper.evaluate(function () {
          var config = {
            resource: 'uuid',
            resourceQualifier: 'TRL',
            resourceName: 'SonarQube',
            periodDate: null
          };
          require(['apps/issues/app-context'], function (App) {
            App.start({ el: '#content', config: config });
          });
        });
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
          test.assertExists('.js-new-search');

          // Workspace header
          test.assertSelectorContains('#issues-total', '4623');
          test.assertExists('.js-prev');
          test.assertExists('.js-next');
          test.assertExists('.js-reload');
          test.assertExists('.js-bulk-change');
        });
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Issue Box', 'Check Elements'), function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issue_filters/app', 'app.json');
        lib.mockRequestFromFile('/api/issues/search', 'search.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/issues/app-new'], function (App) {
            App.start({ el: '#content' });
          });
        });
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

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Issue Box', 'Tags'), function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issue_filters/app', 'app.json');
        lib.mockRequestFromFile('/api/issues/search', 'search-with-tags.json');
        lib.mockRequestFromFile('/api/issues/tags', 'tags.json');
        lib.mockRequestFromFile('/api/issues/set_tags', 'tags-modified.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/issues/app-new'], function (App) {
            App.start({ el: '#content' });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('.issue.selected .js-issue-tags');
      })

      .then(function () {
        test.assertSelectorContains('.issue.selected .js-issue-tags', 'security, cwe');
        casper.click('.issue.selected .js-issue-edit-tags');
      })

      .then(function () {
        casper.waitForSelector('a[data-value=design]');
      })

      .then(function () {
        casper.click('a[data-value=design]');
        test.assertSelectorContains('.issue.selected .js-issue-tags', 'security, cwe, design');
      })

      .then(function () {
        casper.click('a[data-value=cwe]');
        test.assertSelectorContains('.issue.selected .js-issue-tags', 'security, design');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Issue Box', 'Transitions'), function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issue_filters/app', 'app.json');
        lib.mockRequestFromFile('/api/issues/search', 'search.json');
        lib.mockRequestFromFile('/api/issues/show*', 'show.json');
        lib.mockRequest('/api/issues/do_transition', '{}');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/issues/app-new'], function (App) {
            App.start({ el: '#content' });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('.issue.selected .js-issue-transition');
      })

      .then(function () {
        casper.click('.issue.selected .js-issue-transition');
        casper.waitForSelector('.menu > li > a');
      })

      .then(function () {
        test.assertExists('a[data-value=unconfirm]');
        test.assertExists('a[data-value=resolve]');
        test.assertExists('a[data-value=falsepositive]');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Issue Box', 'Rule'), function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issue_filters/app', 'app.json');
        lib.mockRequestFromFile('/api/issues/search', 'search.json');
        lib.mockRequestFromFile('/api/rules/show', 'rule.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/issues/app-new'], function (App) {
            App.start({ el: '#content' });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('.issue.selected');
      })

      .then(function () {
        casper.click('.issue.selected .js-issue-rule');
        casper.waitForSelector('.workspace-viewer-container .coding-rules-detail-properties');
      })

      .then(function () {
        test.assertSelectorContains('.workspace-viewer-name', 'Insufficient branch coverage by unit tests');
        test.assertSelectorContains('.workspace-viewer-container', 'Reliability > Unit tests coverage');
        test.assertSelectorContains('.workspace-viewer-container', 'An issue is created on a file as soon as the');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('File-Level Issues'), function (test) {
  var issueKey = '200d4a8b-9666-4e70-9953-7bab57933f97',
      issueSelector = '.issue[data-key="' + issueKey + '"]';

  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issue_filters/app', 'app.json');
        lib.mockRequestFromFile('/api/issues/search', 'file-level/search.json');
        lib.mockRequestFromFile('/api/components/app', 'file-level/components-app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'file-level/lines.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/issues/app-new'], function (App) {
            App.start({ el: '#content' });
          });
        });
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

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Severity Facet'), function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issue_filters/app', 'app.json');
        lib.mockRequestFromFile('/api/issues/search', 'search-reopened.json', { data: { severities: 'BLOCKER' } });
        lib.mockRequestFromFile('/api/issues/search', 'search.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/issues/app-new'], function (App) {
            App.start({ el: '#content' });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('.facet[data-value=BLOCKER]', function () {
          casper.click('.facet[data-value=BLOCKER]');
        });
      })

      .then(function () {
        casper.waitForSelectorTextChange('#issues-total', function () {
          test.assertElementCount('.issue', 4);
        });
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Select Issues'), 11, function (test) {
  var issueKey = '94357807-fcb4-40cc-9598-9a715f1eee6e',
      issueSelector = '.issue[data-key="' + issueKey + '"]';

  casper
      .start(lib.buildUrl('base#resolved=false'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/issue_filters/app', 'app.json');
        this.searchMock = lib.mockRequestFromFile('/api/issues/search', 'search.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/issues/app-new'], function (App) {
            App.start({ el: '#content' });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('.issue');
      })

      .then(function () {
        test.assertExists('.js-selection');
        test.assertDoesntExist('.js-selection.icon-checkbox-checked');
        test.assertVisible('.issue .js-toggle');
        test.assertElementCount('.js-toggle', 50);
      })

      .then(function () {
        test.assertDoesntExist(issueSelector + ' .js-toggle .icon-checkbox-checked');
        casper.click(issueSelector + ' .js-toggle');
        test.assertExists(issueSelector + ' .js-toggle .icon-checkbox-checked');
        test.assertExists('.js-selection.icon-checkbox-single.icon-checkbox-checked');
      })

      .then(function () {
        casper.click('.js-selection');
        test.assertDoesntExist('.js-selection.icon-checkbox-checked');
        test.assertDoesntExist('.js-toggle .icon-checkbox-checked');
      })

      .then(function () {
        casper.click('.js-selection');
        test.assertExists('.js-selection.icon-checkbox-checked');
        test.assertElementCount('.js-toggle .icon-checkbox-checked', 50);
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Bulk Change'), function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issue_filters/app', 'app.json');
        lib.mockRequestFromFile('/api/issues/search', 'search.json');
        lib.mockRequest('/issues/bulk_change_form*',
            '<div id="bulk-change-form">bulk change form</div>', { contentType: 'text/plain' });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/issues/app-new'], function (App) {
            App.start({ el: '#content' });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('.js-bulk-change');
      })

      .then(function () {
        casper.click('.js-bulk-change');
        casper.waitForSelector('#bulk-change-form', function () {
          test.assertSelectorContains('#bulk-change-form', 'bulk change form');
        });
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Bulk Change of Selected Issues'), 8, function (test) {
  var issueKey = '94357807-fcb4-40cc-9598-9a715f1eee6e',
      issueSelector = '.issue[data-key="' + issueKey + '"]';

  casper
      .start(lib.buildUrl('base#resolved=false'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/issue_filters/app', 'app.json');
        this.searchMock = lib.mockRequestFromFile('/api/issues/search', 'search.json');
        lib.mockRequest('/issues/bulk_change_form*',
            '<div id="bulk-change-form">bulk change form</div>', { contentType: 'text/plain' });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/issues/app-new'], function (App) {
            App.start({ el: '#content' });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('.issue');
      })

      .then(function () {
        test.assertExists('.js-selection');
        test.assertDoesntExist('.js-selection.icon-checkbox-checked');
        test.assertVisible('.issue .js-toggle');
      })

      .then(function () {
        test.assertDoesntExist(issueSelector + ' .js-toggle .icon-checkbox-checked');
        casper.click(issueSelector + ' .js-toggle');
        test.assertExists(issueSelector + ' .js-toggle .icon-checkbox-checked');
        test.assertExists('.js-selection.icon-checkbox-single.icon-checkbox-checked');
      })

      .then(function () {
        casper.click('.js-bulk-change-selected');
        casper.waitForSelector('#bulk-change-form');
      })

      .then(function () {
        test.assertSelectorContains('#bulk-change-form', 'bulk change form');
      })

      .then(function () {
        lib.clearRequestMock(this.searchMock);
        lib.mockRequestFromFile('/api/issues/search', 'search-changed.json');
        casper.evaluate(function () {
          window.onBulkIssues();
        });
        casper.waitForSelectorTextChange(issueSelector + ' .js-issue-set-severity');
      })

      .then(function () {
        test.assertExists(issueSelector + ' .js-toggle .icon-checkbox-checked');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Filter Similar Issues'), 12, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issue_filters/app', 'app.json');
        lib.mockRequestFromFile('/api/issues/search', 'search-filter-similar-issues-severities.json',
            { data: { severities: 'MAJOR' } });
        lib.mockRequestFromFile('/api/issues/search', 'search-filter-similar-issues.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/issues/app-new'], function (App) {
            App.start({ el: '#content' });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('.issue.selected');
      })

      .then(function () {
        casper.click('.issue.selected .js-issue-filter');
        casper.waitForSelector('.bubble-popup');
      })

      .then(function () {
        test.assertExists('.bubble-popup [data-property="severities"][data-value="MAJOR"]');
        test.assertExists('.bubble-popup [data-property="statuses"][data-value="CONFIRMED"]');
        test.assertExists('.bubble-popup [data-property="resolved"][data-value="false"]');
        test.assertExists('.bubble-popup [data-property="rules"][data-value="squid:S1214"]');
        test.assertExists('.bubble-popup [data-property="assigned"][data-value="false"]');
        test.assertExists('.bubble-popup [data-property="planned"][data-value="false"]');
        test.assertExists('.bubble-popup [data-property="tags"][data-value="bad-practice"]');
        test.assertExists('.bubble-popup [data-property="tags"][data-value="brain-overload"]');
        test.assertExists('.bubble-popup [data-property="projectUuids"][data-value="69e57151-be0d-4157-adff-c06741d88879"]');
        test.assertExists('.bubble-popup [data-property="moduleUuids"][data-value="7feef7c3-11b9-4175-b5a7-527ca3c75cb7"]');
        test.assertExists('.bubble-popup [data-property="fileUuids"][data-value="b0517331-0aaf-4091-b5cf-8e305dd0337a"]');

        casper.click('.bubble-popup [data-property="severities"]');
        casper.waitForSelectorTextChange('#issues-total', function () {
          test.assertSelectorContains('#issues-total', '17');
        });
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
