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
lib.changeWorkingDirectory('source-viewer-scm');
lib.configureCasper();


casper.test.begin(testName('SCM'), 4, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/components/app', 'app.json', { data: { uuid: 'uuid' } });
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json', { data: { uuid: 'uuid' } });
        lib.mockRequestFromFile('/api/issues/search', 'issues.json', { data: { componentUuids: 'uuid' } });
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
        casper.click('.source-line-scm[data-line-number="1"]');
        casper.waitForSelector('.bubble-popup');
      })

      .then(function () {
        test.assertElementCount('.bubble-popup-section', 3);
        test.assertSelectorContains('.bubble-popup', 'sample-author');
        test.assertSelectorContains('.bubble-popup', 'samplerevision');
      })

      // do not hide popup on click
      // to allow selecting text by mouse
      .then(function () {
        casper.click('.bubble-popup');
        test.assertExists('.bubble-popup');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
