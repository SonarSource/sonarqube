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
    testName = lib.testName('Quality Gates');

lib.initMessages();
lib.changeWorkingDirectory('quality-gates-spec');
lib.configureCasper();


casper.test.begin(testName('Should Show List'), 5, function (test) {
  casper
      .start(lib.buildUrl('quality_gates'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/qualitygates/app', 'app.json');
        lib.mockRequestFromFile('/api/qualitygates/list', 'list.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/quality-gate/app']);
        });
      })

      .then(function () {
        casper.waitForSelector('.quality-gates-results .list-group-item');
      })

      .then(function () {
        test.assertElementCount('.quality-gates-results .list-group-item', 3);
        test.assertSelectorContains('.quality-gates-results .list-group-item', 'SonarQube way');
        test.assertSelectorContains('.quality-gates-results .list-group-item', 'Simple Gate');
        test.assertSelectorContains('.quality-gates-results .list-group-item', 'Another Gate');

        test.assertElementCount('.quality-gates-results .badge', 1);
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Should Show Details', 'Anonymous'), 12, function (test) {
  casper
      .start(lib.buildUrl('quality_gates'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/qualitygates/app', 'app-anonymous.json');
        lib.mockRequestFromFile('/api/qualitygates/list', 'list.json');
        lib.mockRequestFromFile('/api/qualitygates/show?id=1', 'show.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/quality-gate/app']);
        });
      })

      .then(function () {
        casper.waitForSelector('.quality-gates-results .list-group-item');
      })

      .then(function () {
        // FIXME use better selector
        casper.click('.quality-gates-results .list-group-item:last-child');
        casper.waitForSelector('.search-navigator-header-component');
      })

      .then(function () {
        test.assertElementCount('.quality-gates-results .list-group-item.active', 1);
        test.assertSelectorContains('.quality-gates-results .list-group-item.active', 'SonarQube way');

        test.assertSelectorContains('.search-navigator-workspace-header', 'SonarQube way');
        test.assertDoesntExist('#quality-gate-rename');
        test.assertDoesntExist('#quality-gate-copy');
        test.assertDoesntExist('#quality-gate-unset-as-default');
        test.assertDoesntExist('#quality-gate-delete');

        test.assertExists('.quality-gate-conditions');
        test.assertElementCount('.quality-gate-conditions tbody tr', 8);
        test.assertDoesntExist('.quality-gate-conditions .update-condition');
        test.assertDoesntExist('.quality-gate-conditions .delete-condition');

        test.assertExists('.quality-gate-default-message');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Should Show Details', 'Admin'), 12, function (test) {
  casper
      .start(lib.buildUrl('quality_gates'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/qualitygates/app', 'app.json');
        lib.mockRequestFromFile('/api/qualitygates/list', 'list.json');
        lib.mockRequestFromFile('/api/qualitygates/show?id=1', 'show.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/quality-gate/app']);
        });
      })

      .then(function () {
        casper.waitForSelector('.quality-gates-results .list-group-item');
      })

      .then(function () {
        // FIXME use better selector
        casper.click('.quality-gates-results .list-group-item:last-child');
        casper.waitForSelector('.search-navigator-header-component');
      })

      .then(function () {
        test.assertElementCount('.quality-gates-results .list-group-item.active', 1);
        test.assertSelectorContains('.quality-gates-results .list-group-item.active', 'SonarQube way');

        test.assertSelectorContains('.search-navigator-workspace-header', 'SonarQube way');
        test.assertExists('#quality-gate-rename');
        test.assertExists('#quality-gate-copy');
        test.assertExists('#quality-gate-unset-as-default');
        test.assertExists('#quality-gate-delete');

        test.assertExists('.quality-gate-conditions');
        test.assertElementCount('.quality-gate-conditions tbody tr', 8);
        test.assertElementCount('.quality-gate-conditions .update-condition', 8);
        test.assertElementCount('.quality-gate-conditions .delete-condition', 8);

        test.assertExists('.quality-gate-default-message');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Should Rename'), 2, function (test) {
  casper
      .start(lib.buildUrl('quality_gates#show/1'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/qualitygates/app', 'app.json');
        lib.mockRequestFromFile('/api/qualitygates/list', 'list.json');
        lib.mockRequestFromFile('/api/qualitygates/show?id=1', 'show.json');
        lib.mockRequestFromFile('/api/qualitygates/rename', 'rename.json', { data: { id: '1', name: 'New Name' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/quality-gate/app']);
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.search-navigator-header-component');
      })

      .then(function () {
        casper.click('#quality-gate-rename');
        casper.waitUntilVisible('#quality-gate-edit-name');
      })

      .then(function () {
        casper.evaluate(function () {
          jQuery('#quality-gate-edit-name').val('New Name');
        });
        casper.click('.modal-foot button');
        casper.waitForSelectorTextChange('.search-navigator-header-component');
      })

      .then(function () {
        test.assertSelectorContains('.search-navigator-header-component', 'New Name');
        test.assertSelectorContains('.quality-gates-results .list-group-item.active', 'New Name');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Should Copy'), 3, function (test) {
  casper
      .start(lib.buildUrl('quality_gates#show/1'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/qualitygates/app', 'app.json');
        lib.mockRequestFromFile('/api/qualitygates/list', 'list.json');
        lib.mockRequestFromFile('/api/qualitygates/show?id=1', 'show.json');
        lib.mockRequestFromFile('/api/qualitygates/copy', 'copy.json', { data: { id: '1', name: 'New Name' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/quality-gate/app']);
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.search-navigator-header-component');
      })

      .then(function () {
        casper.click('#quality-gate-copy');
        casper.waitUntilVisible('#quality-gate-edit-name');
      })

      .then(function () {
        casper.evaluate(function () {
          jQuery('#quality-gate-edit-name').val('New Name');
        });
        casper.click('.modal-foot button');
        casper.waitForSelectorTextChange('.search-navigator-header-component');
      })

      .then(function () {
        test.assertSelectorContains('.search-navigator-header-component', 'New Name');
        test.assertSelectorContains('.quality-gates-results .list-group-item.active', 'New Name');
        test.assertSelectorContains('.quality-gates-results .list-group-item', 'SonarQube way');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Should Set As Default'), 4, function (test) {
  casper
      .start(lib.buildUrl('quality_gates#show/5'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/qualitygates/app', 'app.json');
        lib.mockRequestFromFile('/api/qualitygates/list', 'list.json');
        lib.mockRequestFromFile('/api/qualitygates/show?id=5', 'show-another.json');
        lib.mockRequest('/api/qualitygates/set_as_default', '{}', { data: { id: '5' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/quality-gate/app']);
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.search-navigator-header-component');
      })

      .then(function () {
        test.assertDoesntExist('.quality-gates-results .list-group-item.active .badge');
        test.assertDoesntExist('.quality-gate-default-message');
        casper.click('#quality-gate-set-as-default');
        casper.waitForSelector('.quality-gates-results .list-group-item.active .badge');
      })

      .then(function () {
        test.assertExists('.quality-gate-default-message');
        test.assertElementCount('.quality-gates-results .badge', 1);
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Should Unset As Default'), 4, function (test) {
  casper
      .start(lib.buildUrl('quality_gates#show/1'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/qualitygates/app', 'app.json');
        lib.mockRequestFromFile('/api/qualitygates/list', 'list.json');
        lib.mockRequestFromFile('/api/qualitygates/show?id=1', 'show.json');
        lib.mockRequest('/api/qualitygates/unset_default', '{}');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/quality-gate/app']);
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.search-navigator-header-component');
      })

      .then(function () {
        test.assertExists('.quality-gates-results .list-group-item.active .badge');
        test.assertExists('.quality-gate-default-message');
        casper.click('#quality-gate-unset-as-default');
        casper.waitWhileSelector('.quality-gates-results .list-group-item.active .badge');
      })

      .then(function () {
        test.assertDoesntExist('.quality-gate-default-message');
        test.assertDoesntExist('.quality-gates-results .badge');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Should Create'), 2, function (test) {
  casper
      .start(lib.buildUrl('quality_gates'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/qualitygates/app', 'app.json');
        lib.mockRequestFromFile('/api/qualitygates/list', 'list.json');
        lib.mockRequestFromFile('/api/qualitygates/create', 'create.json', { data: { name: 'New Name' } });
        lib.mockRequestFromFile('/api/qualitygates/show?id=6', 'show-created.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/quality-gate/app']);
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.quality-gates-results .list-group-item');
      })

      .then(function () {
        casper.click('#quality-gate-add');
        casper.waitUntilVisible('#quality-gate-edit-name');
      })

      .then(function () {
        casper.evaluate(function () {
          jQuery('#quality-gate-edit-name').val('New Name');
        });
        casper.click('.modal-foot button');
        casper.waitForSelector('.search-navigator-header-component');
      })

      .then(function () {
        test.assertSelectorContains('.search-navigator-header-component', 'New Name');
        test.assertSelectorContains('.quality-gates-results .list-group-item.active', 'New Name');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Should Delete'), 2, function (test) {
  casper
      .start(lib.buildUrl('quality_gates#show/5'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/qualitygates/app', 'app.json');
        lib.mockRequestFromFile('/api/qualitygates/list', 'list.json');
        lib.mockRequestFromFile('/api/qualitygates/show?id=5', 'show-another.json');
        lib.mockRequest('/api/qualitygates/destroy', '{}', { data: { id: '5' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/quality-gate/app']);
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.search-navigator-header-component');
      })

      .then(function () {
        test.assertElementCount('.quality-gates-results .list-group-item', 3);
        casper.click('#quality-gate-delete');
        casper.waitForSelector('button[data-confirm="yes"]');
      })

      .then(function () {
        casper.click('button[data-confirm="yes"]');
        casper.waitWhileSelector('.search-navigator-header-component');
      })

      .then(function () {
        test.assertElementCount('.quality-gates-results .list-group-item', 2);
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Should Add Condition'), 6, function (test) {
  casper
      .start(lib.buildUrl('quality_gates#show/5'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/qualitygates/app', 'app.json');
        lib.mockRequestFromFile('/api/qualitygates/list', 'list.json');
        lib.mockRequestFromFile('/api/qualitygates/show?id=5', 'show-another.json');
        lib.mockRequestFromFile('/api/qualitygates/create_condition', 'create-condition.json',
            { data: { metric: 'complexity', op: 'GT', period: '1', warning: '3', error: '4' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/quality-gate/app']);
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.search-navigator-header-component');
      })

      .then(function () {
        test.assertElementCount('.quality-gate-conditions [name="error"]', 0);

        casper.evaluate(function () {
          jQuery('#quality-gate-new-condition-metric').val('complexity').change();
        });
        test.assertElementCount('.quality-gate-conditions [name="error"]', 1);
      })

      .then(function () {
        casper.click('.cancel-add-condition');
        casper.waitWhileSelector('.cancel-add-condition');
      })

      .then(function () {
        test.assertElementCount('.quality-gate-conditions [name="error"]', 0);

        casper.evaluate(function () {
          jQuery('#quality-gate-new-condition-metric').val('complexity').change();
        });
        test.assertElementCount('.quality-gate-conditions [name="error"]', 1);

        casper.evaluate(function () {
          jQuery('[name="period"]').val('1');
          jQuery('[name="operator"]').val('GT');
          jQuery('[name="warning"]').val('3');
          jQuery('[name="error"]').val('4');
        });
        casper.click('.add-condition');
        casper.waitForSelector('.update-condition');
      })

      .then(function () {
        test.assertExists('.update-condition[disabled]');
        test.assertExists('.delete-condition');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Should Update Condition'), 3, function (test) {
  casper
      .start(lib.buildUrl('quality_gates#show/1'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/qualitygates/app', 'app.json');
        lib.mockRequestFromFile('/api/qualitygates/list', 'list.json');
        lib.mockRequestFromFile('/api/qualitygates/show?id=1', 'show.json');
        lib.mockRequestFromFile('/api/qualitygates/update_condition', 'update-condition.json',
            { data: { id: '1', warning: '173' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/quality-gate/app']);
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.update-condition');
      })

      .then(function () {
        test.assertExists('.quality-gate-conditions tr:first-child .update-condition[disabled]');
        casper.evaluate(function () {
          jQuery('.quality-gate-conditions tr:first-child [name="warning"]').val('173').change();
        });
        test.assertDoesntExist('.quality-gate-conditions tr:first-child .update-condition[disabled]');
        casper.click('.quality-gate-conditions tr:first-child .update-condition');
        casper.waitWhileSelector('.quality-gate-conditions tr:first-child .update-condition:not([disabled])');
      })

      .then(function () {
        test.assertExists('.quality-gate-conditions tr:first-child .update-condition[disabled]');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Should Delete Condition'), 2, function (test) {
  casper
      .start(lib.buildUrl('quality_gates#show/1'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/qualitygates/app', 'app.json');
        lib.mockRequestFromFile('/api/qualitygates/list', 'list.json');
        lib.mockRequestFromFile('/api/qualitygates/show?id=1', 'show.json');
        lib.mockRequest('/api/qualitygates/delete_condition', '{}', { data: { id: '1' } });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/quality-gate/app']);
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('.delete-condition');
      })

      .then(function () {
        test.assertElementCount('.delete-condition', 8);

        casper.click('.quality-gate-conditions tr:first-child .delete-condition');
        casper.waitForSelector('button[data-confirm="yes"]');
      })

      .then(function () {
        casper.click('button[data-confirm="yes"]');
        lib.waitForElementCount('.delete-condition', 7);
      })

      .then(function () {
        test.assert(true);
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
