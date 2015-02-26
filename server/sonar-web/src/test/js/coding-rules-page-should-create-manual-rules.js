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
lib.changeWorkingDirectory('coding-rules-page-should-create-manual-rules');
lib.configureCasper();


casper.test.begin('coding-rules-page-should-delete-manual-rules', 3, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
        lib.mockRequestFromFile('/api/rules/create', 'show.json');
        lib.mockRequestFromFile('/api/rules/show', 'show.json');
        lib.mockRequest('/api/issues/search', '{}');
      })

      .then(function () {
        casper.waitForSelector('.js-create-manual-rule', function () {
          casper.click('.js-create-manual-rule');
        });
      })

      .then(function () {
        casper.waitForSelector('.modal');
      })

      .then(function () {
        casper.evaluate(function () {
          jQuery('.modal [name="name"]').val('Manual Rule');
          jQuery('.modal [name="key"]').val('manual:Manual_Rule');
          jQuery('.modal [name="markdown_description"]').val('Manual Rule Description');
          jQuery('.modal #coding-rules-manual-rule-creation-create').click();
        });
        casper.waitForSelector('.coding-rules-detail-header');
      })

      .then(function () {
        test.assertSelectorContains('.coding-rules-detail-header', 'Manual Rule');
        test.assertSelectorContains('.coding-rule-details .subtitle', 'manual:Manual_Rule');
        test.assertSelectorContains('.coding-rules-detail-description', 'Manual Rule Description');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
