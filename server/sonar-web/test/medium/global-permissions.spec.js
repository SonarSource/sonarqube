define(function (require) {
  var bdd = require('intern!bdd');
  require('../helpers/test-page');

  bdd.describe('Global Permissions', function () {
    bdd.it('should show permissions', function () {
      return this.remote
          .open()
          .mockFromFile('/api/permissions/search_global_permissions', 'permissions/global-permissions.json')
          .mockFromFile('/api/permissions/users', 'permissions/users.json')
          .mockFromFile('/api/permissions/groups', 'permissions/groups.json')
          .startAppBrowserify('global-permissions')
          .checkElementExist('#global-permissions-header')
          .checkElementExist('#global-permissions-list')
          .checkElementCount('#global-permissions-list > li', 6)
          .checkElementInclude('#global-permissions-list > li h3', 'Administer System')
          .checkElementInclude('#global-permissions-list > li p', 'Ability to perform all administration')
          .checkElementInclude('#global-permissions-list > li ul > li:first-child', 'Administrator')
          .checkElementInclude('#global-permissions-list > li ul > li:last-child', '1');
    });
  });
});
