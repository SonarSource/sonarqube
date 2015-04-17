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
lib.changeWorkingDirectory('coding-rules-page-active-severity-facet');
lib.configureCasper();


casper.test.begin('coding-rules-page-active-severity-facet', 7, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search-BLOCKER.json', { data: { active_severities: 'BLOCKER' } });
        lib.mockRequestFromFile('/api/rules/search', 'facet.json', { data: { facets: 'active_severities', ps: '1' } });
        lib.mockRequestFromFile('/api/rules/search', 'search-qprofile.json',
            { data: { qprofile: 'java-default-with-mojo-conventions-49307' } });
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/coding-rules/app.js']);
        });
      })

      .then(function () {
        casper.waitForSelector('.coding-rule');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '609');
        test.assertExists('.search-navigator-facet-box-forbidden[data-property="active_severities"]');
        casper.click('[data-property="qprofile"] .js-facet-toggle');
        casper.waitForSelector('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
      })

      .then(function () {
        casper.click('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '407');
        test.assertDoesntExist('.search-navigator-facet-box-forbidden[data-property="active_severities"]');
        casper.click('[data-property="active_severities"] .js-facet-toggle');
        casper.waitForSelector('[data-property="active_severities"] [data-value="BLOCKER"]');
      })

      .then(function () {
        casper.click('[data-property="active_severities"] [data-value="BLOCKER"]');
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '4');
        casper.click('[data-property="qprofile"] .js-facet-toggle');
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '609');
        test.assertExists('.search-navigator-facet-box-forbidden[data-property="active_severities"]');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
