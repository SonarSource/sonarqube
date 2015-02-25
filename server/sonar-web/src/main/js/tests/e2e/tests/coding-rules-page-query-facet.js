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
lib.changeWorkingDirectory('coding-rules-page-query-facet');
lib.configureCasper();


casper.test.begin('coding-rules-page-query-facet', 3, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search-query.json', { data: { q: 'query' } });
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
      })

      .then(function () {
        casper.waitForSelector('.coding-rule');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '609');
        casper.evaluate(function () {
          jQuery('[data-property="q"] input').val('query');
          jQuery('[data-property="q"] form').submit();
        });
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '4');
        casper.evaluate(function () {
          jQuery('[data-property="q"] input').val('');
          jQuery('[data-property="q"] form').submit();
        });
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '609');
      })

      .run(function () {
        test.done();
      });
});
