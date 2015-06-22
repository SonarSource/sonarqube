/* global describe:false, it:false */
var lib = require('../lib');

describe('Quality Gates App', function () {

  it('should show list', 5, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();

          lib.fmock('/api/qualitygates/app', 'app.json');
          lib.fmock('/api/qualitygates/list', 'list.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/quality-gates/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.js-list .list-group-item');
        })

        .then(function () {
          test.assertElementCount('.js-list .list-group-item', 3);
          test.assertSelectorContains('.js-list .list-group-item', 'SonarQube way');
          test.assertSelectorContains('.js-list .list-group-item', 'Simple Gate');
          test.assertSelectorContains('.js-list .list-group-item', 'Another Gate');

          test.assertElementCount('.js-list .badge', 1);
        });
  });


  it('should show details for anonymous', 14, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();

          lib.fmock('/api/qualitygates/app', 'app-anonymous.json');
          lib.fmock('/api/qualitygates/list', 'list.json');
          lib.fmock('/api/qualitygates/show', 'show.json', { data: { id: '1' } });
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/quality-gates/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.js-list .list-group-item');
        })

        .then(function () {
          casper.click('.js-list .list-group-item[data-id="1"]');
          casper.waitForSelector('.search-navigator-header-component');
        })

        .then(function () {
          test.assertElementCount('.js-list .list-group-item.active', 1);
          test.assertSelectorContains('.js-list .list-group-item.active', 'SonarQube way');

          test.assertSelectorContains('.search-navigator-workspace-header', 'SonarQube way');
          test.assertDoesntExist('#quality-gate-rename');
          test.assertDoesntExist('#quality-gate-copy');
          test.assertDoesntExist('#quality-gate-unset-as-default');
          test.assertDoesntExist('#quality-gate-delete');

          test.assertExists('.js-conditions');
          test.assertElementCount('.js-conditions tbody tr', 8);
          test.assertDoesntExist('.js-conditions .update-condition');
          test.assertDoesntExist('.js-conditions .delete-condition');

          test.assertExists('.quality-gate-default-message');
        })

        .then(function () {
          test.assertNotVisible('.js-more');
          casper.click('.js-show-more');
          test.assertVisible('.js-more');
        });
  });


  it('should show details for admin', 12, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();

          lib.fmock('/api/qualitygates/app', 'app.json');
          lib.fmock('/api/qualitygates/list', 'list.json');
          lib.fmock('/api/qualitygates/show', 'show.json', { data: { id: '1' } });
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/quality-gates/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.js-list .list-group-item');
        })

        .then(function () {
          casper.click('.js-list .list-group-item[data-id="1"]');
          casper.waitForSelector('.search-navigator-header-component');
        })

        .then(function () {
          test.assertElementCount('.js-list .list-group-item.active', 1);
          test.assertSelectorContains('.js-list .list-group-item.active', 'SonarQube way');

          test.assertSelectorContains('.search-navigator-workspace-header', 'SonarQube way');
          test.assertExists('#quality-gate-rename');
          test.assertExists('#quality-gate-copy');
          test.assertExists('#quality-gate-toggle-default');
          test.assertExists('#quality-gate-delete');

          test.assertExists('.js-conditions');
          test.assertElementCount('.js-conditions tbody tr', 8);
          test.assertElementCount('.js-conditions .update-condition', 8);
          test.assertElementCount('.js-conditions .delete-condition', 8);

          test.assertExists('.quality-gate-default-message');
        });
  });


  it('should show projects', 2, function (casper, test) {
    return casper
        .start(lib.buildUrl('base#show/5'), function () {
          lib.setDefaultViewport();

          lib.fmock('/api/qualitygates/app', 'app-anonymous.json');
          lib.fmock('/api/qualitygates/list', 'list.json');
          lib.fmock('/api/qualitygates/show', 'show-another.json', { data: { id: '5' } });
          lib.fmock('/api/qualitygates/search?gateId=5', 'projects.json');
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/quality-gates/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.select-list-list li');
        })

        .then(function () {
          test.assertElementCount('.select-list-list li', 1);
          test.assertSelectorContains('.select-list-list li', 'SonarQube');
        });
  });


  it('should rename', 2, function (casper, test) {
    return casper
        .start(lib.buildUrl('base#show/1'), function () {
          lib.setDefaultViewport();

          lib.fmock('/api/qualitygates/app', 'app.json');
          lib.fmock('/api/qualitygates/list', 'list.json');
          lib.fmock('/api/qualitygates/show', 'show.json', { data: { id: '1' } });
          lib.fmock('/api/qualitygates/rename', 'rename.json', { data: { id: '1', name: 'New Name' } });
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/quality-gates/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.search-navigator-header-component');
        })

        .then(function () {
          casper.click('#quality-gate-rename');
          casper.waitUntilVisible('#quality-gate-form-name');
        })

        .then(function () {
          casper.evaluate(function () {
            jQuery('#quality-gate-form-name').val('New Name');
          });
          casper.click('.modal-foot button');
          casper.waitForSelectorTextChange('.search-navigator-header-component');
        })

        .then(function () {
          test.assertSelectorContains('.search-navigator-header-component', 'New Name');
          test.assertSelectorContains('.js-list .list-group-item.active', 'New Name');
        });
  });


  it('should copy', 3, function (casper, test) {
    return casper
        .start(lib.buildUrl('base#show/1'), function () {
          lib.setDefaultViewport();

          lib.fmock('/api/qualitygates/app', 'app.json');
          lib.fmock('/api/qualitygates/list', 'list.json');
          lib.fmock('/api/qualitygates/show', 'show.json', { data: { id: '1' } });
          lib.fmock('/api/qualitygates/show', 'show-created.json', { data: { id: '6' } });
          lib.fmock('/api/qualitygates/copy', 'copy.json', { data: { id: '1', name: 'New Name' } });
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/quality-gates/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.search-navigator-header-component');
        })

        .then(function () {
          casper.click('#quality-gate-copy');
          casper.waitUntilVisible('#quality-gate-form-name');
        })

        .then(function () {
          casper.evaluate(function () {
            jQuery('#quality-gate-form-name').val('New Name');
          });
          casper.click('.modal-foot button');
          casper.waitForSelectorTextChange('.search-navigator-header-component');
        })

        .then(function () {
          test.assertSelectorContains('.search-navigator-header-component', 'New Name');
          test.assertSelectorContains('.js-list .list-group-item.active', 'New Name');
          test.assertSelectorContains('.js-list .list-group-item', 'SonarQube way');
        });
  });


  it('should set as default', 4, function (casper, test) {
    return casper
        .start(lib.buildUrl('base#show/5'), function () {
          lib.setDefaultViewport();

          lib.fmock('/api/qualitygates/app', 'app.json');
          lib.fmock('/api/qualitygates/list', 'list.json');
          lib.fmock('/api/qualitygates/show', 'show-another.json', { data: { id: '5' } });
          lib.smock('/api/qualitygates/set_as_default', '{}', { data: { id: '5' } });
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/quality-gates/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.search-navigator-header-component');
        })

        .then(function () {
          test.assertDoesntExist('.js-list .list-group-item.active .badge');
          test.assertDoesntExist('.quality-gate-default-message');
          casper.click('#quality-gate-toggle-default');
          casper.waitForSelector('.js-list .list-group-item.active .badge');
        })

        .then(function () {
          test.assertExists('.quality-gate-default-message');
          test.assertElementCount('.js-list .badge', 1);
        });
  });


  it('should unset as default', 4, function (casper, test) {
    return casper
        .start(lib.buildUrl('base#show/1'), function () {
          lib.setDefaultViewport();

          lib.fmock('/api/qualitygates/app', 'app.json');
          lib.fmock('/api/qualitygates/list', 'list.json');
          lib.fmock('/api/qualitygates/show', 'show.json', { data: { id: '1' } });
          lib.smock('/api/qualitygates/unset_default', '{}', { data: { id: '1' } });
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/quality-gates/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.search-navigator-header-component');
        })

        .then(function () {
          test.assertExists('.js-list .list-group-item.active .badge');
          test.assertExists('.quality-gate-default-message');
          casper.click('#quality-gate-toggle-default');
          casper.waitWhileSelector('.js-list .list-group-item.active .badge');
        })

        .then(function () {
          test.assertDoesntExist('.quality-gate-default-message');
          test.assertDoesntExist('.js-list .badge');
        });
  });


  it('should create', 2, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();

          lib.fmock('/api/qualitygates/app', 'app.json');
          lib.fmock('/api/qualitygates/list', 'list.json');
          lib.smock('/api/qualitygates/create', '{"errors":[{"msg": "error"}]}',
              { status: 400, data: { name: 'Bad' } });
          lib.fmock('/api/qualitygates/create', 'create.json', { data: { name: 'New Name' } });
          lib.fmock('/api/qualitygates/show', 'show-created.json', { data: { id: '6' } });
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/quality-gates/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.js-list .list-group-item');
        })

        .then(function () {
          casper.click('#quality-gate-add');
          casper.waitUntilVisible('#quality-gate-form-name');
        })

        .then(function () {
          casper.evaluate(function () {
            jQuery('#quality-gate-form-name').val('Bad');
          });
          casper.click('.modal-foot button');
          casper.waitForSelector('.alert-danger');
        })

        .then(function () {
          casper.evaluate(function () {
            jQuery('#quality-gate-form-name').val('New Name');
          });
          casper.click('.modal-foot button');
          casper.waitForSelector('.search-navigator-header-component');
        })

        .then(function () {
          test.assertSelectorContains('.search-navigator-header-component', 'New Name');
          test.assertSelectorContains('.js-list .list-group-item.active', 'New Name');
        });
  });


  it('should delete', 2, function (casper, test) {
    return casper
        .start(lib.buildUrl('base#show/5'), function () {
          lib.setDefaultViewport();

          lib.fmock('/api/qualitygates/app', 'app.json');
          lib.fmock('/api/qualitygates/list', 'list.json');
          lib.fmock('/api/qualitygates/show', 'show-another.json', { data: { id: '5' } });
          this.deleteMock = lib.smock('/api/qualitygates/destroy', '{"errors":[{"msg": "error"}]}',
              { status: 400 });
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/quality-gates/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.search-navigator-header-component');
        })

        .then(function () {
          test.assertElementCount('.js-list .list-group-item', 3);
          casper.click('#quality-gate-delete');
          casper.waitForSelector('#delete-gate-submit');
        })

        .then(function () {
          casper.click('#delete-gate-submit');
          casper.waitForSelector('.alert-danger');
        })

        .then(function () {
          lib.clearRequestMock(this.deleteMock);
          lib.smock('/api/qualitygates/destroy', '{}', { data: { id: '5' } });

          casper.click('#delete-gate-submit');
          casper.waitWhileSelector('.search-navigator-header-component');
        })

        .then(function () {
          test.assertElementCount('.js-list .list-group-item', 2);
        });
  });


  it('should add condition', 6, function (casper, test) {
    return casper
        .start(lib.buildUrl('base#show/5'), function () {
          lib.setDefaultViewport();

          lib.fmock('/api/qualitygates/app', 'app.json');
          lib.fmock('/api/qualitygates/list', 'list.json');
          lib.fmock('/api/qualitygates/show', 'show-another.json', { data: { id: '5' } });
          lib.fmock('/api/qualitygates/create_condition', 'create-condition.json',
              { data: { gateId: '5', metric: 'complexity', op: 'GT', period: '1', warning: '3', error: '4' } });
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/quality-gates/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.search-navigator-header-component');
        })

        .then(function () {
          test.assertElementCount('.js-conditions [name="error"]', 0);

          casper.evaluate(function () {
            jQuery('#quality-gate-new-condition-metric').val('complexity').change();
          });
          test.assertElementCount('.js-conditions [name="error"]', 1);
        })

        .then(function () {
          casper.click('.cancel-add-condition');
          casper.waitWhileSelector('.cancel-add-condition');
        })

        .then(function () {
          test.assertElementCount('.js-conditions [name="error"]', 0);

          casper.evaluate(function () {
            jQuery('#quality-gate-new-condition-metric').val('complexity').change();
          });
          test.assertElementCount('.js-conditions [name="error"]', 1);

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
        });
  });


  it('should update condition', 3, function (casper, test) {
    return casper
        .start(lib.buildUrl('base#show/1'), function () {
          lib.setDefaultViewport();

          lib.fmock('/api/qualitygates/app', 'app.json');
          lib.fmock('/api/qualitygates/list', 'list.json');
          lib.fmock('/api/qualitygates/show', 'show.json', { data: { id: '1' } });
          lib.fmock('/api/qualitygates/update_condition', 'update-condition.json',
              { data: { id: '1', warning: '173' } });
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/quality-gates/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.update-condition');
        })

        .then(function () {
          test.assertExists('.js-conditions tr:first-child .update-condition[disabled]');
          casper.evaluate(function () {
            jQuery('.js-conditions tr:first-child [name="warning"]').val('173').change();
          });
          test.assertDoesntExist('.js-conditions tr:first-child .update-condition[disabled]');
          casper.click('.js-conditions tr:first-child .update-condition');
          casper.waitWhileSelector('.js-conditions tr:first-child .update-condition:not([disabled])');
        })

        .then(function () {
          test.assertExists('.js-conditions tr:first-child .update-condition[disabled]');
        });
  });


  it('should delete condition', 1, function (casper, test) {
    return casper
        .start(lib.buildUrl('base#show/1'), function () {
          lib.setDefaultViewport();

          lib.fmock('/api/qualitygates/app', 'app.json');
          lib.fmock('/api/qualitygates/list', 'list.json');
          lib.fmock('/api/qualitygates/show', 'show.json', { data: { id: '1' } });
          this.deleteMock = lib.smock('/api/qualitygates/delete_condition', '{"errors":[{"msg": "error"}]}',
              { status: 400 });
        })

        .then(function () {
          casper.evaluate(function () {
            require(['apps/quality-gates/app'], function (App) {
              App.start({ el: '#content', urlRoot: '/pages/base' });
            });
            jQuery.ajaxSetup({ dataType: 'json' });
          });
        })

        .then(function () {
          casper.waitForSelector('.delete-condition');
        })

        .then(function () {
          test.assertElementCount('.delete-condition', 8);

          casper.click('.js-conditions tr:first-child .delete-condition');
          casper.waitForSelector('#delete-condition-submit');
        })

        .then(function () {
          casper.click('#delete-condition-submit');
          casper.waitForSelector('.alert-danger');
        })

        .then(function () {
          lib.clearRequestMock(this.deleteMock);
          lib.smock('/api/qualitygates/delete_condition', '{}', { data: { id: '1' } });
          casper.click('#delete-condition-submit');
          lib.waitForElementCount('.delete-condition', 7);
        });
  });

});
