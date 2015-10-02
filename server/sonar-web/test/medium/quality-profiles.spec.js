define(function (require) {
  var bdd = require('intern!bdd');
  require('../helpers/test-page');

  bdd.describe('Quality Profiles Page', function () {
    bdd.it('should show list', function () {
      return this.remote
          .open()
          .mockFromFile('/api/users/current', 'quality-profiles/user.json')
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search.json')
          .mockFromFile('/api/qualityprofiles/exporters', 'quality-profiles/exporters.json')
          .mockFromFile('/api/languages/list', 'quality-profiles/languages.json')
          .startAppBrowserify('quality-profiles', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .checkElementCount('.js-list .list-group-item', 5)
          .checkElementInclude('.js-list .list-group-item', 'Sonar way')
          .checkElementInclude('.js-list .list-group-item', 'PSR-2')
          .checkElementCount('.js-list-language', 4)
          .checkElementInclude('.js-list-language', 'Java')
          .checkElementInclude('.js-list-language', 'JavaScript')
          .checkElementInclude('.js-list-language', 'PHP')
          .checkElementInclude('.js-list-language', 'Python')
          .checkElementCount('.js-list .badge', 4);
    });

    bdd.it('should filter list by language', function () {
      return this.remote
          .open()
          .mockFromFile('/api/users/current', 'quality-profiles/user.json')
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search.json')
          .mockFromFile('/api/qualityprofiles/exporters', 'quality-profiles/exporters.json')
          .mockFromFile('/api/languages/list', 'quality-profiles/languages.json')
          .startAppBrowserify('quality-profiles', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .checkElementCount('.js-list .list-group-item', 5)
          .checkElementExist('.js-list .list-group-item[data-key="java-sonar-way-67887"]:not(.hidden)')
          .checkElementExist('.js-list .list-group-item[data-key="js-sonar-way-71566"]:not(.hidden)')
          .checkElementCount('.js-list-language', 4)
          .checkElementExist('.js-list-language[data-language="java"]:not(.hidden)')
          .checkElementExist('.js-list-language[data-language="js"]:not(.hidden)')
          .checkElementExist('#quality-profiles-filter-by-language')
          .clickElement('#quality-profiles-filter-by-language .dropdown-toggle')
          .clickElement('.js-filter-by-language[data-language="js"]')
          .checkElementNotExist('.js-list .list-group-item[data-key="java-sonar-way-67887"]:not(.hidden)')
          .checkElementExist('.js-list .list-group-item[data-key="js-sonar-way-71566"]:not(.hidden)')
          .checkElementNotExist('.js-list-language[data-language="java"]:not(.hidden)')
          .checkElementExist('.js-list-language[data-language="js"]:not(.hidden)')
          .clickElement('#quality-profiles-filter-by-language .dropdown-toggle')
          .clickElement('.js-filter-by-language:nth-child(1)')
          .checkElementExist('.js-list .list-group-item[data-key="java-sonar-way-67887"]:not(.hidden)')
          .checkElementExist('.js-list .list-group-item[data-key="js-sonar-way-71566"]:not(.hidden)')
          .checkElementExist('.js-list-language[data-language="java"]:not(.hidden)')
          .checkElementExist('.js-list-language[data-language="js"]:not(.hidden)');
    });

    bdd.it('should show details', function () {
      return this.remote
          .open()
          .mockFromFile('/api/users/current', 'quality-profiles/user.json')
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search.json')
          .mockFromFile('/api/qualityprofiles/exporters', 'quality-profiles/exporters.json')
          .mockFromFile('/api/languages/list', 'quality-profiles/languages.json')
          .mockFromFile('/api/rules/search', 'quality-profiles/rules.json',
          { data: { qprofile: 'java-sonar-way-67887', activation: 'true' } })
          .mockFromFile('/api/qualityprofiles/inheritance', 'quality-profiles/inheritance.json',
          { data: { profileKey: 'java-sonar-way-67887' } })
          .startAppBrowserify('quality-profiles', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .clickElement('.js-list .list-group-item[data-key="java-sonar-way-67887"]')
          .checkElementExist('.search-navigator-header-component')
          .checkElementCount('.js-list .list-group-item.active', 1)
          .checkElementInclude('.js-list .list-group-item.active', 'Sonar way')
          .checkElementInclude('.search-navigator-workspace-header', 'Sonar way')
          .checkElementInclude('.search-navigator-workspace-header', 'Java')
          .checkElementExist('#quality-profile-backup')
          .checkElementNotExist('#quality-profile-rename')
          .checkElementNotExist('#quality-profile-copy')
          .checkElementNotExist('#quality-profile-delete')
          .checkElementNotExist('#quality-profile-set-as-default')
          .checkElementNotExist('#quality-profile-change-parent');
    });

    bdd.it('should show details for admin', function () {
      return this.remote
          .open()
          .mockFromFile('/api/users/current', 'quality-profiles/user-admin.json')
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search.json')
          .mockFromFile('/api/qualityprofiles/exporters', 'quality-profiles/exporters.json')
          .mockFromFile('/api/languages/list', 'quality-profiles/languages.json')
          .mockFromFile('/api/rules/search', 'quality-profiles/rules.json',
          { data: { qprofile: 'java-sonar-way-67887', activation: 'true' } })
          .mockFromFile('/api/qualityprofiles/inheritance', 'quality-profiles/inheritance.json',
          { data: { profileKey: 'java-sonar-way-67887' } })
          .startAppBrowserify('quality-profiles', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .clickElement('.js-list .list-group-item[data-key="java-sonar-way-67887"]')
          .checkElementExist('.search-navigator-header-component')
          .checkElementCount('.js-list .list-group-item.active', 1)
          .checkElementInclude('.js-list .list-group-item.active', 'Sonar way')
          .checkElementInclude('.search-navigator-workspace-header', 'Sonar way')
          .checkElementInclude('.search-navigator-workspace-header', 'Java')
          .checkElementExist('#quality-profile-backup')
          .checkElementExist('#quality-profile-rename')
          .checkElementExist('#quality-profile-copy')
          .checkElementNotExist('#quality-profile-delete')
          .checkElementNotExist('#quality-profile-set-as-default')
          .checkElementExist('#quality-profile-change-parent');
    });

    bdd.it('should show inheritance details', function () {
      return this.remote
          .open()
          .mockFromFile('/api/users/current', 'quality-profiles/user-admin.json')
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search-inheritance.json')
          .mockFromFile('/api/qualityprofiles/exporters', 'quality-profiles/exporters.json')
          .mockFromFile('/api/languages/list', 'quality-profiles/languages.json')
          .mockFromFile('/api/rules/search', 'quality-profiles/rules.json')
          .mockFromFile('/api/qualityprofiles/inheritance', 'quality-profiles/inheritance-plus.json', {
            data: { profileKey: 'java-inherited-profile-85155' }
          })
          .startAppBrowserify('quality-profiles', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .clickElement('.js-list .list-group-item[data-key="java-inherited-profile-85155"]')
          .checkElementExist('.search-navigator-header-component')
          .checkElementCount('#quality-profile-ancestors li', 1)
          .checkElementInclude('#quality-profile-ancestors', 'Sonar way')
          .checkElementInclude('#quality-profile-ancestors', '161')
          .checkElementCount('#quality-profile-inheritance-current', 1)
          .checkElementInclude('#quality-profile-inheritance-current', 'Inherited Profile')
          .checkElementInclude('#quality-profile-inheritance-current', '163')
          .checkElementInclude('#quality-profile-inheritance-current', '7')
          .checkElementCount('#quality-profile-children li', 1)
          .checkElementInclude('#quality-profile-children', 'Second Level Inherited Profile')
          .checkElementInclude('#quality-profile-children', '165');
    });

    bdd.it('should show selected projects', function () {
      return this.remote
          .open()
          .mockFromFile('/api/users/current', 'quality-profiles/user-admin.json')
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search.json')
          .mockFromFile('/api/qualityprofiles/exporters', 'quality-profiles/exporters.json')
          .mockFromFile('/api/languages/list', 'quality-profiles/languages.json')
          .mockFromFile('/api/rules/search', 'quality-profiles/rules.json')
          .mockFromFile('/api/qualityprofiles/projects?key=php-psr-2-46772', 'quality-profiles/projects.json')
          .mockFromFile('/api/qualityprofiles/inheritance', 'quality-profiles/inheritance.json')
          .startAppBrowserify('quality-profiles', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .clickElement('.js-list .list-group-item[data-key="php-psr-2-46772"]')
          .checkElementExist('#quality-profile-projects')
          .checkElementCount('#quality-profile-projects .select-list-list li', 2)
          .checkElementInclude('#quality-profile-projects .select-list-list li', 'CSS')
          .checkElementInclude('#quality-profile-projects .select-list-list li', 'http-request-parent');
    });

    bdd.it('should move between profiles', function () {
      return this.remote
          .open()
          .mockFromFile('/api/users/current', 'quality-profiles/user.json')
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search-inheritance.json')
          .mockFromFile('/api/qualityprofiles/exporters', 'quality-profiles/exporters.json')
          .mockFromFile('/api/languages/list', 'quality-profiles/languages.json')
          .mockFromFile('/api/rules/search', 'quality-profiles/rules.json',
          { data: { qprofile: 'java-inherited-profile-85155', activation: 'true' } })
          .mockFromFile('/api/qualityprofiles/inheritance', 'quality-profiles/inheritance-plus.json')
          .startAppBrowserify('quality-profiles', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .clickElement('.js-list .list-group-item[data-key="java-inherited-profile-85155"]')
          .checkElementExist('#quality-profile-ancestors')
          .clearMocks()
          .clearMocks()
          .mockFromFile('/api/rules/search', 'quality-profiles/rules.json',
          { data: { qprofile: 'java-sonar-way-67887', activation: 'true' } })
          .mockFromFile('/api/qualityprofiles/inheritance', 'quality-profiles/inheritance.json')
          .clickElement('#quality-profile-ancestors .js-profile[data-key="java-sonar-way-67887"]')
          .checkElementInclude('.search-navigator-header-component', 'Sonar way');
    });

    bdd.it('should copy profile', function () {
      return this.remote
          .open()
          .mockFromFile('/api/users/current', 'quality-profiles/user-admin.json')
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search.json')
          .mockFromFile('/api/qualityprofiles/exporters', 'quality-profiles/exporters.json')
          .mockFromFile('/api/languages/list', 'quality-profiles/languages.json')
          .mockFromFile('/api/rules/search', 'quality-profiles/rules.json')
          .mockFromFile('/api/qualityprofiles/inheritance', 'quality-profiles/inheritance.json')
          .mockFromFile('/api/qualityprofiles/copy', 'quality-profiles/copy.json', {
            data: { fromKey: 'java-sonar-way-67887', toName: 'Copied Profile' }
          })
          .startAppBrowserify('quality-profiles', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .checkElementCount('.js-list .list-group-item', 5)
          .clickElement('.js-list .list-group-item[data-key="java-sonar-way-67887"]')
          .checkElementExist('#quality-profile-copy')
          .clickElement('#quality-profile-copy')
          .checkElementExist('.modal')
          .fillElement('#copy-profile-name', 'Copied Profile')
          .clickElement('#copy-profile-submit')
          .checkElementInclude('.search-navigator-header-component', 'Copied Profile')
          .checkElementInclude('.js-list .list-group-item.active', 'Copied Profile')
          .checkElementInclude('.search-navigator-header-component', 'Java')
          .checkElementCount('.js-list .list-group-item', 6);
    });

    bdd.it('should rename profile', function () {
      return this.remote
          .open()
          .mockFromFile('/api/users/current', 'quality-profiles/user-admin.json')
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search.json')
          .mockFromFile('/api/qualityprofiles/exporters', 'quality-profiles/exporters.json')
          .mockFromFile('/api/languages/list', 'quality-profiles/languages.json')
          .mockFromFile('/api/rules/search', 'quality-profiles/rules.json')
          .mockFromFile('/api/qualityprofiles/inheritance', 'quality-profiles/inheritance.json')
          .startAppBrowserify('quality-profiles', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .clickElement('.js-list .list-group-item[data-key="java-sonar-way-67887"]')
          .checkElementExist('#quality-profile-rename')
          .clickElement('#quality-profile-rename')
          .checkElementExist('.modal')
          .clearMocks()
          .mockFromString('/api/qualityprofiles/rename', '{}', {
            data: { key: 'java-sonar-way-67887', name: 'Renamed Profile' }
          })
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search-renamed.json')
          .fillElement('#rename-profile-name', 'Renamed Profile')
          .clickElement('#rename-profile-submit')
          .checkElementInclude('.js-list .list-group-item.active', 'Renamed Profile')
          .checkElementInclude('.search-navigator-header-component', 'Renamed Profile');
    });

    bdd.it('should make profile default', function () {
      return this.remote
          .open()
          .mockFromFile('/api/users/current', 'quality-profiles/user-admin.json')
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search.json')
          .mockFromFile('/api/qualityprofiles/exporters', 'quality-profiles/exporters.json')
          .mockFromFile('/api/languages/list', 'quality-profiles/languages.json')
          .mockFromFile('/api/rules/search', 'quality-profiles/rules.json')
          .mockFromFile('/api/qualityprofiles/inheritance', 'quality-profiles/inheritance.json')
          .startAppBrowserify('quality-profiles', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .checkElementNotExist('.js-list .list-group-item[data-key="php-psr-2-46772"] .badge')
          .checkElementExist('.js-list .list-group-item[data-key="php-sonar-way-10778"] .badge')
          .clickElement('.js-list .list-group-item[data-key="php-psr-2-46772"]')
          .checkElementExist('#quality-profile-set-as-default')
          .clearMocks(this.searchMock)
          .mockFromString('/api/qualityprofiles/set_default', '{}', {
            data: { profileKey: 'php-psr-2-46772' }
          })
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search-another-default.json')
          .clickElement('#quality-profile-set-as-default')
          .checkElementNotExist('.js-list .list-group-item[data-key="php-sonar-way-10778"] .badge')
          .checkElementNotExist('#quality-profile-set-as-default')
          .checkElementExist('.js-list .list-group-item[data-key="php-psr-2-46772"] .badge');
    });

    bdd.it('should delete profile', function () {
      return this.remote
          .open()
          .mockFromFile('/api/users/current', 'quality-profiles/user-admin.json')
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search-with-copy.json')
          .mockFromFile('/api/qualityprofiles/exporters', 'quality-profiles/exporters.json')
          .mockFromFile('/api/languages/list', 'quality-profiles/languages.json')
          .mockFromFile('/api/rules/search', 'quality-profiles/rules.json')
          .mockFromFile('/api/qualityprofiles/inheritance', 'quality-profiles/inheritance.json')
          .startAppBrowserify('quality-profiles', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .checkElementCount('.js-list .list-group-item', 6)
          .clickElement('.js-list .list-group-item[data-key="java-copied-profile-11711"]')
          .checkElementExist('#quality-profile-delete')
          .clickElement('#quality-profile-delete')
          .checkElementExist('.modal')
          .clearMocks()
          .mockFromString('/api/qualityprofiles/delete', '{}', {
            data: { profileKey: 'java-copied-profile-11711' }
          })
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search.json')
          .clickElement('#delete-profile-submit')
          .checkElementCount('.js-list .list-group-item', 5)
          .checkElementNotInclude('.js-list .list-group-item', 'Copied Profile');
    });

    bdd.it('should create profile', function () {
      return this.remote
          .open()
          .mockFromFile('/api/users/current', 'quality-profiles/user-admin.json')
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search.json')
          .mockFromFile('/api/qualityprofiles/exporters', 'quality-profiles/exporters.json')
          .mockFromFile('/api/rules/search', 'quality-profiles/rules.json')
          .mockFromFile('/api/qualityprofiles/inheritance', 'quality-profiles/inheritance.json')
          .mockFromFile('/api/languages/list', 'quality-profiles/languages.json')
          .mockFromFile('/api/qualityprofiles/importers', 'quality-profiles/importers-empty.json')
          .startAppBrowserify('quality-profiles', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .checkElementCount('.js-list .list-group-item', 5)
          .clickElement('#quality-profiles-create')
          .checkElementExist('.modal')
          .checkElementExist('#create-profile-name')
          .checkElementExist('#create-profile-language');
    });

    bdd.it('should restore profile', function () {
      return this.remote
          .open()
          .mockFromFile('/api/users/current', 'quality-profiles/user-admin.json')
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search.json')
          .mockFromFile('/api/qualityprofiles/exporters', 'quality-profiles/exporters.json')
          .mockFromFile('/api/languages/list', 'quality-profiles/languages.json')
          .mockFromFile('/api/rules/search', 'quality-profiles/rules.json')
          .mockFromFile('/api/qualityprofiles/inheritance', 'quality-profiles/inheritance.json')
          .startAppBrowserify('quality-profiles', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .checkElementCount('.js-list .list-group-item', 5)
          .clickElement('#quality-profiles-actions')
          .clickElement('#quality-profiles-restore')
          .checkElementExist('.modal')
          .checkElementExist('.modal input[type="file"]');
    });

    bdd.it('should show importers', function () {
      return this.remote
          .open()
          .mockFromFile('/api/users/current', 'quality-profiles/user-admin.json')
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search.json')
          .mockFromFile('/api/qualityprofiles/exporters', 'quality-profiles/exporters.json')
          .mockFromFile('/api/rules/search', 'quality-profiles/rules.json')
          .mockFromFile('/api/qualityprofiles/inheritance', 'quality-profiles/inheritance.json')
          .mockFromFile('/api/languages/list', 'quality-profiles/languages.json')
          .mockFromFile('/api/qualityprofiles/importers', 'quality-profiles/importers.json')
          .startAppBrowserify('quality-profiles', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .clickElement('#quality-profiles-create')
          .checkElementExist('.modal')
          .checkElementExist('.js-importer[data-key="pmd"]:not(.hidden)')
          .checkElementExist('.js-importer[data-key="random"]:not(.hidden)')
          .fillElement('#create-profile-language', 'js')
          .changeElement('#create-profile-language')
          .checkElementNotExist('.js-importer[data-key="pmd"]:not(.hidden)')
          .checkElementExist('.js-importer[data-key="random"]:not(.hidden)')
          .fillElement('#create-profile-language', 'py')
          .changeElement('#create-profile-language')
          .checkElementNotExist('.js-importer[data-key="pmd"]:not(.hidden)')
          .checkElementNotExist('.js-importer[data-key="random"]:not(.hidden)');
    });

    bdd.it('should restore built-in profiles', function () {
      return this.remote
          .open()
          .mockFromFile('/api/users/current', 'quality-profiles/user-admin.json')
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search-modified.json')
          .mockFromFile('/api/qualityprofiles/exporters', 'quality-profiles/exporters.json')
          .mockFromFile('/api/rules/search', 'quality-profiles/rules.json')
          .mockFromFile('/api/qualityprofiles/inheritance', 'quality-profiles/inheritance.json')
          .mockFromFile('/api/languages/list', 'quality-profiles/languages.json')
          .startAppBrowserify('quality-profiles', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .checkElementCount('.js-list .js-list-language', 1)
          .checkElementCount('.js-list .list-group-item', 1)
          .clickElement('#quality-profiles-actions')
          .clickElement('#quality-profiles-restore-built-in')
          .checkElementExist('.modal')
          .clearMocks()
          .mockFromString('/api/qualityprofiles/restore_built_in', '{}', { data: { language: 'java' } })
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search.json')
          .fillElement('#restore-built-in-profiles-language', 'java')
          .clickElement('#restore-built-in-profiles-submit')
          .checkElementCount('.js-list .js-list-language', 4)
          .checkElementCount('.js-list .list-group-item', 5)
          .checkElementInclude('.js-list .list-group-item', 'Sonar way')
          .checkElementNotExist('.search-navigator-header-component');
    });

    bdd.it('should change profile\'s parent', function () {
      return this.remote
          .open()
          .mockFromFile('/api/users/current', 'quality-profiles/user-admin.json')
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search-change-parent.json')
          .mockFromFile('/api/qualityprofiles/exporters', 'quality-profiles/exporters.json')
          .mockFromFile('/api/languages/list', 'quality-profiles/languages.json')
          .mockFromFile('/api/rules/search', 'quality-profiles/rules.json')
          .mockFromFile('/api/qualityprofiles/inheritance', 'quality-profiles/inheritance-change-parent.json')
          .startAppBrowserify('quality-profiles', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-list .list-group-item')
          .clickElement('.js-list .list-group-item[data-key="java-inherited-profile-85155"]')
          .checkElementExist('#quality-profile-change-parent')
          .clickElement('#quality-profile-change-parent')
          .checkElementExist('.modal')
          .clearMocks()
          .mockFromString('/api/qualityprofiles/change_parent', '{}', {
            data: { profileKey: 'java-inherited-profile-85155', parentKey: 'java-another-profile-00609' }
          })
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search-changed-parent.json')
          .mockFromFile('/api/qualityprofiles/inheritance', 'quality-profiles/inheritance-changed-parent.json')
          .mockFromFile('/api/qualityprofiles/exporters', 'quality-profiles/exporters.json')
          .mockFromFile('/api/languages/list', 'quality-profiles/languages.json')
          .mockFromFile('/api/rules/search', 'quality-profiles/rules.json')
          .fillElement('#change-profile-parent', 'java-another-profile-00609')
          .clickElement('#change-profile-parent-submit')
          .checkElementInclude('#quality-profile-ancestors', 'Another Profile');
    });

    bdd.it('should open permalink', function () {
      return this.remote
          .open('#show?key=java-sonar-way-67887')
          .mockFromFile('/api/users/current', 'quality-profiles/user-admin.json')
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search.json')
          .mockFromFile('/api/qualityprofiles/exporters', 'quality-profiles/exporters.json')
          .mockFromFile('/api/languages/list', 'quality-profiles/languages.json')
          .mockFromFile('/api/rules/search', 'quality-profiles/rules.json')
          .mockFromFile('/api/qualityprofiles/inheritance', 'quality-profiles/inheritance.json')
          .startAppBrowserify('quality-profiles', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('#quality-profile-rename')
          .checkElementCount('.js-list .list-group-item.active', 1)
          .checkElementInclude('.js-list .list-group-item.active', 'Sonar way')
          .checkElementInclude('.search-navigator-workspace-header', 'Sonar way')
          .checkElementInclude('.search-navigator-workspace-header', 'Java')
          .checkElementExist('#quality-profile-backup')
          .checkElementExist('#quality-profile-rename')
          .checkElementExist('#quality-profile-copy')
          .checkElementNotExist('#quality-profile-delete')
          .checkElementNotExist('#quality-profile-set-as-default');
    });

    bdd.it('should show changelog', function () {
      return this.remote
          .open('#show?key=java-sonar-way-67887')
          .mockFromFile('/api/users/current', 'quality-profiles/user.json')
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search.json')
          .mockFromFile('/api/qualityprofiles/exporters', 'quality-profiles/exporters.json')
          .mockFromFile('/api/languages/list', 'quality-profiles/languages.json')
          .mockFromFile('/api/rules/search', 'quality-profiles/rules.json')
          .mockFromFile('/api/qualityprofiles/inheritance', 'quality-profiles/inheritance.json')
          .mockFromFile('/api/qualityprofiles/changelog', 'quality-profiles/changelog.json', {
            data: { profileKey: 'java-sonar-way-67887' }
          })
          .startAppBrowserify('quality-profiles', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('#quality-profile-changelog-form-submit')
          .checkElementNotExist('.js-show-more-changelog')
          .clickElement('#quality-profile-changelog-form-submit')
          .checkElementExist('#quality-profile-changelog table')
          .checkElementExist('.js-show-more-changelog')
          .checkElementCount('#quality-profile-changelog tbody tr', 2)
          .checkElementInclude('#quality-profile-changelog tbody tr:nth-child(1)', 'April 13 2015')
          .checkElementInclude('#quality-profile-changelog tbody tr:nth-child(1)', 'System')
          .checkElementInclude('#quality-profile-changelog tbody tr:nth-child(1)', 'ACTIVATED')
          .checkElementInclude('#quality-profile-changelog tbody tr:nth-child(1)', 'Synchronisation should not')
          .checkElementInclude('#quality-profile-changelog tbody tr:nth-child(1)', 'BLOCKER')
          .checkElementInclude('#quality-profile-changelog tbody tr:nth-child(2)', 'April 13 2015')
          .checkElementInclude('#quality-profile-changelog tbody tr:nth-child(2)', 'Anakin Skywalker')
          .checkElementInclude('#quality-profile-changelog tbody tr:nth-child(2)', 'ACTIVATED')
          .checkElementInclude('#quality-profile-changelog tbody tr:nth-child(2)', 'Double.longBitsToDouble')
          .checkElementInclude('#quality-profile-changelog tbody tr:nth-child(2)', 'threshold')
          .checkElementInclude('#quality-profile-changelog tbody tr:nth-child(2)', '3')
          .checkElementInclude('#quality-profile-changelog tbody tr:nth-child(2)', 'emptyParameter')
          .clearMocks()
          .mockFromFile('/api/qualityprofiles/changelog', 'quality-profiles/changelog2.json', {
            data: { profileKey: 'java-sonar-way-67887' }
          })
          .clickElement('.js-show-more-changelog')
          .checkElementCount('#quality-profile-changelog tbody tr', 3)
          .checkElementNotExist('.js-show-changelog')
          .checkElementNotExist('.js-show-more-changelog')
          .checkElementInclude('#quality-profile-changelog tbody tr:nth-child(3)', 'April 13 2015')
          .checkElementInclude('#quality-profile-changelog tbody tr:nth-child(3)', 'System')
          .checkElementInclude('#quality-profile-changelog tbody tr:nth-child(3)', 'DEACTIVATED')
          .checkElementInclude('#quality-profile-changelog tbody tr:nth-child(3)', 'runFinalizersOnExit')
          .clickElement('.js-hide-changelog')
          .checkElementNotExist('#quality-profile-changelog tbody tr');
    });

    bdd.it('should open changelog permalink', function () {
      return this.remote
          .open('#changelog?since=2015-03-25&key=java-sonar-way-67887&to=2015-03-26')
          .mockFromFile('/api/users/current', 'quality-profiles/user.json')
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search.json')
          .mockFromFile('/api/qualityprofiles/exporters', 'quality-profiles/exporters.json')
          .mockFromFile('/api/languages/list', 'quality-profiles/languages.json')
          .mockFromFile('/api/rules/search', 'quality-profiles/rules.json')
          .mockFromFile('/api/qualityprofiles/inheritance', 'quality-profiles/inheritance.json')
          .mockFromFile('/api/qualityprofiles/changelog', 'quality-profiles/changelog.json', {
            data: {
              since: '2015-03-25',
              to: '2015-03-26',
              profileKey: 'java-sonar-way-67887'
            }
          })
          .startAppBrowserify('quality-profiles', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-show-more-changelog')
          .checkElementCount('#quality-profile-changelog tbody tr', 2)
          .clearMocks()
          .mockFromFile('/api/qualityprofiles/changelog', 'quality-profiles/changelog2.json', {
            data: {
              since: '2015-03-25',
              to: '2015-03-26',
              profileKey: 'java-sonar-way-67887'
            }
          })
          .clickElement('.js-show-more-changelog')
          .checkElementCount('#quality-profile-changelog tbody tr', 3)
          .checkElementNotExist('.js-show-more-changelog');
    });

    bdd.it('should show comparison', function () {
      return this.remote
          .open('#show?key=java-sonar-way-67887')
          .mockFromFile('/api/users/current', 'quality-profiles/user.json')
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search-with-copy.json')
          .mockFromFile('/api/qualityprofiles/exporters', 'quality-profiles/exporters.json')
          .mockFromFile('/api/languages/list', 'quality-profiles/languages.json')
          .mockFromFile('/api/rules/search', 'quality-profiles/rules.json')
          .mockFromFile('/api/qualityprofiles/inheritance', 'quality-profiles/inheritance.json')
          .mockFromFile('/api/qualityprofiles/compare', 'quality-profiles/compare.json', {
            data: { leftKey: 'java-sonar-way-67887', rightKey: 'java-copied-profile-11711' }
          })
          .startAppBrowserify('quality-profiles', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('#quality-profile-comparison-form-submit')
          .checkElementCount('#quality-profile-comparison-with-key option', 1)
          .clickElement('#quality-profile-comparison-form-submit')
          .checkElementExist('#quality-profile-comparison table')
          .checkElementCount('.js-comparison-in-left', 2)
          .checkElementCount('.js-comparison-in-right', 2)
          .checkElementCount('.js-comparison-modified', 2)
          .checkElementInclude('.js-comparison-in-left', '".equals()" should not be used to test')
          .checkElementInclude('.js-comparison-in-left', '"@Override" annotation should be used on')
          .checkElementInclude('.js-comparison-in-right', '"ConcurrentLinkedQueue.size()" should not be used')
          .checkElementInclude('.js-comparison-in-right', '"compareTo" results should not be checked')
          .checkElementInclude('.js-comparison-modified', 'Control flow statements')
          .checkElementInclude('.js-comparison-modified', '"Cloneables" should implement "clone"')
          .checkElementInclude('.js-comparison-modified', 'max: 5')
          .checkElementInclude('.js-comparison-modified', 'max: 3');
    });

    bdd.it('should open comparison permalink', function () {
      return this.remote
          .open('#compare?key=java-sonar-way-67887&withKey=java-copied-profile-11711')
          .mockFromFile('/api/users/current', 'quality-profiles/user.json')
          .mockFromFile('/api/qualityprofiles/search', 'quality-profiles/search-with-copy.json')
          .mockFromFile('/api/qualityprofiles/exporters', 'quality-profiles/exporters.json')
          .mockFromFile('/api/languages/list', 'quality-profiles/languages.json')
          .mockFromFile('/api/rules/search', 'quality-profiles/rules.json')
          .mockFromFile('/api/qualityprofiles/inheritance', 'quality-profiles/inheritance.json')
          .mockFromFile('/api/qualityprofiles/compare', 'quality-profiles/compare.json', {
            data: { leftKey: 'java-sonar-way-67887', rightKey: 'java-copied-profile-11711' }
          })
          .startAppBrowserify('quality-profiles', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('#quality-profile-comparison table')
          .checkElementCount('#quality-profile-comparison-with-key option', 1)
          .checkElementCount('.js-comparison-in-left', 2)
          .checkElementCount('.js-comparison-in-right', 2)
          .checkElementCount('.js-comparison-modified', 2);

    });
  });

});
