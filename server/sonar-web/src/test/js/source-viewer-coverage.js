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
    testName = lib.testName('Source Viewer', 'Coverage');

lib.initMessages();
lib.changeWorkingDirectory('source-viewer-coverage');
lib.configureCasper();


casper.test.begin(testName(), 12, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/components/app', 'app.json', { data: { uuid: 'uuid' } });
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json', { data: { uuid: 'uuid' } });
        lib.mockRequestFromFile('/api/issues/search', 'issues.json', { data: { componentUuids: 'uuid' } });
        lib.mockRequestFromFile('/api/tests/list', 'test-cases.json',
            { data: { sourceFileUuid: 'uuid', sourceFileLineNumber: '11' } });
      })

      .then(function () {
        casper.evaluate(function () {
          var file = { uuid: 'uuid', key: 'key' };
          require(['apps/source-viewer/app'], function (App) {
            App.start({ el: '#content', file: file });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('.source-line');
      })

      .then(function () {
        test.assertExists('.source-line-covered[data-line-number="6"]');
        test.assertExists('.source-line-covered[data-line-number="8"]');
        test.assertExists('.source-line-covered[data-line-number="11"]');
        test.assertExists('.source-line-covered[data-line-number="12"]');

        test.assertExists('.source-line-partially-covered[data-line-number="5"]');
        test.assertExists('.source-line-partially-covered[data-line-number="7"]');

        test.assertExists('.source-line-uncovered[data-line-number="1"]');
        test.assertExists('.source-line-uncovered[data-line-number="2"]');
      })

      .then(function () {
        casper.click('.source-line-covered[data-line-number="11"]');
        casper.waitForSelector('.bubble-popup');
      })

      .then(function () {
        test.assertSelectorContains('.bubble-popup', 'SampleTest');
        test.assertSelectorContains('.bubble-popup', '2ms');
        test.assertExists('.bubble-popup .icon-test-status-ok');
      })

      .then(function () {
        casper.click('[data-uuid="uuid"]');
        casper.waitForSelector('.workspace-viewer .source-line');
      })

      .then(function () {
        test.assertElementCount('.workspace-viewer .source-line', 17);
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
