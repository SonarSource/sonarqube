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
  });
});
