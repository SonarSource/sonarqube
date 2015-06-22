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

var lib = require('../lib');


lib.initMessages();
lib.changeWorkingDirectory('issues-page-should-open-issue-permalink');
lib.configureCasper();


var issueKey = 'some-issue-key';


casper.test.begin('issues-page-should-open-issue-permalink', 3, function (test) {
  casper
      .start(lib.buildUrl('base#issues=' + encodeURI(issueKey)), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/issue_filters/app', 'app.json');
        lib.mockRequest('/api/issues/search', '{}', { data: { issues: issueKey, p: 2 } });
        lib.mockRequestFromFile('/api/issues/search', 'search.json', { data: { issues: issueKey } });
        lib.mockRequestFromFile('/api/components/app', 'components-app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/issues/app-new'], function (App) {
            App.start({ el: '#content' });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('.source-line', function () {
          test.assertSelectorContains('.source-viewer', 'public void executeOn(Project project, SensorContext context');
          test.assertElementCount('.issue', 1);
          test.assertExist('.issue[data-key="' + issueKey + '"]');
        });
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
