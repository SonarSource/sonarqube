define(function (require) {
  var bdd = require('intern!bdd');
  require('../helpers/test-page');

  bdd.describe('Update Center Page', function () {
    bdd.it('should show plugin card', function () {
      return this.remote
          .open()
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/plugins/installed', 'update-center-spec/installed.json')
          .mockFromFile('/api/plugins/updates', 'update-center-spec/updates.json')
          .mockFromFile('/api/plugins/pending', 'update-center-spec/pending.json')
          .startAppBrowserify('update-center', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-plugin-name')
          .checkElementCount('li[data-id]', 5)
          .checkElementInclude('li[data-id="scmgit"] .js-plugin-name', 'Git')
          .checkElementInclude('li[data-id="scmgit"] .js-plugin-category', 'Integration')
          .checkElementInclude('li[data-id="scmgit"] .js-plugin-description', 'Git SCM Provider.')
          .checkElementInclude('li[data-id="scmgit"] .js-plugin-installed-version', '1.0')
          .checkElementCount('li[data-id="scmgit"] .js-update-version', 1)
          .checkElementInclude('li[data-id="scmgit"] .js-update-version', '1.1')
          .checkElementCount('li[data-id="scmgit"] .js-changelog', 1)
          .checkElementCount('li[data-id="scmgit"] .js-plugin-homepage', 1)
          .checkElementCount('li[data-id="scmgit"] .js-plugin-issues', 1)
          .checkElementNotExist('li[data-id="scmgit"] .js-plugin-terms')
          .checkElementInclude('li[data-id="scmgit"] .js-plugin-license', 'GNU LGPL 3')
          .checkElementInclude('li[data-id="scmgit"] .js-plugin-organization', 'SonarSource')
          .checkElementCount('li[data-id="scmgit"] .js-update', 1)
          .checkElementCount('li[data-id="scmgit"] .js-uninstall', 1)
          .checkElementNotExist('li[data-id="scmgit"] .js-install');
    });

    bdd.it('should show system upgrade', function () {
      return this.remote
          .open('#system')
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/plugins/pending', 'update-center-spec/pending.json')
          .mockFromFile('/api/system/upgrades', 'update-center-spec/system-updates.json')
          .startAppBrowserify('update-center', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-plugin-name')
          .checkElementCount('li[data-system]', 1)
          .checkElementInclude('li[data-system] .js-plugin-name', 'SonarQube 5.3')
          .checkElementInclude('li[data-system] .js-plugin-category', 'System Upgrade')
          .checkElementInclude('li[data-system] .js-plugin-description', 'New!')
          .checkElementCount('li[data-system] .js-plugin-release-notes', 1)
          .checkElementCount('li[data-system] .js-plugin-date', 1)
          .checkElementCount('li[data-system] .js-plugin-update-steps', 1)
          .checkElementCount('li[data-system] .js-plugin-update-steps > li', 4);
    });

    bdd.it('should show installed', function () {
      return this.remote
          .open('#installed')
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/plugins/installed', 'update-center-spec/installed.json')
          .mockFromFile('/api/plugins/updates', 'update-center-spec/updates.json')
          .mockFromFile('/api/plugins/pending', 'update-center-spec/pending.json')
          .startAppBrowserify('update-center', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-plugin-name')
          .checkElementCount('li[data-id]', 5)
          .checkElementExist('li[data-id="scmgit"]')
          .checkElementExist('li[data-id="javascript"]');
    });

    bdd.it('should show updates', function () {
      return this.remote
          .open('#updates')
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/plugins/installed', 'update-center-spec/installed.json')
          .mockFromFile('/api/plugins/updates', 'update-center-spec/updates.json')
          .mockFromFile('/api/plugins/pending', 'update-center-spec/pending.json')
          .startAppBrowserify('update-center', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-plugin-name')
          .checkElementCount('li[data-id]', 4)
          .checkElementExist('li[data-id="scmgit"]')
          .checkElementNotExist('li[data-id="javascript"]');
    });

    bdd.it('should show available', function () {
      return this.remote
          .open('#available')
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/plugins/available', 'update-center-spec/available.json')
          .mockFromFile('/api/plugins/pending', 'update-center-spec/pending.json')
          .startAppBrowserify('update-center', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-plugin-name')
          .checkElementCount('li[data-id]', 3)
          .checkElementNotExist('li[data-id="scmgit"]')
          .checkElementExist('li[data-id="abap"]');
    });

    bdd.it('should work offline', function () {
      return this.remote
          .open('')
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/plugins/installed', 'update-center-spec/installed.json')
          .mockFromFile('/api/plugins/updates', 'update-center-spec/updates.json')
          .mockFromFile('/api/plugins/pending', 'update-center-spec/pending.json')
          .execute(function () {
            window.SS.updateCenterActive = false;
          })
          .startAppBrowserify('update-center', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-plugin-name')
          .checkElementCount('li[data-id]', 5)
          .checkElementExist('li[data-id="scmgit"]')
          .checkElementExist('li[data-id="javascript"]')
          .checkElementNotExist('#update-center-filter-installed[disabled]')
          .checkElementExist('#update-center-filter-updates[disabled]')
          .checkElementExist('#update-center-filter-available[disabled]')
          .checkElementExist('#update-center-filter-system[disabled]');
    });

    bdd.it('should switch between views', function () {
      return this.remote
          .open('#installed')
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/plugins/installed', 'update-center-spec/installed.json')
          .mockFromFile('/api/plugins/updates', 'update-center-spec/updates.json')
          .mockFromFile('/api/plugins/pending', 'update-center-spec/pending.json')
          .mockFromFile('/api/plugins/available', 'update-center-spec/available.json')
          .mockFromFile('/api/system/upgrades', 'update-center-spec/system-updates.json')
          .startAppBrowserify('update-center', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-plugin-name')
          .checkElementCount('li[data-id]', 5)
          .checkElementExist('li[data-id="javascript"]')
          .checkElementExist('#update-center-filter-installed:checked')
          .clickElement('[for="update-center-filter-available"]')
          .checkElementExist('li[data-id="abap"]')
          .checkElementCount('li[data-id]', 3)
          .checkElementExist('li[data-id="abap"]')
          .checkElementExist('#update-center-filter-available:checked')
          .clickElement('[for="update-center-filter-updates"]')
          .checkElementExist('li[data-id="scmgit"]')
          .checkElementCount('li[data-id]', 4)
          .checkElementExist('li[data-id="scmgit"]')
          .checkElementExist('#update-center-filter-updates:checked')
          .clickElement('[for="update-center-filter-system"]')
          .checkElementExist('li[data-system]')
          .checkElementExist('#update-center-filter-system:checked')
          .clickElement('[for="update-center-filter-installed"]')
          .checkElementExist('li[data-id="javascript"]')
          .checkElementCount('li[data-id]', 5)
          .checkElementExist('li[data-id="javascript"]')
          .checkElementExist('#update-center-filter-installed:checked')
          .clickElement('[for="update-center-filter-available"]')
          .checkElementExist('li[data-id="abap"]')
          .checkElementCount('li[data-id]', 3)
          .checkElementExist('li[data-id="abap"]')
          .checkElementExist('#update-center-filter-available:checked')
          .clickElement('[for="update-center-filter-updates"]')
          .checkElementExist('li[data-id="scmgit"]')
          .checkElementCount('li[data-id]', 4)
          .checkElementExist('li[data-id="scmgit"]')
          .checkElementExist('#update-center-filter-updates:checked')
          .clickElement('[for="update-center-filter-system"]')
          .checkElementExist('li[data-system]')
          .checkElementExist('#update-center-filter-system:checked');
    });

    bdd.it('should search', function () {
      return this.remote
          .open('#installed')
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/plugins/installed', 'update-center-spec/installed.json')
          .mockFromFile('/api/plugins/updates', 'update-center-spec/updates.json')
          .mockFromFile('/api/plugins/pending', 'update-center-spec/pending.json')
          .startAppBrowserify('update-center', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-plugin-name')
          .checkElementCount('li[data-id]', 5)
          .checkElementNotExist('li.hidden[data-id]')
          .fillElement('#update-center-search-query', 'jA')
          .clickElement('#update-center-search-submit')
          .checkElementExist('li.hidden[data-id]')
          .checkElementCount('li[data-id]', 5)
          .checkElementCount('li.hidden[data-id]', 3)
          .checkElementInclude('li:not(.hidden)[data-id] .js-plugin-name', 'JavaScript');
    });

    bdd.it('should search by category on click', function () {
      return this.remote
          .open('#available')
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/plugins/available', 'update-center-spec/available.json')
          .mockFromFile('/api/plugins/pending', 'update-center-spec/pending.json')
          .startAppBrowserify('update-center', { urlRoot: '/test/medium/base.html' })
          .checkElementCount('li[data-id]:not(.hidden)', 3)
          .clickElement('li[data-id="abap"] .js-plugin-category')
          .checkElementCount('li[data-id]:not(.hidden)', 2);
    });

    bdd.it('should show changelog of plugin update', function () {
      return this.remote
          .open('#installed')
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/plugins/installed', 'update-center-spec/installed.json')
          .mockFromFile('/api/plugins/updates', 'update-center-spec/updates.json')
          .mockFromFile('/api/plugins/pending', 'update-center-spec/pending.json')
          .startAppBrowserify('update-center', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-plugin-name')
          .clickElement('li[data-id="python"] .js-changelog')
          .checkElementExist('.bubble-popup')
          .checkElementCount('.bubble-popup .js-plugin-changelog-version', 2)
          .checkElementCount('.bubble-popup .js-plugin-changelog-date', 2)
          .checkElementCount('.bubble-popup .js-plugin-changelog-link', 2)
          .checkElementCount('.bubble-popup .js-plugin-changelog-description', 2);
    });

    bdd.it('should show changelog of plugin release', function () {
      return this.remote
          .open('#available')
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/plugins/available', 'update-center-spec/available.json')
          .mockFromFile('/api/plugins/pending', 'update-center-spec/pending.json')
          .startAppBrowserify('update-center', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-plugin-name')
          .clickElement('li[data-id="abap"] .js-changelog')
          .checkElementExist('.bubble-popup')
          .checkElementCount('.bubble-popup .js-plugin-changelog-version', 1)
          .checkElementCount('.bubble-popup .js-plugin-changelog-date', 1)
          .checkElementCount('.bubble-popup .js-plugin-changelog-link', 1)
          .checkElementCount('.bubble-popup .js-plugin-changelog-description', 1);
    });

    bdd.it('should update plugin', function () {
      return this.remote
          .open('#installed')
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/plugins/installed', 'update-center-spec/installed.json')
          .mockFromFile('/api/plugins/updates', 'update-center-spec/updates.json')
          .mockFromFile('/api/plugins/pending', 'update-center-spec/pending.json')
          .mockFromString('/api/plugins/update', '{}', { data: { key: 'scmgit' } })
          .startAppBrowserify('update-center', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-plugin-name')
          .clickElement('li[data-id="scmgit"] .js-update')
          .checkElementNotExist('li[data-id="scmgit"] .js-spinner')
          .checkElementInclude('li[data-id="scmgit"]', 'Update Pending');
    });

    bdd.it('should uninstall plugin', function () {
      return this.remote
          .open('#installed')
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/plugins/installed', 'update-center-spec/installed.json')
          .mockFromFile('/api/plugins/updates', 'update-center-spec/updates.json')
          .mockFromFile('/api/plugins/pending', 'update-center-spec/pending.json')
          .mockFromString('/api/plugins/uninstall', '{}', { data: { key: 'scmgit' } })
          .startAppBrowserify('update-center', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-plugin-name')
          .clickElement('li[data-id="scmgit"] .js-uninstall')
          .checkElementNotExist('li[data-id="scmgit"] .js-spinner')
          .checkElementInclude('li[data-id="scmgit"]', 'Uninstall Pending');
    });

    bdd.it('should install plugin', function () {
      return this.remote
          .open('#available')
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/plugins/available', 'update-center-spec/available.json')
          .mockFromFile('/api/plugins/pending', 'update-center-spec/pending.json')
          .mockFromString('/api/plugins/install', '{}', { data: { key: 'android' } })
          .startAppBrowserify('update-center', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-plugin-name')
          .clickElement('li[data-id="android"] .js-install')
          .checkElementNotExist('li[data-id="android"] .js-spinner')
          .checkElementInclude('li[data-id="android"]', 'Install Pending');
    });

    bdd.it('should cancel all pending', function () {
      return this.remote
          .open('#available')
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/plugins/available', 'update-center-spec/available.json')
          .mockFromFile('/api/plugins/pending', 'update-center-spec/pending.json')
          .mockFromString('/api/plugins/cancel_all', '{}')
          .startAppBrowserify('update-center', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-plugin-name')
          .checkElementExist('.js-pending')
          .clickElement('.js-cancel-all')
          .checkElementNotExist('.js-pending');
    });

    bdd.it('should should check terms and conditions', function () {
      return this.remote
          .open('#available')
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/plugins/available', 'update-center-spec/available.json')
          .mockFromFile('/api/plugins/pending', 'update-center-spec/pending.json')
          .mockFromString('/api/plugins/install', '{}', { data: { key: 'abap' } })
          .startAppBrowserify('update-center', { urlRoot: '/test/medium/base.html' })
          .checkElementExist('.js-plugin-name')
          .checkElementExist('li[data-id="abap"] .js-terms')
          .checkElementExist('li[data-id="abap"] .js-install[disabled]')
          .clickElement('li[data-id="abap"] .js-terms')
          .checkElementNotExist('li[data-id="abap"] .js-install[disabled]')
          .clickElement('li[data-id="abap"] .js-install')
          .checkElementNotExist('li[data-id="abap"] .js-spinner')
          .checkElementInclude('li[data-id="abap"]', 'Install Pending');
    });
  });
});

