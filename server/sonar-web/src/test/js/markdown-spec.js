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
    testName = lib.testName('Markdown');


lib.initMessages();
lib.configureCasper();


casper.test.begin(testName(), 8, function (test) {

  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/markdown/app'], function (App) {
            App.start({ el: '#content' });
          });
        });
      })

      .then(function () {
        casper.waitForText('Markdown Syntax');
      })

      .then(function () {
        ['strong', 'a', 'ul', 'ol', 'h3', 'code', 'pre', 'blockquote'].forEach(function (tag) {
          test.assertExists('' + tag, '<' + tag + '> found');
        });
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
