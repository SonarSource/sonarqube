define(function (require) {
  var bdd = require('intern!bdd');
  require('../helpers/test-page');

  bdd.describe('Project Permissions', function () {
    bdd.it('should show permissions', function () {
      return this.remote
          .open()
          .mockFromFile('/api/permissions/search_project_permissions', 'permissions/project-permissions.json')
          .mockFromFile('/api/permissions/search_templates', 'permissions/permission-templates.json')
          .startAppBrowserify('project-permissions')
          .checkElementExist('#project-permissions-header')
          .checkElementExist('#projects')
          .checkElementCount('#projects > thead > tr > th', 4)
          .checkElementCount('#projects > tbody > tr', 2)
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(1)', 'My Project')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(2)', '3')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(2)', '4')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(3)', '1')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(3)', '2');
    });

    bdd.it('should apply a permission template', function () {
      return this.remote
          .open()
          .mockFromFile('/api/permissions/search_project_permissions', 'permissions/project-permissions.json')
          .mockFromFile('/api/permissions/search_templates', 'permissions/permission-templates.json')
          .startAppBrowserify('project-permissions')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(1)', 'My Project')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(2)', '3')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(2)', '4')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(3)', '1')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(3)', '2')
          .clearMocks()
          .mockFromFile('/api/permissions/search_project_permissions', 'permissions/project-permissions-changed.json')
          .mockFromString('/api/permissions/apply_template', '{}')
          .clickElement('#projects > tbody > tr:first-child .js-apply-template')
          .clickElement('#project-permissions-apply-template')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(2)', '13')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(2)', '14')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(3)', '11')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(3)', '12');
    });

    bdd.it('should bulk apply a permission template', function () {
      return this.remote
          .open()
          .mockFromFile('/api/permissions/search_project_permissions', 'permissions/project-permissions.json')
          .mockFromFile('/api/permissions/search_templates', 'permissions/permission-templates.json')
          .startAppBrowserify('project-permissions')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(1)', 'My Project')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(2)', '3')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(2)', '4')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(3)', '1')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(3)', '2')
          .clearMocks()
          .mockFromFile('/api/permissions/search_project_permissions', 'permissions/project-permissions-changed.json')
          .mockFromString('/api/permissions/apply_template', '{}')
          .clickElement('.js-bulk-apply-template')
          .clickElement('#project-permissions-apply-template')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(2)', '13')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(2)', '14')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(3)', '11')
          .checkElementInclude('#projects > tbody > tr:first-child td:nth-child(3)', '12');
    });
  });
});
