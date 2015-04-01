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
    testName = lib.testName('UI');


lib.initMessages();
lib.configureCasper();


casper.test.begin(testName('Global Messages'), 27, function (test) {
  casper
      .start(lib.buildUrl('ui-global-messages'), function () {
        lib.setDefaultViewport();
      })

      .then(function () {
        test.assertExists('#messages-panel');
        test.assertNotVisible('#messages-panel');
        test.assertExists('#error');
        test.assertNotVisible('#error');
        test.assertExists('#warning');
        test.assertNotVisible('#warning');
        test.assertExists('#info');
        test.assertNotVisible('#info');
      })

      .then(function () {
        casper.evaluate(function () {
          error('some error message');
          warning('some warning message');
          info('some info message');
        });
      })

      .then(function () {
        test.assertVisible('#messages-panel');
        test.assertVisible('#error');
        test.assertVisible('#warning');
        test.assertVisible('#info');
        test.assertSelectorContains('#error', 'some error message');
        test.assertSelectorContains('#warning', 'some warning message');
        test.assertSelectorContains('#info', 'some info message');
      })

      .then(function () {
        casper.click('#error a');
        test.assertVisible('#messages-panel');
        test.assertNotVisible('#error');
        test.assertVisible('#warning');
        test.assertVisible('#info');
      })

      .then(function () {
        casper.click('#info a');
        test.assertVisible('#messages-panel');
        test.assertNotVisible('#error');
        test.assertVisible('#warning');
        test.assertNotVisible('#info');
      })

      .then(function () {
        casper.click('#warning a');
        test.assertNotVisible('#messages-panel');
        test.assertNotVisible('#error');
        test.assertNotVisible('#warning');
        test.assertNotVisible('#info');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
