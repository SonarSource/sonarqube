define(function (require) {
  var bdd = require('intern!bdd');
  require('../helpers/test-page');

  bdd.describe('Quality Gates Page', function () {
    bdd.it('should show list', function () {
      return this.remote
          .open()
          .mockFromFile('/api/qualitygates/app', 'quality-gates-spec/app.json')
          .mockFromFile('/api/qualitygates/list', 'quality-gates-spec/list.json')
          .startAppBrowserify('quality-gates', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .checkElementCount('.js-list .list-group-item', 3)
          .checkElementInclude('.js-list .list-group-item', 'SonarQube way')
          .checkElementInclude('.js-list .list-group-item', 'Simple Gate')
          .checkElementInclude('.js-list .list-group-item', 'Another Gate')
          .checkElementCount('.js-list .badge', 1);
    });

    bdd.it('should show details for anonymous', function () {
      return this.remote
          .open()
          .mockFromFile('/api/qualitygates/app', 'quality-gates-spec/app-anonymous.json')
          .mockFromFile('/api/qualitygates/list', 'quality-gates-spec/list.json')
          .mockFromFile('/api/qualitygates/show', 'quality-gates-spec/show.json', { data: { id: 1 } })
          .startAppBrowserify('quality-gates', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .clickElement('.js-list .list-group-item[data-id="1"]')
          .checkElementExist('.search-navigator-header-component')
          .checkElementCount('.js-list .list-group-item.active', 1)
          .checkElementInclude('.js-list .list-group-item.active', 'SonarQube way')
          .checkElementInclude('.search-navigator-workspace-header', 'SonarQube way')
          .checkElementNotExist('#quality-gate-rename')
          .checkElementNotExist('#quality-gate-copy')
          .checkElementNotExist('#quality-gate-unset-as-default')
          .checkElementNotExist('#quality-gate-delete')
          .checkElementExist('.js-conditions')
          .checkElementCount('.js-conditions tbody tr', 8)
          .checkElementNotExist('.js-conditions .update-condition')
          .checkElementNotExist('.js-conditions .delete-condition')
          .checkElementExist('.quality-gate-default-message')
          .checkElementExist('.js-more.hidden')
          .clickElement('.js-show-more')
          .checkElementExist('.js-more:not(.hidden)');
    });

    bdd.it('should show details for admin', function () {
      return this.remote
          .open()
          .mockFromFile('/api/qualitygates/app', 'quality-gates-spec/app.json')
          .mockFromFile('/api/qualitygates/list', 'quality-gates-spec/list.json')
          .mockFromFile('/api/qualitygates/show', 'quality-gates-spec/show.json', { data: { id: 1 } })
          .startAppBrowserify('quality-gates', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .clickElement('.js-list .list-group-item[data-id="1"]')
          .checkElementExist('.search-navigator-header-component')
          .checkElementCount('.js-list .list-group-item.active', 1)
          .checkElementInclude('.js-list .list-group-item.active', 'SonarQube way')
          .checkElementInclude('.search-navigator-workspace-header', 'SonarQube way')
          .checkElementExist('#quality-gate-rename')
          .checkElementExist('#quality-gate-copy')
          .checkElementExist('#quality-gate-toggle-default')
          .checkElementExist('#quality-gate-delete')
          .checkElementExist('.js-conditions')
          .checkElementCount('.js-conditions tbody tr', 8)
          .checkElementCount('.js-conditions .update-condition', 8)
          .checkElementCount('.js-conditions .delete-condition', 8)
          .checkElementExist('.quality-gate-default-message');
    });

    bdd.it('should show projects', function () {
      return this.remote
          .open('#show/5')
          .mockFromFile('/api/qualitygates/app', 'quality-gates-spec/app-anonymous.json')
          .mockFromFile('/api/qualitygates/list', 'quality-gates-spec/list.json')
          .mockFromFile('/api/qualitygates/show', 'quality-gates-spec/show-another.json', { data: { id: 5 } })
          .mockFromFile('/api/qualitygates/search?gateId=5', 'quality-gates-spec/projects.json')
          .startAppBrowserify('quality-gates', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .checkElementExist('.select-list-list li')
          .checkElementCount('.select-list-list li', 1)
          .checkElementInclude('.select-list-list li', 'SonarQube');
    });

    bdd.it('should rename', function () {
      return this.remote
          .open('#show/1')
          .mockFromFile('/api/qualitygates/app', 'quality-gates-spec/app.json')
          .mockFromFile('/api/qualitygates/list', 'quality-gates-spec/list.json')
          .mockFromFile('/api/qualitygates/show', 'quality-gates-spec/show.json', { data: { id: 1 } })
          .mockFromFile('/api/qualitygates/rename', 'quality-gates-spec/rename.json', { data: { id: 1, name: 'New Name' } })
          .startAppBrowserify('quality-gates', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .checkElementExist('.search-navigator-header-component')
          .clickElement('#quality-gate-rename')
          .checkElementExist('#quality-gate-form-name')
          .fillElement('#quality-gate-form-name', 'New Name')
          .clickElement('.modal-foot button')
          .checkElementInclude('.search-navigator-header-component', 'New Name')
          .checkElementInclude('.js-list .list-group-item.active', 'New Name');
    });

    bdd.it('should copy', function () {
      return this.remote
          .open('#show/1')
          .mockFromFile('/api/qualitygates/app', 'quality-gates-spec/app.json')
          .mockFromFile('/api/qualitygates/list', 'quality-gates-spec/list.json')
          .mockFromFile('/api/qualitygates/show', 'quality-gates-spec/show.json', { data: { id: 1 } })
          .mockFromFile('/api/qualitygates/show', 'quality-gates-spec/show-created.json', { data: { id: 6 } })
          .mockFromFile('/api/qualitygates/copy', 'quality-gates-spec/copy.json', { data: { id: 1, name: 'New Name' } })
          .startAppBrowserify('quality-gates', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .checkElementExist('.search-navigator-header-component')
          .clickElement('#quality-gate-copy')
          .checkElementExist('#quality-gate-form-name')
          .fillElement('#quality-gate-form-name', 'New Name')
          .clickElement('.modal-foot button')
          .checkElementInclude('.search-navigator-header-component', 'New Name')
          .checkElementInclude('.js-list .list-group-item.active', 'New Name')
          .checkElementInclude('.js-list .list-group-item', 'SonarQube way');
    });

    bdd.it('should set as default', function () {
      return this.remote
          .open('#show/5')
          .mockFromFile('/api/qualitygates/app', 'quality-gates-spec/app.json')
          .mockFromFile('/api/qualitygates/list', 'quality-gates-spec/list.json')
          .mockFromFile('/api/qualitygates/show', 'quality-gates-spec/show-another.json', { data: { id: 5 } })
          .mockFromString('/api/qualitygates/set_as_default', '{}', { data: { id: 5 } })
          .startAppBrowserify('quality-gates', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .checkElementExist('.search-navigator-header-component')
          .checkElementNotExist('.js-list .list-group-item.active .badge')
          .checkElementNotExist('.quality-gate-default-message')
          .clickElement('#quality-gate-toggle-default')
          .checkElementExist('.js-list .list-group-item.active .badge')
          .checkElementExist('.quality-gate-default-message')
          .checkElementCount('.js-list .badge', 1);
    });

    bdd.it('should unset as default', function () {
      return this.remote
          .open('#show/1')
          .mockFromFile('/api/qualitygates/app', 'quality-gates-spec/app.json')
          .mockFromFile('/api/qualitygates/list', 'quality-gates-spec/list.json')
          .mockFromFile('/api/qualitygates/show', 'quality-gates-spec/show.json', { data: { id: 1 } })
          .mockFromString('/api/qualitygates/unset_default', '{}', { data: { id: 1 } })
          .startAppBrowserify('quality-gates', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .checkElementExist('.search-navigator-header-component')
          .checkElementExist('.js-list .list-group-item.active .badge')
          .checkElementExist('.quality-gate-default-message')
          .clickElement('#quality-gate-toggle-default')
          .checkElementNotExist('.js-list .list-group-item.active .badge')
          .checkElementNotExist('.quality-gate-default-message')
          .checkElementNotExist('.js-list .badge');
    });

    bdd.it('should create', function () {
      return this.remote
          .open()
          .mockFromFile('/api/qualitygates/app', 'quality-gates-spec/app.json')
          .mockFromFile('/api/qualitygates/list', 'quality-gates-spec/list.json')
          .mockFromString('/api/qualitygates/create', '{"errors":[{"msg": "error"}]}',
          { status: 400, data: { name: 'Bad' } })
          .mockFromFile('/api/qualitygates/create', 'quality-gates-spec/create.json', { data: { name: 'New Name' } })
          .mockFromFile('/api/qualitygates/show', 'quality-gates-spec/show-created.json', { data: { id: 6 } })
          .startAppBrowserify('quality-gates', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .clickElement('#quality-gate-add')
          .checkElementExist('#quality-gate-form-name')
          .fillElement('#quality-gate-form-name', 'Bad')
          .clickElement('.modal-foot button')
          .checkElementExist('.alert-danger')
          .fillElement('#quality-gate-form-name', 'New Name')
          .clickElement('.modal-foot button')
          .checkElementExist('.search-navigator-header-component')
          .checkElementInclude('.search-navigator-header-component', 'New Name')
          .checkElementInclude('.js-list .list-group-item.active', 'New Name');
    });

    bdd.it('should delete', function () {
      return this.remote
          .open('#show/5')
          .mockFromFile('/api/qualitygates/app', 'quality-gates-spec/app.json')
          .mockFromFile('/api/qualitygates/list', 'quality-gates-spec/list.json')
          .mockFromFile('/api/qualitygates/show', 'quality-gates-spec/show-another.json', { data: { id: 5 } })
          .mockFromString('/api/qualitygates/destroy', '{"errors":[{"msg": "error"}]}',
          { status: 400 })
          .startAppBrowserify('quality-gates', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .checkElementExist('.search-navigator-header-component')
          .checkElementCount('.js-list .list-group-item', 3)
          .clickElement('#quality-gate-delete')
          .checkElementExist('#delete-gate-submit')
          .clickElement('#delete-gate-submit')
          .checkElementExist('.alert-danger')
          .clearMocks()
          .mockFromString('/api/qualitygates/destroy', '{}', { data: { id: 5 } })
          .clickElement('#delete-gate-submit')
          .checkElementNotExist('.search-navigator-header-component')
          .checkElementCount('.js-list .list-group-item', 2);
    });

    bdd.it('should add condition', function () {
      return this.remote
          .open('#show/5')
          .mockFromFile('/api/qualitygates/app', 'quality-gates-spec/app.json')
          .mockFromFile('/api/qualitygates/list', 'quality-gates-spec/list.json')
          .mockFromFile('/api/qualitygates/show', 'quality-gates-spec/show-another.json', { data: { id: 5 } })
          .mockFromFile('/api/qualitygates/create_condition', 'quality-gates-spec/create-condition.json',
          { data: { gateId: 5, metric: 'complexity', op: 'GT', period: '1', warning: '3', error: '4' } })
          .startAppBrowserify('quality-gates', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .checkElementExist('.search-navigator-header-component')
          .checkElementCount('.js-conditions [name="error"]', 0)
          .fillElement('#quality-gate-new-condition-metric', 'complexity')
          .changeElement('#quality-gate-new-condition-metric')
          .checkElementCount('.js-conditions [name="error"]', 1)
          .clickElement('.cancel-add-condition')
          .checkElementNotExist('.cancel-add-condition')
          .checkElementCount('.js-conditions [name="error"]', 0)
          .fillElement('#quality-gate-new-condition-metric', 'complexity')
          .changeElement('#quality-gate-new-condition-metric')
          .checkElementCount('.js-conditions [name="error"]', 1)
          .fillElement('[name="period"]', '1')
          .fillElement('[name="operator"]', 'GT')
          .fillElement('[name="warning"]', '3')
          .fillElement('[name="error"]', '4')
          .clickElement('.add-condition')
          .checkElementExist('.update-condition')
          .checkElementExist('.update-condition[disabled]')
          .checkElementExist('.delete-condition');
    });

    bdd.it('should update condition', function () {
      return this.remote
          .open('#show/1')
          .mockFromFile('/api/qualitygates/app', 'quality-gates-spec/app.json')
          .mockFromFile('/api/qualitygates/list', 'quality-gates-spec/list.json')
          .mockFromFile('/api/qualitygates/show', 'quality-gates-spec/show.json', { data: { id: 1 } })
          .mockFromFile('/api/qualitygates/update_condition', 'quality-gates-spec/update-condition.json',
          { data: { id: 1, warning: '173' } })
          .startAppBrowserify('quality-gates', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .checkElementExist('.update-condition')
          .checkElementExist('.js-conditions tr:first-child .update-condition[disabled]')
          .fillElement('.js-conditions tr:first-child [name="warning"]', '173')
          .changeElement('.js-conditions tr:first-child [name="warning"]')
          .checkElementNotExist('.js-conditions tr:first-child .update-condition[disabled]')
          .clickElement('.js-conditions tr:first-child .update-condition')
          .checkElementNotExist('.js-conditions tr:first-child .update-condition:not([disabled])')
          .checkElementExist('.js-conditions tr:first-child .update-condition[disabled]');
    });

    bdd.it('should delete condition', function () {
      return this.remote
          .open('#show/1')
          .mockFromFile('/api/qualitygates/app', 'quality-gates-spec/app.json')
          .mockFromFile('/api/qualitygates/list', 'quality-gates-spec/list.json')
          .mockFromFile('/api/qualitygates/show', 'quality-gates-spec/show.json', { data: { id: 1 } })
          .mockFromString('/api/qualitygates/delete_condition', '{"errors":[{"msg": "error"}]}', { status: 400 })
          .startAppBrowserify('quality-gates', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .checkElementExist('.delete-condition')
          .checkElementCount('.delete-condition', 8)
          .clickElement('.js-conditions tr:first-child .delete-condition')
          .checkElementExist('#delete-condition-submit')
          .clickElement('#delete-condition-submit')
          .checkElementExist('.alert-danger')
          .clearMocks()
          .mockFromString('/api/qualitygates/delete_condition', '{}', { data: { id: 1 } })
          .clickElement('#delete-condition-submit')
          .checkElementCount('.delete-condition', 7);
    });
  });
});
