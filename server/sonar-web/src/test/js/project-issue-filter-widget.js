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
    testName = lib.testName('Project Issue Filter Widget');


lib.initMessages();
lib.changeWorkingDirectory('project-issues-filter-widget');
lib.configureCasper();


casper.test.begin(testName('Unresolved Issues By Severity'), 13, function (test) {
  casper
      .start(lib.buildUrl('issue-filter-widget'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/issues/search', 'unresolved-issues-by-severity.json',
            { data: { resolved: 'false' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/widgets/issue-filter.js'], function (IssueFilter) {
            window.requestMessages().done(function () {
              new IssueFilter({
                el: '#issue-filter-widget',
                query: 'resolved=false',
                distributionAxis: 'severities',
                componentUuid: '69e57151-be0d-4157-adff-c06741d88879',
                componentKey: 'org.codehaus.sonar:sonar'
              });
            });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#issue-filter-widget > table');
      })

      .then(function () {
        // check count
        test.assertElementCount('tr', 6);

        // check order and values
        test.assertSelectorContains('tr:nth-child(1)', '6851');
        test.assertSelectorContains('tr:nth-child(2)', '1');
        test.assertSelectorContains('tr:nth-child(3)', '105');
        test.assertSelectorContains('tr:nth-child(4)', '5027');
        test.assertSelectorContains('tr:nth-child(5)', '540');
        test.assertSelectorContains('tr:nth-child(6)', '1178');

        // check links
        test.assertExists('tr:nth-child(1) a[href="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false"]');
        test.assertExists('tr:nth-child(2) a[href="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false|severities=BLOCKER"]');
        test.assertExists('tr:nth-child(3) a[href="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false|severities=CRITICAL"]');
        test.assertExists('tr:nth-child(4) a[href="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false|severities=MAJOR"]');
        test.assertExists('tr:nth-child(5) a[href="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false|severities=MINOR"]');
        test.assertExists('tr:nth-child(6) a[href="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false|severities=INFO"]');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Unresolved Issues By Date'), 18, function (test) {
  casper
      .start(lib.buildUrl('issue-filter-widget'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/issues/search', 'unresolved-issues-by-date.json',
            { data: { resolved: 'false' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/widgets/issue-filter.js'], function (IssueFilter) {
            window.requestMessages().done(function () {
              new IssueFilter({
                el: '#issue-filter-widget',
                query: 'resolved=false',
                distributionAxis: 'createdAt',
                componentUuid: '69e57151-be0d-4157-adff-c06741d88879',
                componentKey: 'org.codehaus.sonar:sonar'
              });
            });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#issue-filter-widget > table');
      })

      .then(function () {
        // check count
        test.assertElementCount('tr', 6);

        // check order and values
        test.assertSelectorContains('tr:nth-child(1)', '6851');
        test.assertSelectorContains('tr:nth-child(2)', '1724');
        test.assertSelectorContains('tr:nth-child(3)', '3729');
        test.assertSelectorContains('tr:nth-child(4)', '1262');
        test.assertSelectorContains('tr:nth-child(5)', '64');
        test.assertSelectorContains('tr:nth-child(6)', '72');

        // check links
        test.assertExists('tr:nth-child(1) a[href="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false"]');
        // do not check createdBefore value, because it is set dynamically to *now*
        test.assertExists('tr:nth-child(2) a[href^="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false|createdAfter=2015-01-01|createdBefore="]');
        test.assertExists('tr:nth-child(3) a[href="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false|createdAfter=2014-01-01|createdBefore=2014-12-31"]');
        test.assertExists('tr:nth-child(4) a[href="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false|createdAfter=2013-01-01|createdBefore=2013-12-31"]');
        test.assertExists('tr:nth-child(5) a[href="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false|createdAfter=2012-01-01|createdBefore=2012-12-31"]');
        test.assertExists('tr:nth-child(6) a[href="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false|createdAfter=2011-01-01|createdBefore=2011-12-31"]');

        // check labels
        // do not check label fully, because it is set dynamically using *now*
        test.assertSelectorContains('tr:nth-child(2)', 'January 1 2015 – ');
        test.assertSelectorContains('tr:nth-child(3)', 'January 1 2014 – December 31 2014');
        test.assertSelectorContains('tr:nth-child(4)', 'January 1 2013 – December 31 2013');
        test.assertSelectorContains('tr:nth-child(5)', 'January 1 2012 – December 31 2012');
        test.assertSelectorContains('tr:nth-child(6)', 'January 1 2011 – December 31 2011');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Unresolved Issues By Severity With Differential Period'), 13, function (test) {
  casper
      .start(lib.buildUrl('issue-filter-widget'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/issues/search', 'unresolved-issues-by-severity-with-differential-period.json',
            { data: { resolved: 'false', createdAfter: '2014-12-09T17:12:38+0100' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/widgets/issue-filter.js'], function (IssueFilter) {
            window.requestMessages().done(function () {
              new IssueFilter({
                el: '#issue-filter-widget',
                query: 'resolved=false',
                distributionAxis: 'severities',
                periodDate: '2014-12-09T17:12:38+0100',
                componentUuid: '69e57151-be0d-4157-adff-c06741d88879',
                componentKey: 'org.codehaus.sonar:sonar'
              });
            });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#issue-filter-widget > table');
      })

      .then(function () {
        // check count
        test.assertElementCount('tr', 6);

        // check order and values
        test.assertSelectorContains('tr:nth-child(1)', '+549');
        test.assertSelectorContains('tr:nth-child(2)', '+0');
        test.assertSelectorContains('tr:nth-child(3)', '+59');
        test.assertSelectorContains('tr:nth-child(4)', '+306');
        test.assertSelectorContains('tr:nth-child(5)', '+135');
        test.assertSelectorContains('tr:nth-child(6)', '+49');

        // check links
        test.assertExists('tr:nth-child(1) a[href="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false|createdAfter=2014-12-09T17%3A12%3A38%2B0100"]');
        test.assertExists('tr:nth-child(2) a[href="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false|createdAfter=2014-12-09T17%3A12%3A38%2B0100|severities=BLOCKER"]');
        test.assertExists('tr:nth-child(3) a[href="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false|createdAfter=2014-12-09T17%3A12%3A38%2B0100|severities=CRITICAL"]');
        test.assertExists('tr:nth-child(4) a[href="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false|createdAfter=2014-12-09T17%3A12%3A38%2B0100|severities=MAJOR"]');
        test.assertExists('tr:nth-child(5) a[href="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false|createdAfter=2014-12-09T17%3A12%3A38%2B0100|severities=MINOR"]');
        test.assertExists('tr:nth-child(6) a[href="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false|createdAfter=2014-12-09T17%3A12%3A38%2B0100|severities=INFO"]');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Unresolved Issues By Severity With IGNORED Differential Period'), 19, function (test) {
  casper
      .start(lib.buildUrl('issue-filter-widget'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/issues/search',
            'unresolved-issues-by-severity-with-IGNORED-differential-period.json',
            { data: { resolved: 'false', createdInLast: '1w' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/widgets/issue-filter.js'], function (IssueFilter) {
            window.requestMessages().done(function () {
              new IssueFilter({
                el: '#issue-filter-widget',
                query: 'resolved=false|createdInLast=1w',
                distributionAxis: 'severities',
                periodDate: '2014-12-09T17:12:38+0100',
                componentUuid: '69e57151-be0d-4157-adff-c06741d88879',
                componentKey: 'org.codehaus.sonar:sonar'
              });
            });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#issue-filter-widget > table');
      })

      .then(function () {
        // check count
        test.assertElementCount('tr', 6);

        // check order and values
        test.assertSelectorContains('tr:nth-child(1)', '549');
        test.assertSelectorContains('tr:nth-child(2)', '0');
        test.assertSelectorContains('tr:nth-child(3)', '59');
        test.assertSelectorContains('tr:nth-child(4)', '306');
        test.assertSelectorContains('tr:nth-child(5)', '135');
        test.assertSelectorContains('tr:nth-child(6)', '49');

        // check that differential period is ignored
        test.assertSelectorDoesntContain('tr:nth-child(1)', '+');
        test.assertSelectorDoesntContain('tr:nth-child(2)', '+');
        test.assertSelectorDoesntContain('tr:nth-child(3)', '+');
        test.assertSelectorDoesntContain('tr:nth-child(4)', '+');
        test.assertSelectorDoesntContain('tr:nth-child(5)', '+');
        test.assertSelectorDoesntContain('tr:nth-child(6)', '+');

        // check links
        test.assertExists('tr:nth-child(1) a[href="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false|createdInLast=1w"]');
        test.assertExists('tr:nth-child(2) a[href="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false|createdInLast=1w|severities=BLOCKER"]');
        test.assertExists('tr:nth-child(3) a[href="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false|createdInLast=1w|severities=CRITICAL"]');
        test.assertExists('tr:nth-child(4) a[href="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false|createdInLast=1w|severities=MAJOR"]');
        test.assertExists('tr:nth-child(5) a[href="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false|createdInLast=1w|severities=MINOR"]');
        test.assertExists('tr:nth-child(6) a[href="/component_issues/index?id=org.codehaus.sonar%3Asonar#resolved=false|createdInLast=1w|severities=INFO"]');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
