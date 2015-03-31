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
    testName = lib.testName('Coding Rules', 'List Actions');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules-page-list-actions');
lib.configureCasper();


casper.test.begin(testName('Show Activation Details'), 6, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search-actives.json', { data: { activation: true } });
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/coding-rules/app.js']);
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.coding-rule');
      })

      .then(function () {
        test.assertDoesntExist('.coding-rule-activation');
        casper.click('[data-property="qprofile"] .js-facet-toggle');
        casper.waitForSelector('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
      })

      .then(function () {
        casper.click('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertElementCount('.coding-rule-activation', 2);
        test.assertElementCount('.coding-rule-activation .icon-severity-major', 2);
        test.assertElementCount('.coding-rule-activation .icon-inheritance', 1);
        test.assertDoesntExist('.coding-rules-detail-quality-profile-activate');
        test.assertDoesntExist('.coding-rules-detail-quality-profile-deactivate');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Activate'), 9, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app-admin.json');
        lib.mockRequestFromFile('/api/rules/search', 'search-inactive.json', { data: { activation: 'false' } });
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
        lib.mockRequest('/api/qualityprofiles/activate_rule', '{}');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/coding-rules/app.js']);
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.coding-rule');
      })

      .then(function () {
        test.assertDoesntExist('.coding-rule-activation');
        casper.click('[data-property="qprofile"] .js-facet-toggle');
        casper.waitForSelector('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
      })

      .then(function () {
        casper.click('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
        casper.click('.js-facet[data-value="java-default-with-mojo-conventions-49307"] .js-inactive');
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertDoesntExist('.coding-rule-activation .icon-severity-major');
        test.assertExists('.coding-rules-detail-quality-profile-activate');
        casper.click('.coding-rules-detail-quality-profile-activate');
        casper.waitForSelector('.modal');
      })

      .then(function () {
        test.assertExists('#coding-rules-quality-profile-activation-select');
        test.assertElementCount('#coding-rules-quality-profile-activation-select option', 1);
        test.assertExists('#coding-rules-quality-profile-activation-severity');
        casper.click('#coding-rules-quality-profile-activation-activate');
        casper.waitForSelector('.coding-rule-activation .icon-severity-major');
      })

      .then(function () {
        test.assertExist('.coding-rule-activation .icon-severity-major');
        test.assertDoesntExist('.coding-rules-detail-quality-profile-activate');
        test.assertExist('.coding-rules-detail-quality-profile-deactivate');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Deactivate'), 6, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app-admin.json');
        lib.mockRequestFromFile('/api/rules/search', 'search-active.json', { data: { activation: true } });
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
        lib.mockRequest('/api/qualityprofiles/deactivate_rule', '{}');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['/js/coding-rules/app.js']);
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.coding-rule');
      })

      .then(function () {
        test.assertDoesntExist('.coding-rule-activation');
        casper.click('[data-property="qprofile"] .js-facet-toggle');
        casper.waitForSelector('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
      })

      .then(function () {
        casper.click('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertExists('.coding-rule-activation .icon-severity-major');
        test.assertDoesntExist('.coding-rules-detail-quality-profile-activate');
        casper.click('.coding-rules-detail-quality-profile-deactivate');
        casper.waitForSelector('button[data-confirm="yes"]');
      })

      .then(function () {
        casper.click('button[data-confirm="yes"]');
        casper.waitWhileSelector('.coding-rule-activation .icon-severity-major');
      })

      .then(function () {
        test.assertDoesntExist('.coding-rule-activation .icon-severity-major');
        test.assertExist('.coding-rules-detail-quality-profile-activate');
        test.assertDoesntExist('.coding-rules-detail-quality-profile-deactivate');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
