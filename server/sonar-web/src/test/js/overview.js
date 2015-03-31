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
    testName = lib.testName('Overview');

lib.initMessages();
lib.changeWorkingDirectory('overview');
lib.configureCasper();


casper.test.begin(testName(), 34, function (test) {
  casper
      .start(lib.buildUrl('overview'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/metrics', 'metrics.json');
        lib.mockRequestFromFile('/api/resources/index', 'measures.json');
        lib.mockRequestFromFile('/api/timemachine/index', 'timemachine.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/overview/app.js']);
        });
      })

      .then(function () {
        casper.waitForText('165,077');
      })

      .then(function () {
        test.assertSelectorContains('#overview-gate', '7');
        test.assertSelectorContains('#overview-gate', '64.7%');
        test.assertSelectorContains('#overview-gate', '2');
        test.assertSelectorContains('#overview-gate', '5');
        test.assertSelectorContains('#overview-gate', '0');
        test.assertElementCount('#overview-gate .overview-status', 9);
        test.assertElementCount('#overview-gate .overview-status-ERROR', 3);
        test.assertElementCount('#overview-gate .overview-status-WARN', 1);
        test.assertElementCount('#overview-gate .overview-status-OK', 5);

        test.assertSelectorContains('#overview-size', '165,077');
        test.assertSelectorContains('#overview-size', '+14');
        test.assertSelectorContains('#overview-size', '+62,886');
        test.assertSelectorContains('#overview-size', '+3,916');
        test.assertExists('#overview-size-trend path');

        test.assertSelectorContains('#overview-issues', '1,605');
        test.assertExists('#overview-issues-trend path');

        test.assertSelectorContains('#overview-debt', 'A');
        test.assertSelectorContains('#overview-debt', '66');
        test.assertSelectorContains('#overview-debt', '-2');
        test.assertSelectorContains('#overview-debt', '-49');
        test.assertSelectorContains('#overview-debt', '-64');
        test.assertExists('#overview-debt-trend path');

        test.assertSelectorContains('#overview-coverage', '83.9%');
        test.assertSelectorContains('#overview-coverage', '0%');
        test.assertSelectorContains('#overview-coverage', '+0.6%');
        test.assertSelectorContains('#overview-coverage', '88.2%');
        test.assertSelectorContains('#overview-coverage', '87.9%');
        test.assertSelectorContains('#overview-coverage', '90.0%');
        test.assertExists('#overview-coverage-trend path');

        test.assertSelectorContains('#overview-duplications', '1.0%');
        test.assertSelectorContains('#overview-duplications', '0%');
        test.assertSelectorContains('#overview-duplications', '-0.1%');
        test.assertSelectorContains('#overview-duplications', '+0.1%');
        test.assertExists('#overview-duplications-trend path');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
