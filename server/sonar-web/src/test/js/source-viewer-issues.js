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
/* global casper:false */


var lib = require('../lib'),
    testName = lib.testName('Source Viewer', 'Issues');

lib.initMessages();
lib.changeWorkingDirectory('source-viewer-issues');
lib.configureCasper();


casper.test.begin(testName(), 16, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/components/app', 'app.json', { data: { uuid: 'uuid' } });
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json', { data: { uuid: 'uuid' } });
        lib.mockRequestFromFile('/api/issues/search', 'issues.json', { data: { componentUuids: 'uuid' } });
        lib.mockRequestFromFile('/api/issues/set_severity', 'set-severity.json',
            { data: { issue: '59fc17f7-c977-4cb6-8f04-fbe88e4b9186', severity: 'CRITICAL' } });
      })

      .then(function () {
        casper.evaluate(function () {
          var file = { uuid: 'uuid', key: 'key' };
          require(['apps/source-viewer/app'], function (App) {
            App.start({ el: '#content', file: file });
          });
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.source-line');
      })

      .then(function () {
        test.assertElementCount('.source-line-with-issues', 5);
        test.assertExists('.source-line-with-issues[data-line-number="0"]');
        test.assertExists('.source-line-with-issues[data-line-number="2"]');
        test.assertExists('.source-line-with-issues[data-line-number="12"]');
        test.assertExists('.source-line-with-issues[data-line-number="14"]');
        test.assertExists('.source-line-with-issues[data-line-number="15"]');
        test.assertNotVisible('.issue');
      })

      .then(function () {
        casper.click('.source-line-with-issues[data-line-number="2"]');
        test.assertVisible('#issue-e4de6481-7bfb-460a-8b3c-24459f9561d3');
        test.assertVisible('#issue-59fc17f7-c977-4cb6-8f04-fbe88e4b9186');
        test.assertElementCount('.issue-inner', 2);
      })

      .then(function () {
        test.assertExists('.source-line-with-issues[data-line-number="2"] .icon-severity-minor');

        casper.click('#issue-59fc17f7-c977-4cb6-8f04-fbe88e4b9186 .js-issue-set-severity');
        casper.waitForSelector('.bubble-popup');
      })

      .then(function () {
        casper.click('.bubble-popup .js-issue-severity[data-value="CRITICAL"]');
        casper.waitForSelector('#issue-59fc17f7-c977-4cb6-8f04-fbe88e4b9186 .icon-severity-critical');
      })

      .then(function () {
        test.assertExists('.source-line-with-issues[data-line-number="2"] .icon-severity-critical');
      })

      .then(function () {
        // hide issues
        casper.click('.source-line-with-issues[data-line-number="2"]');
        test.assertNotVisible('#issue-e4de6481-7bfb-460a-8b3c-24459f9561d3');
        test.assertNotVisible('#issue-59fc17f7-c977-4cb6-8f04-fbe88e4b9186');

        // show issues again
        casper.click('.source-line-with-issues[data-line-number="2"]');
        test.assertVisible('#issue-e4de6481-7bfb-460a-8b3c-24459f9561d3');
        test.assertVisible('#issue-59fc17f7-c977-4cb6-8f04-fbe88e4b9186');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
