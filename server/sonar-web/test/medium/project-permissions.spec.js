define(function (require) {
  var bdd = require('intern!bdd');
  require('../helpers/test-page');

  bdd.describe('Project Permissions', function () {
    bdd.it('should show permissions', function () {
      return this.remote
          .open()
          .mockFromFile('/api/permissions/search_project_permissions', 'permissions/project-permissions.json')
          .startApp('project-permissions')
          .checkElementExist('#project-permissions-header')
          .checkElementExist('#projects')
          .checkElementCount('#projects > thead > tr > th', 3)
          .checkElementCount('#projects > tbody > tr', 2)
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(1)', 'My Project')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(2)', '3')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(2)', '4')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(3)', '1')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(3)', '2');
    });
  });
});
