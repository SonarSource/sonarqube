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
    testName = lib.testName('Coding Rules');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules');
lib.configureCasper();


casper.test.begin(testName('Move Between Rules From Detailed View'), 3, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
        this.showMock = lib.mockRequestFromFile('/api/rules/show', 'show.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/coding-rules/app.js']);
        });
      })

      .then(function () {
        casper.waitForSelector('.coding-rule.selected', function () {
          casper.click('.coding-rule.selected .js-rule');
        });
      })

      .then(function () {
        casper.waitForSelector('.coding-rules-detail-header');
      })

      .then(function () {
        test.assertSelectorContains('.coding-rules-detail-header', '".equals()" should not be used to test the values');

        lib.clearRequestMock(this.showMock);
        this.showMock = lib.mockRequestFromFile('/api/rules/show', 'show2.json');

        casper.click('.js-next');
        casper.waitForSelectorTextChange('.coding-rules-detail-header');
      })

      .then(function () {
        test.assertSelectorContains('.coding-rules-detail-header', '"@Override" annotation should be used on any');

        lib.clearRequestMock(this.showMock);
        this.showMock = lib.mockRequestFromFile('/api/rules/show', 'show.json');

        casper.click('.js-prev');
        casper.waitForSelectorTextChange('.coding-rules-detail-header');
      })

      .then(function () {
        test.assertSelectorContains('.coding-rules-detail-header', '".equals()" should not be used to test the values');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Filter Similar Rules'), 3, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();


        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search-sql-tag.json', { data: { tags: 'sql' } });
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/coding-rules/app.js']);
        });
      })

      .then(function () {
        casper.waitForSelector('.coding-rule.selected .js-rule-filter');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '609');

        casper.click('.js-rule-filter');
        casper.waitForSelector('.bubble-popup');
      })

      .then(function () {
        test.assertExists('.bubble-popup [data-property="languages"][data-value="java"]');

        casper.click('.bubble-popup [data-property="tags"][data-value="sql"]');
        casper.wait(1000, function () { lib.capture(); });
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', '2');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
