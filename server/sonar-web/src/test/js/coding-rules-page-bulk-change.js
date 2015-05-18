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
    testName = lib.testName('Coding Rules', 'Bulk Change');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules-page-bulk-change');
lib.configureCasper();


casper.test.begin(testName('Activate', 'Success'), function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
        lib.mockRequest('/api/qualityprofiles/activate_rules', '{ "succeeded": 225 }');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/coding-rules/app']);
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.coding-rule');
      })

      .then(function () {
        test.assertExists('.js-bulk-change');
        casper.click('.js-bulk-change');
        casper.waitForSelector('.bubble-popup');
      })

      .then(function () {
        test.assertExists('.bubble-popup .js-bulk-change[data-action="activate"]');
        casper.click('.js-bulk-change[data-action="activate"]');
        casper.waitForSelector('.modal');
      })

      .then(function () {
        test.assertExists('.modal #coding-rules-bulk-change-profile');
        test.assertExists('.modal #coding-rules-submit-bulk-change');
      })

      .then(function () {
        casper.evaluate(function () {
          jQuery('#coding-rules-bulk-change-profile').val('java-default-with-mojo-conventions-49307');
        });
      })

      .then(function () {
        casper.click('.modal #coding-rules-submit-bulk-change');
        casper.waitForSelector('.modal .alert-success');
      })

      .then(function () {
        test.assertSelectorContains('.modal', 'Default - Maven Conventions');
        test.assertSelectorContains('.modal', 'Java');
        test.assertSelectorContains('.modal', '225');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Activate', 'Failed'), function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
        lib.mockRequest('/api/qualityprofiles/activate_rules', '{ "succeeded": 225, "failed": 395 }');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/coding-rules/app']);
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.coding-rule');
      })

      .then(function () {
        test.assertExists('.js-bulk-change');
        casper.click('.js-bulk-change');
        casper.waitForSelector('.bubble-popup');
      })

      .then(function () {
        test.assertExists('.bubble-popup .js-bulk-change[data-action="activate"]');
        casper.click('.js-bulk-change[data-action="activate"]');
        casper.waitForSelector('.modal');
      })

      .then(function () {
        test.assertExists('.modal #coding-rules-bulk-change-profile');
        test.assertExists('.modal #coding-rules-submit-bulk-change');
      })

      .then(function () {
        casper.evaluate(function () {
          jQuery('#coding-rules-bulk-change-profile').val('java-default-with-mojo-conventions-49307');
        });
      })

      .then(function () {
        casper.click('.modal #coding-rules-submit-bulk-change');
        casper.waitForSelector('.modal .alert-warning');
      })

      .then(function () {
        test.assertSelectorContains('.modal', '225');
        test.assertSelectorContains('.modal', '395');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Filter Profiles by Language'), 4, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/coding-rules/app']);
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.coding-rule');
      })

      .then(function () {
        casper.click('.js-facet[data-value="java"]');
        test.assertExists('.js-bulk-change');
        casper.click('.js-bulk-change');
        casper.waitForSelector('.bubble-popup');
      })

      .then(function () {
        test.assertExists('.bubble-popup .js-bulk-change[data-action="activate"]');
        casper.click('.js-bulk-change[data-action="activate"]');
        casper.waitForSelector('.modal');
      })

      .then(function () {
        test.assertExists('.modal #coding-rules-bulk-change-profile');
        test.assertEqual(8, casper.evaluate(function () {
          return jQuery('.modal').find('#coding-rules-bulk-change-profile').find('option').length;
        }));
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Change Selected Profile'), 4, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search-qprofile-active.json',
            { data: { activation: true } });
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
        lib.mockRequest('/api/qualityprofiles/deactivate_rules', '{ "succeeded": 7 }');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/coding-rules/app']);
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.coding-rule');
      })

      .then(function () {
        casper.click('[data-property="qprofile"] .js-facet-toggle');
        casper.waitForSelector('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
      })

      .then(function () {
        casper.click('.js-facet[data-value="java-default-with-mojo-conventions-49307"]');
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertExists('.js-bulk-change');
        casper.click('.js-bulk-change');
        casper.waitForSelector('.bubble-popup');
      })

      .then(function () {
        test.assertExists('.bubble-popup .js-bulk-change[data-param="java-default-with-mojo-conventions-49307"]');
        casper.click('.js-bulk-change[data-param="java-default-with-mojo-conventions-49307"]');
        casper.waitForSelector('.modal');
      })

      .then(function () {
        test.assertDoesntExist('.modal #coding-rules-bulk-change-profile');
        casper.click('.modal #coding-rules-submit-bulk-change');
        casper.waitForSelector('.modal .alert-success');
      })

      .then(function () {
        test.assertSelectorContains('.modal', '7');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
