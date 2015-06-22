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
    testName = lib.testName('Source Viewer', 'Duplications');

lib.initMessages();
lib.changeWorkingDirectory('source-viewer-duplications');
lib.configureCasper();


casper.test.begin(testName(), 5, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
        lib.mockRequestFromFile('/api/duplications/show', 'duplications.json', { data: { uuid: 'uuid' } });
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
        test.assertElementCount('.source-line-duplications.source-line-duplicated', 5);
        casper.click('.source-line-duplicated');
        lib.waitForElementCount('.source-line-duplications-extra.source-line-duplicated', 5);
      })

      .then(function () {
        casper.waitForSelector('.bubble-popup');
      })

      .then(function () {
        test.assertSelectorContains('.bubble-popup', 'Duplicated');
        test.assertSelectorContains('.bubble-popup', '12');
        test.assertSelectorContains('.bubble-popup', '16');
      })

      .then(function () {
        casper.click('[data-uuid="bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbbb"]');
        casper.waitForSelector('.workspace-viewer .source-line');
      })

      .then(function () {
        test.assertElementCount('.workspace-viewer .source-line', 21);
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('In Removed Component'), 2, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/components/app', 'app.json', { data: { uuid: 'uuid' } });
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json', { data: { uuid: 'uuid' } });
        lib.mockRequestFromFile('/api/issues/search', 'issues.json', { data: { componentUuids: 'uuid' } });
        lib.mockRequestFromFile('/api/duplications/show', 'duplications-removed.json', { data: { uuid: 'uuid' } });
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
        test.assertElementCount('.source-line-duplications.source-line-duplicated', 5);
        casper.click('.source-line-duplicated');
        lib.waitForElementCount('.source-line-duplications-extra.source-line-duplicated', 5);
      })

      .then(function () {
        casper.waitForSelector('.bubble-popup');
      })

      .then(function () {
        test.assertExists('.bubble-popup .alert');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
