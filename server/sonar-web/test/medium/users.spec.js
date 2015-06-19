define(function (require) {
  var intern = require('intern');
  var bdd = require('intern!bdd');
  var assert = require('intern/chai!assert');
  var fs = require('intern/dojo/node!fs');

  bdd.describe('Users Page', function () {

    bdd.it('should show list of users', function () {
      var searchResponse = fs.readFileSync('src/test/json/users-spec/search.json', 'utf-8');

      return this.remote
          .get(require.toUrl('test/medium/base.html'))
          .setFindTimeout(5000)
          .findByCssSelector('#content')
          .execute(function () {
            return jQuery.mockjax(_.extend({ url: '/api/l10n/index', responseText: {} }));
          })
          .execute(function (searchResponse) {
            return jQuery.mockjax(_.extend({ url: '/api/users/search', responseText: searchResponse }));
          }, [searchResponse])
          .execute(function () {
            require(['apps/users/app'], function (App) {
              App.start({ el: '#content' });
            });
          })
          .findByCssSelector('#users-list ul')
          .findAllByCssSelector('#users-list li[data-login]')
          .then(function (elements) {
            assert.equal(3, elements.length);
          })
          .end()
          .findAllByCssSelector('#users-list .js-user-login')
          .getVisibleText()
          .then(function (text) {
            assert.include(text, 'smith');
          })
          .end()
          .findAllByCssSelector('#users-list .js-user-name')
          .getVisibleText()
          .then(function (text) {
            assert.include(text, 'Bob');
          })
          .end()
          .findAllByCssSelector('#users-list .js-user-email')
          .getVisibleText()
          .then(function (text) {
            assert.include(text, 'bob@example.com');
          })
          .end()
          .findAllByCssSelector('#users-list .js-user-update')
          .then(function (elements) {
            assert.equal(3, elements.length);
          })
          .end()
          .findAllByCssSelector('#users-list .js-user-change-password')
          .then(function (elements) {
            assert.equal(3, elements.length);
          })
          .end()
          .findAllByCssSelector('#users-list .js-user-deactivate')
          .then(function (elements) {
            assert.equal(3, elements.length);
          })
          .end()
        //.findByCssSelector('#users-list-footer')
        //.getVisibleText()
        //.then(function (text) {
        //  assert.include(text, '3/3');
        //})
        //.end()
          .findByCssSelector('[data-login="ryan"]')
          .getVisibleText()
          .then(function (text) {
            assert.notInclude(text, 'another@example.com');
          })
          .end()
          .findByCssSelector('[data-login="ryan"] .js-user-more-scm')
          .click()
          .end()
          .findByCssSelector('[data-login="ryan"]')
          .getVisibleText()
          .then(function (text) {
            assert.include(text, 'another@example.com');
          })
          .end()
          .findByCssSelector('[data-login="ryan"]')
          .getVisibleText()
          .then(function (text) {
            assert.notInclude(text, 'four');
          })
          .end()
          .findByCssSelector('[data-login="ryan"] .js-user-more-groups')
          .click()
          .end()
          .findByCssSelector('[data-login="ryan"]')
          .getVisibleText()
          .then(function (text) {
            assert.include(text, 'four');
          });
    });

  });
});
