define(function (require) {
  var bdd = require('intern!bdd');
  require('../helpers/test-page');

  bdd.describe('Project Permissions', function () {
    bdd.it('should show permissions', function () {
      return this.remote
          .open()
          .mockFromFile('/api/permissions/search_project_permissions', 'permissions/project-permissions.json')
          .startApp('project-permissions/app', { component: null })
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

    bdd.it('should show permissions on the project page', function () {
      return this.remote
          .open()
          .mockFromFile('/api/permissions/search_project_permissions', 'permissions/project-permissions.json')
          .mockFromFile('/api/permissions/users', 'permissions/users.json')
          .mockFromFile('/api/permissions/groups', 'permissions/groups.json')
          .startApp('project-permissions/app')
          .checkElementExist('#project-permissions-header')
          .checkElementExist('#global-permissions-list')
          .checkElementCount('#global-permissions-list > li', 2)
          .checkElementInclude('#global-permissions-list > li h3', 'See Source Code')
          .checkElementInclude('#global-permissions-list > li p', 'Ability to view the project\'s source code.')
          .checkElementInclude('#global-permissions-list > li ul > li:first-child', 'Administrator')
          .checkElementInclude('#global-permissions-list > li ul > li:last-child', '1');
    });
  });
});
