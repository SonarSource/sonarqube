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

var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules-page-should-show-rule-issues');
lib.configureCasper();


casper.test.begin('coding-rules-page-should-show-rule-issues', 5, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
        lib.mockRequestFromFile('/api/rules/show', 'show.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues-search.json');
      })

      .then(function () {
        casper.waitForSelector('.coding-rule.selected', function () {
          casper.click('.coding-rule.selected .js-rule');
        });
      })

      .then(function () {
        casper.waitForSelector('.coding-rules-most-violated-projects');
      })

      .then(function () {
        test.assertSelectorContains('.js-rule-issues', '7');
        test.assertSelectorContains('.coding-rules-most-violated-projects', 'SonarQube');
        test.assertSelectorContains('.coding-rules-most-violated-projects', '2');
        test.assertSelectorContains('.coding-rules-most-violated-projects', 'SonarQube Runner');
        test.assertSelectorContains('.coding-rules-most-violated-projects', '1');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
