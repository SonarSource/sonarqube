define(function (require) {
  var bdd = require('intern!bdd');
  require('../helpers/test-page');

  bdd.describe('Users Page', function () {
    bdd.it('should show list of users', function () {
      return this.remote
          .open()
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/users/search', 'users-spec/search.json')
          .startAppBrowserify('users')
          .checkElementCount('#users-list li[data-login]', 3)
          .checkElementInclude('#users-list .js-user-login', 'smith')
          .checkElementInclude('#users-list .js-user-name', 'Bob')
          .checkElementInclude('#users-list .js-user-email', 'bob@example.com')
          .checkElementCount('#users-list .js-user-update', 3)
          .checkElementCount('#users-list .js-user-change-password', 3)
          .checkElementCount('#users-list .js-user-deactivate', 3)
          .checkElementInclude('#users-list-footer', '3/3')
          .checkElementNotInclude('[data-login="ryan"]', 'another@example.com')
          .clickElement('[data-login="ryan"] .js-user-more-scm')
          .checkElementInclude('[data-login="ryan"]', 'another@example.com')
          .checkElementNotInclude('[data-login="ryan"]', 'four')
          .clickElement('[data-login="ryan"] .js-user-more-groups')
          .checkElementInclude('[data-login="ryan"]', 'four');
    });

    bdd.it('should search users', function () {
      return this.remote
          .open()
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/users/search', 'users-spec/search.json')
          .startAppBrowserify('users')
          .checkElementCount('#users-list li[data-login]', 3)
          .clearMocks()
          .mockFromFile('/api/users/search', 'users-spec/search-filtered.json')
          .fillElement('#users-search-query', 'ryan')
          .clickElement('#users-search-submit')
          .checkElementNotExist('[data-login="admin"]')
          .checkElementCount('#users-list li[data-login]', 1)
          .clearMocks()
          .mockFromFile('/api/users/search', 'users-spec/search.json')
          .fillElement('#users-search-query', '')
          .clickElement('#users-search-submit')
          .checkElementCount('[data-login="admin"]', 1)
          .checkElementCount('#users-list li[data-login]', 3);
    });

    bdd.it('should show more', function () {
      return this.remote
          .open()
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/users/search', 'users-spec/search-big-1.json')
          .startAppBrowserify('users')
          .checkElementCount('#users-list li[data-login]', 2)
          .clearMocks()
          .mockFromFile('/api/users/search', 'users-spec/search-big-2.json')
          .clickElement('#users-fetch-more')
          .checkElementCount('[data-login="ryan"]', 1)
          .checkElementCount('#users-list li[data-login]', 3);
    });

    bdd.it('should create a new user', function () {
      return this.remote
          .open()
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/users/search', 'users-spec/search.json')
          .startAppBrowserify('users')
          .checkElementCount('#users-list li[data-login]', 3)
          .clickElement('#users-create')
          .checkElementCount('#create-user-form', 1)
          .clearMocks()
          .mockFromFile('/api/users/search', 'users-spec/search-created.json')
          .mockFromString('/api/users/create', '{}')
          .clickElement('#create-user-add-scm-account')
          .clickElement('#create-user-add-scm-account')
          .fillElement('#create-user-login', 'login')
          .fillElement('#create-user-name', 'name')
          .fillElement('#create-user-email', 'email@example.com')
          .fillElement('#create-user-password', 'secret')
          .fillElement('[name="scmAccounts"]:first-child', 'scm1')
          .fillElement('[name="scmAccounts"]:last-child', 'scm2')
          .clickElement('#create-user-submit')
          .checkElementCount('[data-login="login"]', 1)
          .checkElementCount('#users-list li[data-login]', 4)
          .checkElementInclude('#users-list .js-user-login', 'login')
          .checkElementInclude('#users-list .js-user-name', 'name')
          .checkElementInclude('#users-list .js-user-email', 'email@example.com');
    });

    bdd.it('should update a user', function () {
      return this.remote
          .open()
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/users/search', 'users-spec/search.json')
          .startAppBrowserify('users')
          .clickElement('[data-login="smith"] .js-user-update')
          .checkElementCount('#create-user-form', 1)
          .clearMocks()
          .mockFromFile('/api/users/search', 'users-spec/search-updated.json')
          .mockFromString('/api/users/update', '{}')
          .clickElement('#create-user-add-scm-account')
          .fillElement('#create-user-name', 'Mike')
          .fillElement('#create-user-email', 'mike@example.com')
          .fillElement('[name="scmAccounts"]:first-child', 'scm5')
          .fillElement('[name="scmAccounts"]:last-child', 'scm6')
          .clickElement('#create-user-submit')
          .waitForDeletedByCssSelector('#create-user-form')
          .checkElementInclude('[data-login="smith"] .js-user-login', 'smith')
          .checkElementInclude('[data-login="smith"] .js-user-name', 'Mike')
          .checkElementInclude('[data-login="smith"] .js-user-email', 'mike@example.com');
    });

    bdd.it('should change user\'s password', function () {
      return this.remote
          .open()
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/users/search', 'users-spec/search.json')
          .startAppBrowserify('users')
          .clickElement('[data-login="smith"] .js-user-change-password')
          .checkElementCount('#change-user-password-form', 1)
          .clearMocks()
          .mockFromString('/api/users/change_password', '{}')
          .fillElement('#change-user-password-password', 'secret')
          .fillElement('#change-user-password-password-confirmation', 'another')
          .clickElement('#change-user-password-submit')
          .checkElementCount('.alert.alert-danger', 1)
          .fillElement('#change-user-password-password', 'secret')
          .fillElement('#change-user-password-password-confirmation', 'secret')
          .clickElement('#change-user-password-submit')
          .waitForDeletedByCssSelector('#change-user-password-form');
    });

    bdd.it('should deactivate a user', function () {
      return this.remote
          .open()
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/users/search', 'users-spec/search.json')
          .startAppBrowserify('users')
          .clickElement('[data-login="smith"] .js-user-deactivate')
          .checkElementCount('#deactivate-user-form', 1)
          .clearMocks()
          .mockFromString('/api/users/deactivate', '{}')
          .clickElement('#deactivate-user-submit')
          .waitForDeletedByCssSelector('[data-login="smith"]');
    });
  });

});
