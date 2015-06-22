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
lib.changeWorkingDirectory('source-viewer-spec');
lib.configureCasper();


casper.test.begin(testName('Mark as Favorite'), function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/favourites', '{}', { type: 'POST' });
        lib.mockRequest('/api/favourites/*', '{}', { type: 'DELETE' });
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
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
        test.assertExists('.js-favorite');
        test.assertExists('.icon-not-favorite');
        casper.click('.js-favorite');
        casper.waitForSelector('.icon-favorite', function () {
          test.assertExists('.icon-favorite');
          casper.click('.js-favorite');
          casper.waitForSelector('.icon-not-favorite', function () {
            test.assertExists('.icon-not-favorite');
          });
        });
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Don\'t Show Favorite If Not Logged In'), function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/components/app', 'app-not-logged-in.json');
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
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
        test.assertDoesntExist('.js-favorite');
        test.assertDoesntExist('.icon-favorite');
        test.assertDoesntExist('.icon-not-favorite');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
