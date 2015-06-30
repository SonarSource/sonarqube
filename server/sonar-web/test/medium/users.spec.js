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
          .assertElementCount('#users-list ul', 1)
          .assertElementCount('#users-list li[data-login]', 3)
          .assertElementInclude('#users-list .js-user-login', 'smith')
          .assertElementInclude('#users-list .js-user-name', 'Bob')
          .assertElementInclude('#users-list .js-user-email', 'bob@example.com')
          .assertElementCount('#users-list .js-user-update', 3)
          .assertElementCount('#users-list .js-user-change-password', 3)
          .assertElementCount('#users-list .js-user-deactivate', 3)
        //.assertElementInclude('#users-list-footer', '3/3')
          .assertElementNotInclude('[data-login="ryan"]', 'another@example.com')
          .clickElement('[data-login="ryan"] .js-user-more-scm')
          .assertElementInclude('[data-login="ryan"]', 'another@example.com')
          .assertElementNotInclude('[data-login="ryan"]', 'four')
          .clickElement('[data-login="ryan"] .js-user-more-groups')
          .assertElementInclude('[data-login="ryan"]', 'four');
    });

    bdd.it('should search users', function () {
      return this.remote
          .get(require.toUrl('test/medium/base.html'))
          .setFindTimeout(5000)
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/users/search', 'users-spec/search.json')
          .startApp('users')
          .assertElementCount('#users-list ul', 1)
          .assertElementCount('#users-list li[data-login]', 3)
          .clearMocks()
          .mockFromFile('/api/users/search', 'users-spec/search-filtered.json')
          .fillElement('#users-search-query', 'ryan')
          .clickElement('#users-search-submit')
          .waitForDeletedByCssSelector('[data-login="admin"]')
          .assertElementCount('#users-list li[data-login]', 1)
          .clearMocks()
          .mockFromFile('/api/users/search', 'users-spec/search.json')
          .fillElement('#users-search-query', '')
          .clickElement('#users-search-submit')
          .assertElementCount('[data-login="admin"]', 1)
          .assertElementCount('#users-list li[data-login]', 3);
    });

    bdd.it('should show more', function () {
      return this.remote
          .get(require.toUrl('test/medium/base.html'))
          .setFindTimeout(5000)
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/users/search', 'users-spec/search-big-1.json')
          .startApp('users')
          .assertElementCount('#users-list ul', 1)
          .assertElementCount('#users-list li[data-login]', 2)
          .clearMocks()
          .mockFromFile('/api/users/search', 'users-spec/search-big-2.json')
          .clickElement('#users-fetch-more')
          .assertElementCount('[data-login="ryan"]', 1)
          .assertElementCount('#users-list li[data-login]', 3);
    });

    bdd.it('should create a new user', function () {
      return this.remote
          .get(require.toUrl('test/medium/base.html'))
          .setFindTimeout(5000)
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/users/search', 'users-spec/search.json')
          .startApp('users')
          .assertElementCount('#users-list ul', 1)
          .assertElementCount('#users-list li[data-login]', 3)
          .clickElement('#users-create')
          .assertElementCount('#create-user-form', 1)
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
          .assertElementCount('[data-login="login"]', 1)
          .assertElementCount('#users-list li[data-login]', 4)
          .assertElementInclude('#users-list .js-user-login', 'login')
          .assertElementInclude('#users-list .js-user-name', 'name')
          .assertElementInclude('#users-list .js-user-email', 'email@example.com');
    });

    bdd.it('should update a user', function () {
      return this.remote
          .get(require.toUrl('test/medium/base.html'))
          .setFindTimeout(5000)
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/users/search', 'users-spec/search.json')
          .startApp('users')
          .assertElementCount('#users-list ul', 1)
          .clickElement('[data-login="smith"] .js-user-update')
          .assertElementCount('#create-user-form', 1)
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
          .assertElementInclude('[data-login="smith"] .js-user-login', 'smith')
          .assertElementInclude('[data-login="smith"] .js-user-name', 'Mike')
          .assertElementInclude('[data-login="smith"] .js-user-email', 'mike@example.com');
    });

    bdd.it('should change user\'s password', function () {
      return this.remote
          .get(require.toUrl('test/medium/base.html'))
          .setFindTimeout(5000)
          .mockFromString('/api/l10n/index', '{}')
          .mockFromFile('/api/users/search', 'users-spec/search.json')
          .startApp('users')
          .assertElementCount('#users-list ul', 1)
          .clickElement('[data-login="smith"] .js-user-change-password')
          .assertElementCount('#change-user-password-form', 1)
          .clearMocks()
          .mockFromString('/api/users/change_password', '{}')
          .execute(function () {
            jQuery('#change-user-password-password').val('secret');
            jQuery('#change-user-password-password-confirmation').val('another');
          })
          .clickElement('#change-user-password-submit')
          .assertElementCount('.alert.alert-danger', 1)
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
          .assertElementCount('#users-list ul', 1)
          .clickElement('[data-login="smith"] .js-user-deactivate')
          .assertElementCount('#deactivate-user-form', 1)
          .clearMocks()
          .mockFromString('/api/users/deactivate', '{}')
          .clickElement('#deactivate-user-submit')
          .waitForDeletedByCssSelector('[data-login="smith"]');
    });
  });

});
