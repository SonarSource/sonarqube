define(function (require) {
  var bdd = require('intern!bdd');
  require('../helpers/test-page');

  bdd.describe('Groups Page', function () {
    bdd.it('should show list', function () {
      return this.remote
          .open()
          .mockFromFile('/api/user_groups/search', 'groups-spec/search.json')
          .startAppBrowserify('groups')
          .checkElementInclude('#content', 'sonar-users')
          .checkElementExist('#groups-list ul')
          .checkElementCount('#groups-list li[data-id]', 2)
          .checkElementInclude('#groups-list .js-group-name', 'sonar-users')
          .checkElementInclude('#groups-list .js-group-description',
          'Any new users created will automatically join this group')
          .checkElementCount('#groups-list .js-group-update', 2)
          .checkElementCount('#groups-list .js-group-delete', 2)
          .checkElementInclude('#groups-list-footer', '2/2');
    });

    bdd.it('should search', function () {
      return this.remote
          .open()
          .mockFromFile('/api/user_groups/search', 'groups-spec/search.json')
          .startAppBrowserify('groups')
          .checkElementInclude('#content', 'sonar-users')
          .checkElementCount('#groups-list li[data-id]', 2)
          .clearMocks()
          .mockFromFile('/api/user_groups/search', 'groups-spec/search-filtered.json', { data: { q: 'adm' } })
          .fillElement('#groups-search-query', 'adm')
          .clickElement('#groups-search-submit')
          .checkElementInclude('#groups-list-footer', '1/1')
          .checkElementCount('#groups-list li[data-id]', 1)
          .clearMocks()
          .mockFromFile('/api/user_groups/search', 'groups-spec/search.json')
          .fillElement('#groups-search-query', '')
          .clickElement('#groups-search-submit')
          .checkElementInclude('#groups-list-footer', '2/2')
          .checkElementCount('#groups-list li[data-id]', 2);
    });

    bdd.it('should show more', function () {
      return this.remote
          .open()
          .mockFromFile('/api/user_groups/search', 'groups-spec/search-big-1.json')
          .startAppBrowserify('groups')
          .checkElementInclude('#content', 'sonar-users')
          .checkElementCount('#groups-list li[data-id]', 1)
          .checkElementInclude('#groups-list-footer', '1/2')
          .clearMocks()
          .mockFromFile('/api/user_groups/search', 'groups-spec/search-big-2.json', { data: { p: 2 } })
          .clickElement('#groups-fetch-more')
          .checkElementInclude('#groups-list-footer', '2/2')
          .checkElementCount('#groups-list li[data-id]', 2);
    });

    bdd.it('should show users', function () {
      return this.remote
          .open()
          .mockFromFile('/api/user_groups/search', 'groups-spec/search.json')
          .mockFromFile('/api/user_groups/users*', 'groups-spec/users.json')
          .startAppBrowserify('groups')
          .checkElementInclude('#content', 'sonar-users')
          .checkElementNotInclude('#content', 'Bob')
          .clickElement('[data-id="1"] .js-group-users')
          .checkElementInclude('#groups-users', 'Bob')
          .checkElementInclude('#groups-users', 'John');
    });

    bdd.it('should create new group', function () {
      return this.remote
          .open()
          .mockFromFile('/api/user_groups/search', 'groups-spec/search.json')
          .mockFromFile('/api/user_groups/create', 'groups-spec/error.json', { status: 400 })
          .startAppBrowserify('groups')
          .checkElementInclude('#content', 'sonar-users')
          .checkElementCount('#groups-list li[data-id]', 2)
          .clickElement('#groups-create')
          .checkElementExist('#create-group-form')
          .fillElement('#create-group-name', 'name')
          .fillElement('#create-group-description', 'description')
          .clickElement('#create-group-submit')
          .checkElementExist('.alert.alert-danger')
          .clearMocks()
          .mockFromFile('/api/user_groups/search', 'groups-spec/search-created.json')
          .mockFromString('/api/user_groups/create', '{}', { data: { name: 'name', description: 'description' } })
          .fillElement('#create-group-name', 'name')
          .fillElement('#create-group-description', 'description')
          .clickElement('#create-group-submit')
          .checkElementCount('#groups-list li[data-id]', 3)
          .checkElementInclude('#groups-list .js-group-name', 'name')
          .checkElementInclude('#groups-list .js-group-description', 'description');
    });

    bdd.it('should update group', function () {
      return this.remote
          .open()
          .mockFromFile('/api/user_groups/search', 'groups-spec/search.json')
          .mockFromFile('/api/user_groups/update', 'groups-spec/error.json', { status: 400 })
          .startAppBrowserify('groups')
          .checkElementInclude('#content', 'sonar-users')
          .clickElement('[data-id="2"] .js-group-update')
          .checkElementExist('#create-group-form')
          .fillElement('#create-group-name', 'guys')
          .fillElement('#create-group-description', 'cool guys')
          .clickElement('#create-group-submit')
          .checkElementExist('.alert.alert-danger')
          .clearMocks()
          .mockFromFile('/api/user_groups/search', 'groups-spec/search-updated.json')
          .mockFromString('/api/user_groups/update', '{}', { data: { id: '2' } })
          .fillElement('#create-group-name', 'guys')
          .fillElement('#create-group-description', 'cool guys')
          .clickElement('#create-group-submit')
          .checkElementInclude('[data-id="2"] .js-group-name', 'guys')
          .checkElementInclude('[data-id="2"] .js-group-description', 'cool guys');
    });

    bdd.it('should delete group', function () {
      return this.remote
          .open()
          .mockFromFile('/api/user_groups/search', 'groups-spec/search.json')
          .mockFromFile('/api/user_groups/delete', 'groups-spec/error.json', { status: 400 })
          .startAppBrowserify('groups')
          .checkElementInclude('#content', 'sonar-users')
          .clickElement('[data-id="1"] .js-group-delete')
          .checkElementExist('#delete-group-form')
          .clickElement('#delete-group-submit')
          .checkElementExist('.alert.alert-danger')
          .clickElement('.js-modal-close')
          .checkElementNotExist('#delete-group-form')
          .clickElement('[data-id="1"] .js-group-delete')
          .checkElementExist('#delete-group-form')
          .clearMocks()
          .mockFromString('/api/user_groups/delete', '{}', { data: { id: '1' } })
          .clickElement('#delete-group-submit')
          .checkElementNotExist('[data-id="1"]');
    });
  });
});
