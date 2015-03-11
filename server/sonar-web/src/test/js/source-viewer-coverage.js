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
    testName = lib.testName('Source Viewer');

lib.initMessages();
lib.changeWorkingDirectory('source-viewer-coverage');
lib.configureCasper();


casper.test.begin(testName('Coverage'), 4, function (test) {
  casper
      .start(lib.buildUrl('source-viewer'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
        lib.mockRequestFromFile('/api/tests/test_cases', 'test-cases.json');
      })

      .then(function () {
        casper.waitForSelector('.source-line');
      })

      .then(function () {
        test.assertElementCount('.source-line-covered', 1);
        casper.click('.source-line-covered');
        casper.waitForSelector('.bubble-popup');
      })

      .then(function () {
        test.assertSelectorContains('.bubble-popup', 'SampleTest');
        test.assertSelectorContains('.bubble-popup', '2ms');
        test.assertExists('.bubble-popup .icon-test-status-ok');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
