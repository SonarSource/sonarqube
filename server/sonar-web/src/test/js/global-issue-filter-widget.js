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
    testName = lib.testName('Global Issue Filter Widget');


lib.initMessages();
lib.changeWorkingDirectory('global-issues-filter-widget');
lib.configureCasper();


casper.test.begin(testName('Unresolved Issues By Severity'), 13, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issues/search', 'unresolved-issues-by-severity.json',
            { data: { resolved: 'false' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['widgets/issue-filter/widget'], function (IssueFilter) {
            window.requestMessages().done(function () {
              new IssueFilter({
                el: '#content',
                query: 'resolved=false',
                distributionAxis: 'severities'
              });
            });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#content > table');
      })

      .then(function () {
        // check count
        test.assertElementCount('tr', 6);

        // check order and values
        test.assertSelectorContains('tr:nth-child(1)', '6.9k');
        test.assertSelectorContains('tr:nth-child(2)', '1');
        test.assertSelectorContains('tr:nth-child(3)', '105');
        test.assertSelectorContains('tr:nth-child(4)', '5k');
        test.assertSelectorContains('tr:nth-child(5)', '540');
        test.assertSelectorContains('tr:nth-child(6)', '1.2k');

        // check links
        test.assertExists('tr:nth-child(1) a[href="/issues/search#resolved=false"]');
        test.assertExists('tr:nth-child(2) a[href="/issues/search#resolved=false|severities=BLOCKER"]');
        test.assertExists('tr:nth-child(3) a[href="/issues/search#resolved=false|severities=CRITICAL"]');
        test.assertExists('tr:nth-child(4) a[href="/issues/search#resolved=false|severities=MAJOR"]');
        test.assertExists('tr:nth-child(5) a[href="/issues/search#resolved=false|severities=MINOR"]');
        test.assertExists('tr:nth-child(6) a[href="/issues/search#resolved=false|severities=INFO"]');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Red Issues By Severity'), 9, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issues/search', 'red-issues-by-severity.json',
            { data: { resolved: 'false', severities: 'BLOCKER,CRITICAL,MAJOR' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['widgets/issue-filter/widget'], function (IssueFilter) {
            window.requestMessages().done(function () {
              new IssueFilter({
                el: '#content',
                query: 'resolved=false|severities=BLOCKER,CRITICAL,MAJOR',
                distributionAxis: 'severities'
              });
            });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#content > table');
      })

      .then(function () {
        // check count
        test.assertElementCount('tr', 4);

        // check order and values
        test.assertSelectorContains('tr:nth-child(1)', '6.9k');
        test.assertSelectorContains('tr:nth-child(2)', '1');
        test.assertSelectorContains('tr:nth-child(3)', '105');
        test.assertSelectorContains('tr:nth-child(4)', '5k');

        // check links
        test.assertExists('tr:nth-child(1) a[href="/issues/search#resolved=false|severities=BLOCKER%2CCRITICAL%2CMAJOR"]');
        test.assertExists('tr:nth-child(2) a[href="/issues/search#resolved=false|severities=BLOCKER"]');
        test.assertExists('tr:nth-child(3) a[href="/issues/search#resolved=false|severities=CRITICAL"]');
        test.assertExists('tr:nth-child(4) a[href="/issues/search#resolved=false|severities=MAJOR"]');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('All Issues By Status'), 9, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issues/search', 'all-issues-by-status.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['widgets/issue-filter/widget'], function (IssueFilter) {
            window.requestMessages().done(function () {
              new IssueFilter({
                el: '#content',
                query: '',
                distributionAxis: 'statuses'
              });
            });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#content > table');
      })

      .then(function () {
        // check count
        test.assertElementCount('tr', 6);

        // check order and values
        test.assertSelectorContains('tr:nth-child(1)', '72k');
        test.assertSelectorContains('tr:nth-child(2)', '238');
        test.assertSelectorContains('tr:nth-child(3)', '4');
        test.assertSelectorContains('tr:nth-child(4)', '6.6k');
        test.assertSelectorContains('tr:nth-child(5)', '1.3k');
        test.assertSelectorContains('tr:nth-child(6)', '63k');

        // check links
        test.assertExists('tr:nth-child(1) a[href="/issues/search#"]');
        test.assertExists('tr:nth-child(2) a[href="/issues/search#statuses=OPEN"]');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Unresolved Issues By Status'), 9, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issues/search', 'unresolved-issues-by-status.json',
            { data: { resolved: 'false' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['widgets/issue-filter/widget'], function (IssueFilter) {
            window.requestMessages().done(function () {
              new IssueFilter({
                el: '#content',
                query: 'resolved=false',
                distributionAxis: 'statuses'
              });
            });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#content > table');
      })

      .then(function () {
        // check count
        test.assertElementCount('tr', 4);

        // check order and values
        test.assertSelectorContains('tr:nth-child(1)', '72k');
        test.assertSelectorContains('tr:nth-child(2)', '238');
        test.assertSelectorContains('tr:nth-child(3)', '4');
        test.assertSelectorContains('tr:nth-child(4)', '6.6k');

        // check links
        test.assertExists('tr:nth-child(1) a[href="/issues/search#resolved=false"]');
        test.assertExists('tr:nth-child(2) a[href="/issues/search#resolved=false|statuses=OPEN"]');
        test.assertExists('tr:nth-child(3) a[href="/issues/search#resolved=false|statuses=REOPENED"]');
        test.assertExists('tr:nth-child(4) a[href="/issues/search#resolved=false|statuses=CONFIRMED"]');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('All Issues By Resolution'), 10, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issues/search', 'all-issues-by-resolution.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['widgets/issue-filter/widget'], function (IssueFilter) {
            window.requestMessages().done(function () {
              new IssueFilter({
                el: '#content',
                query: '',
                distributionAxis: 'resolutions'
              });
            });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#content > table');
      })

      .then(function () {
        // check count
        test.assertElementCount('tr', 6);

        // check order and values
        test.assertSelectorContains('tr:nth-child(1)', '72k');
        test.assertSelectorContains('tr:nth-child(2)', '6.9k');
        test.assertSelectorContains('tr:nth-child(3)', '752');
        test.assertSelectorContains('tr:nth-child(4)', '550');
        test.assertSelectorContains('tr:nth-child(5)', '47k');
        test.assertSelectorContains('tr:nth-child(6)', '16k');

        // check links
        test.assertExists('tr:nth-child(1) a[href="/issues/search#"]');
        test.assertExists('tr:nth-child(2) a[href="/issues/search#resolved=false"]');
        test.assertExists('tr:nth-child(3) a[href="/issues/search#resolutions=FALSE-POSITIVE"]');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Unresolved Issues By Resolution'), 5, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issues/search', 'unresolved-issues-by-resolution.json',
            { data: { resolved: 'false' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['widgets/issue-filter/widget'], function (IssueFilter) {
            window.requestMessages().done(function () {
              new IssueFilter({
                el: '#content',
                query: 'resolved=false',
                distributionAxis: 'resolutions'
              });
            });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#content > table');
      })

      .then(function () {
        // check count
        test.assertElementCount('tr', 2);

        // check order and values
        test.assertSelectorContains('tr:nth-child(1)', '6.9k');
        test.assertSelectorContains('tr:nth-child(2)', '6.9k');

        // check links
        test.assertExists('tr:nth-child(1) a[href="/issues/search#resolved=false"]');
        test.assertExists('tr:nth-child(2) a[href="/issues/search#resolved=false"]');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Unresolved Issues By Rule'), 15, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issues/search', 'unresolved-issues-by-rule.json',
            { data: { resolved: 'false' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['widgets/issue-filter/widget'], function (IssueFilter) {
            window.requestMessages().done(function () {
              new IssueFilter({
                el: '#content',
                query: 'resolved=false',
                distributionAxis: 'rules'
              });
            });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#content > table');
      })

      .then(function () {
        // check count
        test.assertElementCount('tr', 16);

        // check order and values
        test.assertSelectorContains('tr:nth-child(1)', '6.9k');
        test.assertSelectorContains('tr:nth-child(2)', '879');
        test.assertSelectorContains('tr:nth-child(3)', '571');
        test.assertSelectorContains('tr:nth-child(15)', '113');
        test.assertSelectorContains('tr:nth-child(16)', '111');

        // check links
        test.assertExists('tr:nth-child(1) a[href="/issues/search#resolved=false"]');
        test.assertExists('tr:nth-child(2) a[href="/issues/search#resolved=false|rules=squid%3AS1161"]');
        test.assertExists('tr:nth-child(3) a[href="/issues/search#resolved=false|rules=squid%3AS1135"]');
        test.assertExists('tr:nth-child(15) a[href="/issues/search#resolved=false|rules=squid%3AS1134"]');
        test.assertExists('tr:nth-child(16) a[href="/issues/search#resolved=false|rules=squid%3AS1192"]');

        // check labels
        test.assertSelectorContains('tr:nth-child(2)', '@Override" annotation should be used');
        test.assertSelectorContains('tr:nth-child(3)', 'TODO tags should be handled');
        test.assertSelectorContains('tr:nth-child(15)', 'FIXME tags should be handled');
        test.assertSelectorContains('tr:nth-child(16)', 'String literals should not be duplicated');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Unresolved Issues By Project'), 15, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issues/search', 'unresolved-issues-by-project.json',
            { data: { resolved: 'false' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['widgets/issue-filter/widget'], function (IssueFilter) {
            window.requestMessages().done(function () {
              new IssueFilter({
                el: '#content',
                query: 'resolved=false',
                distributionAxis: 'projectUuids'
              });
            });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#content > table');
      })

      .then(function () {
        // check count
        test.assertElementCount('tr', 5);

        // check order and values
        test.assertSelectorContains('tr:nth-child(1)', '2.6k');
        test.assertSelectorContains('tr:nth-child(2)', '1.8k');
        test.assertSelectorContains('tr:nth-child(3)', '442');
        test.assertSelectorContains('tr:nth-child(4)', '283');
        test.assertSelectorContains('tr:nth-child(5)', '107');

        // check links
        test.assertExists('tr:nth-child(1) a[href="/issues/search#resolved=false"]');
        test.assertExists('tr:nth-child(2) a[href="/issues/search#resolved=false|projectUuids=69e57151-be0d-4157-adff-c06741d88879"]');
        test.assertExists('tr:nth-child(3) a[href="/issues/search#resolved=false|projectUuids=dd7c3556-ce3f-42d0-a348-914a582dc944"]');
        test.assertExists('tr:nth-child(4) a[href="/issues/search#resolved=false|projectUuids=5eab015a-1f76-4ba4-bd89-bf547132d673"]');
        test.assertExists('tr:nth-child(5) a[href="/issues/search#resolved=false|projectUuids=c156940b-e3ec-43f6-9589-e3b75aa9ca32"]');

        // check labels
        test.assertSelectorContains('tr:nth-child(2)', 'SonarQube');
        test.assertSelectorContains('tr:nth-child(3)', 'SonarQube Java');
        test.assertSelectorContains('tr:nth-child(4)', 'JavaScript');
        test.assertSelectorContains('tr:nth-child(5)', 'Python');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Unresolved Issues By Assignee'), 15, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issues/search', 'unresolved-issues-by-assignee.json',
            { data: { resolved: 'false' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['widgets/issue-filter/widget'], function (IssueFilter) {
            window.requestMessages().done(function () {
              new IssueFilter({
                el: '#content',
                query: 'resolved=false',
                distributionAxis: 'assignees'
              });
            });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#content > table');
      })

      .then(function () {
        // check count
        test.assertElementCount('tr', 5);

        // check order and values
        test.assertSelectorContains('tr:nth-child(1)', '6.9k');
        test.assertSelectorContains('tr:nth-child(2)', '4.1k');
        test.assertSelectorContains('tr:nth-child(3)', '698');
        test.assertSelectorContains('tr:nth-child(4)', '504');
        test.assertSelectorContains('tr:nth-child(5)', '426');

        // check links
        test.assertExists('tr:nth-child(1) a[href="/issues/search#resolved=false"]');
        test.assertExists('tr:nth-child(2) a[href="/issues/search#resolved=false|assigned=false"]');
        test.assertExists('tr:nth-child(3) a[href="/issues/search#resolved=false|assignees=first.user"]');
        test.assertExists('tr:nth-child(4) a[href="/issues/search#resolved=false|assignees=second.user"]');
        test.assertExists('tr:nth-child(5) a[href="/issues/search#resolved=false|assignees=third.user"]');

        // check labels
        test.assertSelectorContains('tr:nth-child(2)', 'unassigned');
        test.assertSelectorContains('tr:nth-child(3)', 'First User');
        test.assertSelectorContains('tr:nth-child(4)', 'Second User');
        test.assertSelectorContains('tr:nth-child(5)', 'Third User');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Unresolved Unassigned Issues By Assignee'), 6, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issues/search', 'unresolved-unassigned-issues-by-assignee.json',
            { data: { resolved: 'false', assigned: 'false' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['widgets/issue-filter/widget'], function (IssueFilter) {
            window.requestMessages().done(function () {
              new IssueFilter({
                el: '#content',
                query: 'resolved=false|assigned=false',
                distributionAxis: 'assignees'
              });
            });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#content > table');
      })

      .then(function () {
        // check count
        test.assertElementCount('tr', 2);

        // check order and values
        test.assertSelectorContains('tr:nth-child(1)', '4.1k');
        test.assertSelectorContains('tr:nth-child(2)', '4.1k');

        // check links
        test.assertExists('tr:nth-child(1) a[href="/issues/search#resolved=false|assigned=false"]');
        test.assertExists('tr:nth-child(2) a[href="/issues/search#resolved=false|assigned=false"]');

        // check labels
        test.assertSelectorContains('tr:nth-child(2)', 'unassigned');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Unresolved Issues By Reporter'), 12, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issues/search', 'unresolved-issues-by-reporter.json',
            { data: { resolved: 'false' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['widgets/issue-filter/widget'], function (IssueFilter) {
            window.requestMessages().done(function () {
              new IssueFilter({
                el: '#content',
                query: 'resolved=false',
                distributionAxis: 'reporters'
              });
            });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#content > table');
      })

      .then(function () {
        // check count
        test.assertElementCount('tr', 4);

        // check order and values
        test.assertSelectorContains('tr:nth-child(1)', '6.9k');
        test.assertSelectorContains('tr:nth-child(2)', '698');
        test.assertSelectorContains('tr:nth-child(3)', '504');
        test.assertSelectorContains('tr:nth-child(4)', '426');

        // check links
        test.assertExists('tr:nth-child(1) a[href="/issues/search#resolved=false"]');
        test.assertExists('tr:nth-child(2) a[href="/issues/search#resolved=false|reporters=first.user"]');
        test.assertExists('tr:nth-child(3) a[href="/issues/search#resolved=false|reporters=second.user"]');
        test.assertExists('tr:nth-child(4) a[href="/issues/search#resolved=false|reporters=third.user"]');

        // check labels
        test.assertSelectorContains('tr:nth-child(2)', 'First User');
        test.assertSelectorContains('tr:nth-child(3)', 'Second User');
        test.assertSelectorContains('tr:nth-child(4)', 'Third User');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Unresolved Issues By Language'), 15, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issues/search', 'unresolved-issues-by-language.json',
            { data: { resolved: 'false' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['widgets/issue-filter/widget'], function (IssueFilter) {
            window.requestMessages().done(function () {
              new IssueFilter({
                el: '#content',
                query: 'resolved=false',
                distributionAxis: 'languages'
              });
            });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#content > table');
      })

      .then(function () {
        // check count
        test.assertElementCount('tr', 5);

        // check order and values
        test.assertSelectorContains('tr:nth-child(1)', '6.9k');
        test.assertSelectorContains('tr:nth-child(2)', '6.3k');
        test.assertSelectorContains('tr:nth-child(3)', '444');
        test.assertSelectorContains('tr:nth-child(4)', '22');
        test.assertSelectorContains('tr:nth-child(5)', '15');

        // check links
        test.assertExists('tr:nth-child(1) a[href="/issues/search#resolved=false"]');
        test.assertExists('tr:nth-child(2) a[href="/issues/search#resolved=false|languages=java"]');
        test.assertExists('tr:nth-child(3) a[href="/issues/search#resolved=false|languages=py"]');
        test.assertExists('tr:nth-child(4) a[href="/issues/search#resolved=false|languages=php"]');
        test.assertExists('tr:nth-child(5) a[href="/issues/search#resolved=false|languages=js"]');

        // check labels
        test.assertSelectorContains('tr:nth-child(2)', 'Java');
        test.assertSelectorContains('tr:nth-child(3)', 'Python');
        test.assertSelectorContains('tr:nth-child(4)', 'PHP');
        test.assertSelectorContains('tr:nth-child(5)', 'JavaScript');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Unresolved Issues By Action Plan'), 15, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issues/search', 'unresolved-issues-by-action-plan.json',
            { data: { resolved: 'false' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['widgets/issue-filter/widget'], function (IssueFilter) {
            window.requestMessages().done(function () {
              new IssueFilter({
                el: '#content',
                query: 'resolved=false',
                distributionAxis: 'actionPlans'
              });
            });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#content > table');
      })

      .then(function () {
        // check count
        test.assertElementCount('tr', 5);

        // check order and values
        test.assertSelectorContains('tr:nth-child(1)', '6.9k');
        test.assertSelectorContains('tr:nth-child(2)', '5.9k');
        test.assertSelectorContains('tr:nth-child(3)', '532');
        test.assertSelectorContains('tr:nth-child(4)', '56');
        test.assertSelectorContains('tr:nth-child(5)', '52');

        // check links
        test.assertExists('tr:nth-child(1) a[href="/issues/search#resolved=false"]');
        test.assertExists('tr:nth-child(2) a[href="/issues/search#resolved=false|planned=false"]');
        test.assertExists('tr:nth-child(3) a[href="/issues/search#resolved=false|actionPlans=0cf48508-2fcd-4cb8-a50b-c5cd7c3decc0"]');
        test.assertExists('tr:nth-child(4) a[href="/issues/search#resolved=false|actionPlans=1b9e7e52-ff58-40c1-80bf-f68429a3275e"]');
        test.assertExists('tr:nth-child(5) a[href="/issues/search#resolved=false|actionPlans=8c1d5d01-948e-4670-a0d9-17c512979486"]');

        // check labels
        test.assertSelectorContains('tr:nth-child(2)', 'unplanned');
        test.assertSelectorContains('tr:nth-child(3)', 'First Action Plan');
        test.assertSelectorContains('tr:nth-child(4)', 'Second Action Plan');
        test.assertSelectorContains('tr:nth-child(5)', 'Third Action Plan');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Unresolved Unplanned Issues By Action Plan'), 6, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issues/search', 'unresolved-unplanned-issues-by-action-plan.json',
            { data: { resolved: 'false', planned: 'false' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['widgets/issue-filter/widget'], function (IssueFilter) {
            window.requestMessages().done(function () {
              new IssueFilter({
                el: '#content',
                query: 'resolved=false|planned=false',
                distributionAxis: 'actionPlans'
              });
            });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#content > table');
      })

      .then(function () {
        // check count
        test.assertElementCount('tr', 2);

        // check order and values
        test.assertSelectorContains('tr:nth-child(1)', '5.9k');
        test.assertSelectorContains('tr:nth-child(2)', '5.9k');

        // check links
        test.assertExists('tr:nth-child(1) a[href="/issues/search#resolved=false|planned=false"]');
        test.assertExists('tr:nth-child(2) a[href="/issues/search#resolved=false|planned=false"]');

        // check labels
        test.assertSelectorContains('tr:nth-child(2)', 'unplanned');
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
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issues/search', 'unresolved-issues-by-date.json',
            { data: { resolved: 'false' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['widgets/issue-filter/widget'], function (IssueFilter) {
            window.requestMessages().done(function () {
              new IssueFilter({
                el: '#content',
                query: 'resolved=false',
                distributionAxis: 'createdAt'
              });
            });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#content > table');
      })

      .then(function () {
        // check count
        test.assertElementCount('tr', 6);

        // check order and values
        test.assertSelectorContains('tr:nth-child(1)', '6.9k');
        test.assertSelectorContains('tr:nth-child(2)', '1.7k');
        test.assertSelectorContains('tr:nth-child(3)', '3.7k');
        test.assertSelectorContains('tr:nth-child(4)', '1.3k');
        test.assertSelectorContains('tr:nth-child(5)', '64');
        test.assertSelectorContains('tr:nth-child(6)', '72');

        // check links
        test.assertExists('tr:nth-child(1) a[href="/issues/search#resolved=false"]');
        // do not check createdBefore value, because it is set dynamically to *now*
        test.assertExists('tr:nth-child(2) a[href^="/issues/search#resolved=false|createdAfter=2015-01-01|createdBefore="]');
        test.assertExists('tr:nth-child(3) a[href="/issues/search#resolved=false|createdAfter=2014-01-01|createdBefore=2014-12-31"]');
        test.assertExists('tr:nth-child(4) a[href="/issues/search#resolved=false|createdAfter=2013-01-01|createdBefore=2013-12-31"]');
        test.assertExists('tr:nth-child(5) a[href="/issues/search#resolved=false|createdAfter=2012-01-01|createdBefore=2012-12-31"]');
        test.assertExists('tr:nth-child(6) a[href="/issues/search#resolved=false|createdAfter=2011-01-01|createdBefore=2011-12-31"]');

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


casper.test.begin(testName('Unresolved Issues on a Limited Period By Date'), 12, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issues/search', 'unresolved-issues-by-date-limited.json',
            { data: { resolved: 'false', createdAfter: '2015-02-16', createdBefore: '2015-02-18' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['widgets/issue-filter/widget'], function (IssueFilter) {
            window.requestMessages().done(function () {
              new IssueFilter({
                el: '#content',
                query: 'resolved=false|createdAfter=2015-02-16|createdBefore=2015-02-18',
                distributionAxis: 'createdAt'
              });
            });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#content > table');
      })

      .then(function () {
        // check count
        test.assertElementCount('tr', 4);

        // check order and values
        test.assertSelectorContains('tr:nth-child(1)', '6.9k');
        test.assertSelectorContains('tr:nth-child(2)', '47');
        test.assertSelectorContains('tr:nth-child(3)', '48');
        test.assertSelectorContains('tr:nth-child(4)', '49');

        // check links
        test.assertExists('tr:nth-child(1) a[href="/issues/search#resolved=false|createdAfter=2015-02-16|createdBefore=2015-02-18"]');
        test.assertExists('tr:nth-child(2) a[href="/issues/search#resolved=false|createdAfter=2015-02-18|createdBefore=2015-02-19"]');
        test.assertExists('tr:nth-child(3) a[href="/issues/search#resolved=false|createdAfter=2015-02-17|createdBefore=2015-02-18"]');
        test.assertExists('tr:nth-child(4) a[href="/issues/search#resolved=false|createdAfter=2015-02-16|createdBefore=2015-02-17"]');

        // check labels
        test.assertSelectorContains('tr:nth-child(2)', 'February 18 2015');
        test.assertSelectorContains('tr:nth-child(3)', 'February 17 2015');
        test.assertSelectorContains('tr:nth-child(4)', 'February 16 2015');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Unresolved Issues By Severity Displaying Debt'), 13, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/l10n/index', 'messages.json');
        lib.mockRequestFromFile('/api/issues/search', 'unresolved-issues-by-severity-debt.json',
            { data: { resolved: 'false' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['widgets/issue-filter/widget'], function (IssueFilter) {
            window.requestMessages().done(function () {
              new IssueFilter({
                el: '#content',
                query: 'resolved=false',
                distributionAxis: 'severities',
                displayMode: 'debt'
              });
            });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#content > table');
      })

      .then(function () {
        // check count
        test.assertElementCount('tr', 6);

        // check order and values
        test.assertSelectorContains('tr:nth-child(1)', '~ 14d');
        test.assertSelectorContains('tr:nth-child(2)', '1min');
        test.assertSelectorContains('tr:nth-child(3)', '~ 1h');
        test.assertSelectorContains('tr:nth-child(4)', '~ 10d');
        test.assertSelectorContains('tr:nth-child(5)', '~ 1d');
        test.assertSelectorContains('tr:nth-child(6)', '~ 2d');

        // check links
        test.assertExists('tr:nth-child(1) a[href="/issues/search#resolved=false|facetMode=debt"]');
        test.assertExists('tr:nth-child(2) a[href="/issues/search#resolved=false|severities=BLOCKER|facetMode=debt"]');
        test.assertExists('tr:nth-child(3) a[href="/issues/search#resolved=false|severities=CRITICAL|facetMode=debt"]');
        test.assertExists('tr:nth-child(4) a[href="/issues/search#resolved=false|severities=MAJOR|facetMode=debt"]');
        test.assertExists('tr:nth-child(5) a[href="/issues/search#resolved=false|severities=MINOR|facetMode=debt"]');
        test.assertExists('tr:nth-child(6) a[href="/issues/search#resolved=false|severities=INFO|facetMode=debt"]');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Unresolved Issues By Date Displaying Debt'), 13, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/l10n/index', 'messages.json');
        lib.mockRequestFromFile('/api/issues/search', 'unresolved-issues-by-date-debt.json',
            { data: { resolved: 'false' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['widgets/issue-filter/widget'], function (IssueFilter) {
            window.requestMessages().done(function () {
              new IssueFilter({
                el: '#content',
                query: 'resolved=false',
                distributionAxis: 'createdAt',
                displayMode: 'debt'
              });
            });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#content > table');
      })

      .then(function () {
        // check count
        test.assertElementCount('tr', 6);

        // check order and values
        lib.capture();
        test.assertSelectorContains('tr:nth-child(1)', '~ 14d');
        test.assertSelectorContains('tr:nth-child(2)', '1min');
        test.assertSelectorContains('tr:nth-child(3)', '~ 1h');
        test.assertSelectorContains('tr:nth-child(4)', '~ 1d');
        test.assertSelectorContains('tr:nth-child(5)', '~ 2d');
        test.assertSelectorContains('tr:nth-child(6)', '~ 10d');

        // check links
        test.assertExists('tr:nth-child(1) a[href="/issues/search#resolved=false|facetMode=debt"]');
        // do not check createdBefore value, because it is set dynamically to *now*
        test.assertExists('tr:nth-child(2) a[href^="/issues/search#resolved=false|createdAfter=2015-01-01|createdBefore="]');
        test.assertExists('tr:nth-child(3) a[href="/issues/search#resolved=false|createdAfter=2014-01-01|createdBefore=2014-12-31|facetMode=debt"]');
        test.assertExists('tr:nth-child(4) a[href="/issues/search#resolved=false|createdAfter=2013-01-01|createdBefore=2013-12-31|facetMode=debt"]');
        test.assertExists('tr:nth-child(5) a[href="/issues/search#resolved=false|createdAfter=2012-01-01|createdBefore=2012-12-31|facetMode=debt"]');
        test.assertExists('tr:nth-child(6) a[href="/issues/search#resolved=false|createdAfter=2011-01-01|createdBefore=2011-12-31|facetMode=debt"]');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
