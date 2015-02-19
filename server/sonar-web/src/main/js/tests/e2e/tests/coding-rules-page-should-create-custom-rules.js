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
lib.changeWorkingDirectory('coding-rules-page-should-create-custom-rules');
lib.configureCasper();


casper.test.begin('coding-rules-page-should-delete-create-rules', 2, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        this.customRulesSearchMock = lib.mockRequestFromFile('/api/rules/search', 'search-custom-rules.json',
            { data: { template_key: 'squid:ArchitecturalConstraint' } });
        this.searchMock = lib.mockRequestFromFile('/api/rules/search', 'search.json');
        lib.mockRequestFromFile('/api/rules/show', 'show.json');
        lib.mockRequest('/api/rules/create', '{}');
        lib.mockRequest('/api/issues/search', '{}');
      })

      .then(function () {
        casper.waitForSelector('.coding-rule.selected', function () {
          casper.click('.coding-rule.selected .js-rule');
        });
      })

      .then(function () {
        casper.waitForSelector('#coding-rules-detail-custom-rules .coding-rules-detail-list-name');
      })

      .then(function () {
        lib.clearRequestMock(this.customRulesSearchMock);
        lib.clearRequestMock(this.searchMock);
        lib.mockRequestFromFile('/api/rules/search', 'search-custom-rules2.json');
      })

      .then(function () {
        test.assertElementCount('#coding-rules-detail-custom-rules .coding-rules-detail-list-name', 1);
        casper.click('.js-create-custom-rule');
        casper.fillForm('.modal form', {
          name: 'test',
          markdown_description: 'test'
        });
        casper.click('#coding-rules-custom-rule-creation-create');
        lib.waitForElementCount('#coding-rules-detail-custom-rules .coding-rules-detail-list-name', 2, function () {
          test.assert(true); // put dummy assert into wait statement
        });
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
