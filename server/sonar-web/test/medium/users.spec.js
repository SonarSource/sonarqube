define(function (require) {
  var bdd = require('intern!bdd');
  require('../helpers/test-page');

  bdd.describe('Users Page', function () {
    bdd.it('should show list of users', function () {
      return this.remote
          .get(require.toUrl('test/medium/base.html'))
          .setFindTimeout(5000)
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/users/search', 'users-spec/search.json')
          .startApp('users')
          .checkElementCount('#users-list ul', 1)
          .checkElementCount('#users-list li[data-login]', 3)
          .checkElementInclude('#users-list .js-user-login', 'smith')
          .checkElementInclude('#users-list .js-user-name', 'Bob')
          .checkElementInclude('#users-list .js-user-email', 'bob@example.com')
          .checkElementCount('#users-list .js-user-update', 3)
          .checkElementCount('#users-list .js-user-change-password', 3)
          .checkElementCount('#users-list .js-user-deactivate', 3)
        //.checkElementInclude('#users-list-footer', '3/3')
          .checkElementNotInclude('[data-login="ryan"]', 'another@example.com')
          .clickElement('[data-login="ryan"] .js-user-more-scm')
          .checkElementInclude('[data-login="ryan"]', 'another@example.com')
          .checkElementNotInclude('[data-login="ryan"]', 'four')
          .clickElement('[data-login="ryan"] .js-user-more-groups')
          .checkElementInclude('[data-login="ryan"]', 'four');
    });

    bdd.it('should search users', function () {
      return this.remote
          .get(require.toUrl('test/medium/base.html'))
          .setFindTimeout(5000)
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/users/search', 'users-spec/search.json')
          .startApp('users')
          .checkElementCount('#users-list ul', 1)
          .checkElementCount('#users-list li[data-login]', 3)
          .clearMocks()
          .mockFromFile('/api/users/search', 'users-spec/search-filtered.json')
          .fillElement('#users-search-query', 'ryan')
          .clickElement('#users-search-submit')
          .waitForDeletedByCssSelector('[data-login="admin"]')
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
          .get(require.toUrl('test/medium/base.html'))
          .setFindTimeout(5000)
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/users/search', 'users-spec/search-big-1.json')
          .startApp('users')
          .checkElementCount('#users-list ul', 1)
          .checkElementCount('#users-list li[data-login]', 2)
          .clearMocks()
          .mockFromFile('/api/users/search', 'users-spec/search-big-2.json')
          .clickElement('#users-fetch-more')
          .checkElementCount('[data-login="ryan"]', 1)
          .checkElementCount('#users-list li[data-login]', 3);
    });

    bdd.it('should create a new user', function () {
      return this.remote
          .get(require.toUrl('test/medium/base.html'))
          .setFindTimeout(5000)
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/users/search', 'users-spec/search.json')
          .startApp('users')
          .checkElementCount('#users-list ul', 1)
          .checkElementCount('#users-list li[data-login]', 3)
          .clickElement('#users-create')
          .checkElementCount('#create-user-form', 1)
          .clearMocks()
          .mockFromFile('/api/users/search', 'users-spec/search-created.json')
          .mockFromString('/api/users/create', '{}')
          .clickElement('#create-user-add-scm-account')
          .clickElement('#create-user-add-scm-account')
          .execute(function () {
            jQuery('#create-user-login').val('login');
            jQuery('#create-user-name').val('name');
            jQuery('#create-user-email').val('email@example.com');
            jQuery('#create-user-password').val('secret');
            jQuery('[name="scmAccounts"]').first().val('scm1');
            jQuery('[name="scmAccounts"]').last().val('scm2');
          })
          .clickElement('#create-user-submit')
          .checkElementCount('[data-login="login"]', 1)
          .checkElementCount('#users-list li[data-login]', 4)
          .checkElementInclude('#users-list .js-user-login', 'login')
          .checkElementInclude('#users-list .js-user-name', 'name')
          .checkElementInclude('#users-list .js-user-email', 'email@example.com');
    });

    bdd.it('should update a user', function () {
      return this.remote
          .get(require.toUrl('test/medium/base.html'))
          .setFindTimeout(5000)
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/users/search', 'users-spec/search.json')
          .startApp('users')
          .checkElementCount('#users-list ul', 1)
          .clickElement('[data-login="smith"] .js-user-update')
          .checkElementCount('#create-user-form', 1)
          .clearMocks()
          .mockFromFile('/api/users/search', 'users-spec/search-updated.json')
          .mockFromString('/api/users/update', '{}')
          .clickElement('#create-user-add-scm-account')
          .execute(function () {
            jQuery('#create-user-name').val('Mike');
            jQuery('#create-user-email').val('mike@example.com');
            jQuery('[name="scmAccounts"]').first().val('scm5');
            jQuery('[name="scmAccounts"]').last().val('scm6');
          })
          .clickElement('#create-user-submit')
          .waitForDeletedByCssSelector('#create-user-form')
          .checkElementInclude('[data-login="smith"] .js-user-login', 'smith')
          .checkElementInclude('[data-login="smith"] .js-user-name', 'Mike')
          .checkElementInclude('[data-login="smith"] .js-user-email', 'mike@example.com');
    });

    bdd.it('should change user\'s password', function () {
      return this.remote
          .get(require.toUrl('test/medium/base.html'))
          .setFindTimeout(5000)
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/users/search', 'users-spec/search.json')
          .startApp('users')
          .checkElementCount('#users-list ul', 1)
          .clickElement('[data-login="smith"] .js-user-change-password')
          .checkElementCount('#change-user-password-form', 1)
          .clearMocks()
          .mockFromString('/api/users/change_password', '{}')
          .execute(function () {
            jQuery('#change-user-password-password').val('secret');
            jQuery('#change-user-password-password-confirmation').val('another');
          })
          .clickElement('#change-user-password-submit')
          .checkElementCount('.alert.alert-danger', 1)
          .execute(function () {
            jQuery('#change-user-password-password').val('secret');
            jQuery('#change-user-password-password-confirmation').val('secret');
          })
          .clickElement('#change-user-password-submit')
          .waitForDeletedByCssSelector('#change-user-password-form');
    });

    bdd.it('should deactivate a user', function () {
      return this.remote
          .get(require.toUrl('test/medium/base.html'))
          .setFindTimeout(5000)
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/users/search', 'users-spec/search.json')
          .startApp('users')
          .checkElementCount('#users-list ul', 1)
          .clickElement('[data-login="smith"] .js-user-deactivate')
          .checkElementCount('#deactivate-user-form', 1)
          .clearMocks()
          .mockFromString('/api/users/deactivate', '{}')
          .clickElement('#deactivate-user-submit')
          .waitForDeletedByCssSelector('[data-login="smith"]');
    });
  });

});
