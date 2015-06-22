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
    testName = lib.testName('Web Service API');

lib.initMessages();
lib.changeWorkingDirectory('api-documentation');
lib.configureCasper();


casper.test.begin(testName('Should Show List'), 12, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/webservices/list', 'list.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/api-documentation/app'], function (App) {
            App.start({ el: '#content', urlRoot: '/pages/base' });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('.api-documentation-results .list-group-item');
      })

      .then(function () {
        test.assertElementCount('.api-documentation-results .list-group-item', 2);
        test.assertSelectorContains('.list-group-item[data-path="api/public"] .list-group-item-heading', 'api/public');
        test.assertSelectorContains('.list-group-item[data-path="api/public"] .list-group-item-text', 'api/public description');
        test.assertSelectorContains('.list-group-item[data-path="api/internal"] .list-group-item-heading', 'api/internal');
        test.assertSelectorContains('.list-group-item[data-path="api/internal"] .list-group-item-text', 'api/internal description');
        test.assertSelectorContains('.list-group-item[data-path="api/internal"]', 'internal');
      })

      .then(function () {
        test.assertVisible('.list-group-item[data-path="api/public"]');
        test.assertNotVisible('.list-group-item[data-path="api/internal"]');
        test.assertFail(casper.evaluate(function () {
          return jQuery('#api-documentation-show-internal').is(':checked');
        }));
      })

      .then(function () {
        casper.click('#api-documentation-show-internal');

        test.assertVisible('.list-group-item[data-path="api/public"]');
        test.assertVisible('.list-group-item[data-path="api/internal"]');
        test.assert(casper.evaluate(function () {
          return jQuery('#api-documentation-show-internal').is(':checked');
        }));
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Should Show Actions'), 10, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/webservices/list', 'list.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/api-documentation/app'], function (App) {
            App.start({ el: '#content', urlRoot: '/pages/base' });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('.api-documentation-results .list-group-item');
      })

      .then(function () {
        casper.click('.list-group-item[data-path="api/public"]');

        test.assertElementCount('.search-navigator-workspace-details .panel', 2);
        test.assertSelectorContains('.panel[data-action="do"]', 'POST api/public/do');
        test.assertSelectorContains('.panel[data-action="do"]', 'api/public/do description');
        test.assertSelectorContains('.panel[data-action="do"]', 'Since 3.6');

        test.assertElementCount('.panel[data-action="do"] table tr', 1);
        test.assertSelectorContains('.panel[data-action="do"] table tr', 'format');
        test.assertSelectorContains('.panel[data-action="do"] table tr', 'optional');
        test.assertSelectorContains('.panel[data-action="do"] table tr', 'api/public/do format description');
        test.assertSelectorContains('.panel[data-action="do"] table tr', 'json');
        test.assertSelectorContains('.panel[data-action="do"] table tr', 'xml');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Should Show Example Response'), 1, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/webservices/list', 'list.json');
        lib.mockRequestFromFile('/api/webservices/response_example', 'response-example.json',
            { data: { controller: 'api/public', action: 'undo' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/api-documentation/app'], function (App) {
            App.start({ el: '#content', urlRoot: '/pages/base' });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('.api-documentation-results .list-group-item');
      })

      .then(function () {
        casper.click('.list-group-item[data-path="api/public"]');
        casper.click('.panel[data-action="undo"] .js-show-response-example');
        casper.waitForSelector('.panel[data-action="undo"] pre');
      })

      .then(function () {
        test.assertSelectorContains('.panel[data-action="undo"] pre', 'leia.organa');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Web Service Permalink'), 1, function (test) {
  casper
      .start(lib.buildUrl('base#api/public'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/webservices/list', 'list.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/api-documentation/app'], function (App) {
            App.start({ el: '#content', urlRoot: '/pages/base' });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('.panel[data-web-service="api/public"]');
      })

      .then(function () {
        test.assertElementCount('.panel[data-web-service="api/public"]', 2);
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Action Permalink'), 1, function (test) {
  casper
      .start(lib.buildUrl('base#api/internal/move'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/webservices/list', 'list.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/api-documentation/app'], function (App) {
            App.start({ el: '#content', urlRoot: '/pages/base' });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('.panel[data-web-service="api/internal"]');
      })

      .then(function () {
        test.assertExists('.panel[data-web-service="api/internal"][data-action="move"]');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
