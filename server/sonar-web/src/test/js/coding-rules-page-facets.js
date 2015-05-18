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
    testName = lib.testName('Coding Rules', 'Facets');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules-page-facets');
lib.configureCasper();


casper.test.begin(testName('Characteristic'), 6, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');

        lib.mockRequestFromFile('/api/rules/search', 'search-with-portability-characteristic.json',
            { data: { debt_characteristics: 'PORTABILITY' } });

        lib.mockRequestFromFile('/api/rules/search', 'search-with-memory-efficiency-characteristic.json',
            { data: { debt_characteristics: 'MEMORY_EFFICIENCY' } });

        lib.mockRequestFromFile('/api/rules/search', 'search-without-characteristic.json',
            { data: { has_debt_characteristic: 'false' } });

        lib.mockRequestFromFile('/api/rules/search', 'search-characteristic.json',
            { data: { facets: 'debt_characteristics' } });

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
        // enable facet
        test.assertExists('.search-navigator-facet-box-collapsed[data-property="debt_characteristics"]');
        casper.click('[data-property="debt_characteristics"] .js-facet-toggle');
        casper.waitForSelector('.js-facet[data-value="PORTABILITY"]');
      })

      .then(function () {
        // select characteristic
        test.assertElementCount('[data-property="debt_characteristics"] .js-facet', 32);
        test.assertElementCount('[data-property="debt_characteristics"] .js-facet.search-navigator-facet-indent', 24);
        casper.click('.js-facet[data-value="PORTABILITY"]');
        casper.waitForSelectorTextChange('#coding-rules-total', function () {
          test.assertSelectorContains('#coding-rules-total', 21);
        });
      })

      .then(function () {
        // select uncharacterized
        casper.click('.js-facet[data-empty-characteristic]');
        casper.waitForSelectorTextChange('#coding-rules-total', function () {
          test.assertSelectorContains('#coding-rules-total', 208);
        });
      })

      .then(function () {
        // select sub-characteristic
        casper.click('.js-facet[data-value="MEMORY_EFFICIENCY"]');
        casper.waitForSelectorTextChange('#coding-rules-total', function () {
          test.assertSelectorContains('#coding-rules-total', 3);
        });
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Characteristic', 'Disable'), 6, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');

        lib.mockRequestFromFile('/api/rules/search', 'search-with-portability-characteristic.json',
            { data: { debt_characteristics: 'PORTABILITY' } });

        lib.mockRequestFromFile('/api/rules/search', 'search-with-memory-efficiency-characteristic.json',
            { data: { debt_characteristics: 'MEMORY_EFFICIENCY' } });

        lib.mockRequestFromFile('/api/rules/search', 'search-without-characteristic.json',
            { data: { has_debt_characteristic: 'false' } });

        lib.mockRequestFromFile('/api/rules/search', 'search-characteristic.json',
            { data: { facets: 'debt_characteristics' } });

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
        // enable facet
        casper.click('[data-property="debt_characteristics"] .js-facet-toggle');
        casper.waitForSelector('.js-facet[data-value="PORTABILITY"]');
      })

      .then(function () {
        // select characteristic
        casper.click('.js-facet[data-value="PORTABILITY"]');
        casper.waitForSelectorTextChange('#coding-rules-total', function () {
          test.assertSelectorContains('#coding-rules-total', 21);
        });
      })

      .then(function () {
        // disable facet
        casper.click('[data-property="debt_characteristics"] .js-facet-toggle');
        casper.waitForSelectorTextChange('#coding-rules-total', function () {
          test.assertSelectorContains('#coding-rules-total', 601);
        });
      })

      .then(function () {
        // enable facet
        casper.click('[data-property="debt_characteristics"] .js-facet-toggle');
        casper.waitForSelector('.js-facet[data-value="MEMORY_EFFICIENCY"]');
      })

      .then(function () {
        // select sub-characteristic
        casper.click('.js-facet[data-value="MEMORY_EFFICIENCY"]');
        casper.waitForSelectorTextChange('#coding-rules-total', function () {
          test.assertSelectorContains('#coding-rules-total', 3);
        });
      })

      .then(function () {
        // disable facet
        casper.click('[data-property="debt_characteristics"] .js-facet-toggle');
        casper.waitForSelectorTextChange('#coding-rules-total', function () {
          test.assertSelectorContains('#coding-rules-total', 601);
        });
      })

      .then(function () {
        // enable facet
        casper.click('[data-property="debt_characteristics"] .js-facet-toggle');
        casper.waitForSelector('.js-facet[data-empty-characteristic]');
      })

      .then(function () {
        // select uncharacterized
        casper.click('.js-facet[data-empty-characteristic]');
        casper.waitForSelectorTextChange('#coding-rules-total', function () {
          test.assertSelectorContains('#coding-rules-total', 208);
        });
      })

      .then(function () {
        // disable facet
        casper.click('[data-property="debt_characteristics"] .js-facet-toggle');
        casper.waitForSelectorTextChange('#coding-rules-total', function () {
          test.assertSelectorContains('#coding-rules-total', 601);
        });
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Template'), 4, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');

        lib.mockRequestFromFile('/api/rules/search', 'search-only-templates.json',
            { data: { 'is_template': 'true' } });

        lib.mockRequestFromFile('/api/rules/search', 'search-hide-templates.json',
            { data: { 'is_template': 'false' } });

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
        // enable facet
        test.assertExists('.search-navigator-facet-box-collapsed[data-property="is_template"]');
        casper.click('[data-property="is_template"] .js-facet-toggle');
        casper.waitForSelector('[data-property="is_template"] .js-facet[data-value="true"]');
      })

      .then(function () {
        // show only templates
        casper.click('[data-property="is_template"] .js-facet[data-value="true"]');
        casper.waitForSelectorTextChange('#coding-rules-total', function () {
          test.assertSelectorContains('#coding-rules-total', 8);
        });
      })

      .then(function () {
        // hide templates
        casper.click('[data-property="is_template"] .js-facet[data-value="false"]');
        casper.waitForSelectorTextChange('#coding-rules-total', function () {
          test.assertSelectorContains('#coding-rules-total', 7);
        });
      })

      .then(function () {
        // disable facet
        casper.click('[data-property="is_template"] .js-facet-toggle');
        casper.waitForSelectorTextChange('#coding-rules-total', function () {
          test.assertSelectorContains('#coding-rules-total', 601);
        });
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Language'), function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');

        lib.mockRequestFromFile('/api/rules/search', 'search-with-custom-language.json',
            { data: { languages: 'custom' } });

        lib.mockRequestFromFile('/api/rules/search', 'search.json');

        lib.mockRequest('/api/languages/list', '{"languages":[{"key":"custom","name":"Custom"}]}',
            { data: { q: 'custom' } });
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
        casper.click('[data-property="languages"] .select2-choice');
        casper.waitForSelector('.select2-search', function () {
          casper.evaluate(function () {
            jQuery('.select2-input').val('custom').trigger('keyup-change');
          });
        });
      })

      .then(function () {
        casper.waitForSelector('.select2-result');
      })

      .then(function () {
        test.assertSelectorContains('.select2-result', 'Custom');
        casper.evaluate(function () {
          jQuery('.select2-result').mouseup();
        });
      })

      .then(function () {
        casper.waitForSelectorTextChange('#coding-rules-total');
      })

      .then(function () {
        test.assertSelectorContains('#coding-rules-total', 13);
        test.assertExists('[data-property="languages"] .js-facet.active');
        test.assertSelectorContains('[data-property="languages"] .js-facet.active', 'custom');
        test.assertSelectorContains('[data-property="languages"] .js-facet.active', '13');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
