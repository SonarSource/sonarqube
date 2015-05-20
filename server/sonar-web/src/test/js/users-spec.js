/* globals casper: false */
var lib = require('../lib'),
    testName = lib.testName('Users');

lib.initMessages();
lib.changeWorkingDirectory('users-spec');
lib.configureCasper();

casper.test.begin(testName('List'), 11, function (test) {
  casper
      .start(lib.buildUrl('users'), function () {
        lib.setDefaultViewport();
        lib.mockRequestFromFile('/api/users/search', 'search.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/users/app'], function (App) {
            App.start({ el: '#users' });
          });
        });
      })

      .then(function () {
        casper.waitForText('Bob');
      })

      .then(function () {
        test.assertExists('#users-list table');
        test.assertExists('#users-list thead');
        test.assertExists('#users-list tbody');
        test.assertElementCount('#users-list tbody tr', 3);
        test.assertSelectorContains('#users-list tbody', 'smith');
        test.assertSelectorContains('#users-list tbody', 'Bob');
        test.assertSelectorContains('#users-list tbody', 'bob@example.com');
        test.assertElementCount('#users-list tbody .js-user-update', 3);
        test.assertElementCount('#users-list tbody .js-user-change-password', 3);
        test.assertElementCount('#users-list tbody .js-user-deactivate', 3);
        test.assertSelectorContains('#users-list-footer', '3/3');
      })

      .then(function () {
        lib.sendCoverage();
      })
      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Filter'), 4, function (test) {
  casper
      .start(lib.buildUrl('users'), function () {
        lib.setDefaultViewport();
        this.searchMock = lib.mockRequestFromFile('/api/users/search', 'search.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/users/app'], function (App) {
            App.start({ el: '#users' });
          });
        });
      })

      .then(function () {
        casper.waitForText('Bob');
      })

      .then(function () {
        test.assertElementCount('#users-list tbody tr', 3);
        lib.clearRequestMock(this.searchMock);
        this.searchMock = lib.mockRequestFromFile('/api/users/search', 'search-filtered.json', { data: { q: 'ryan' } });
        casper.evaluate(function () {
          jQuery('#users-search-query').val('ryan');
        });
        casper.click('#users-search-submit');
        casper.waitForSelectorTextChange('#users-list-footer');
      })

      .then(function () {
        test.assertElementCount('#users-list tbody tr', 1);
        lib.clearRequestMock(this.searchMock);
        this.searchMock = lib.mockRequestFromFile('/api/users/search', 'search.json');
        casper.click('#users-search-cancel');
        casper.waitForSelectorTextChange('#users-list-footer');
      })

      .then(function () {
        test.assertElementCount('#users-list tbody tr', 3);
        test.assert(casper.evaluate(function () {
          return jQuery('#users-search-query').val() === '';
        }));
      })

      .then(function () {
        lib.sendCoverage();
      })
      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Show More'), 4, function (test) {
  casper
      .start(lib.buildUrl('users'), function () {
        lib.setDefaultViewport();
        this.searchMock = lib.mockRequestFromFile('/api/users/search', 'search-big-1.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/users/app'], function (App) {
            App.start({ el: '#users' });
          });
        });
      })

      .then(function () {
        casper.waitForText('Bob');
      })

      .then(function () {
        test.assertElementCount('#users-list tbody tr', 2);
        test.assertSelectorContains('#users-list-footer', '2/3');
        lib.clearRequestMock(this.searchMock);
        this.searchMock = lib.mockRequestFromFile('/api/users/search', 'search-big-2.json', { data: { p: '2' } });
        casper.click('#users-fetch-more');
        casper.waitForSelectorTextChange('#users-list-footer');
      })

      .then(function () {
        test.assertElementCount('#users-list tbody tr', 3);
        test.assertSelectorContains('#users-list-footer', '3/3');
      })

      .then(function () {
        lib.sendCoverage();
      })
      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Create'), 5, function (test) {
  casper
      .start(lib.buildUrl('users'), function () {
        lib.setDefaultViewport();
        this.searchMock = lib.mockRequestFromFile('/api/users/search', 'search.json');
        this.createMock = lib.mockRequestFromFile('/api/users/create', 'error.json', { status: 400 });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/users/app'], function (App) {
            App.start({ el: '#users' });
          });
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForText('Bob');
      })

      .then(function () {
        test.assertElementCount('#users-list tbody tr', 3);
        casper.click('#users-create');
        casper.waitForSelector('#create-user-form');
      })

      .then(function () {
        casper.click('#create-user-submit');
        casper.waitForSelector('.alert.alert-danger');
      })

      .then(function () {
        lib.clearRequestMock(this.searchMock);
        lib.mockRequestFromFile('/api/users/search', 'search-created.json');
        lib.clearRequestMock(this.createMock);
        lib.mockRequest('/api/users/create', '{}',
            { data: { login: 'login', name: 'name', email: 'email@example.com', scmAccounts: 'scm1,scm2' } });
        casper.click('#create-user-add-scm-account');
        casper.click('#create-user-add-scm-account');
        casper.evaluate(function () {
          jQuery('#create-user-login').val('login');
          jQuery('#create-user-name').val('name');
          jQuery('#create-user-email').val('email@example.com');
          jQuery('[name="scmAccounts"]').first().val('scm1');
          jQuery('[name="scmAccounts"]').last().val('scm2');
        });
        casper.click('#create-user-submit');
        casper.waitForSelectorTextChange('#users-list-footer');
      })

      .then(function () {
        test.assertElementCount('#users-list tbody tr', 4);
        test.assertSelectorContains('#users-list tbody', 'login');
        test.assertSelectorContains('#users-list tbody', 'name');
        test.assertSelectorContains('#users-list tbody', 'email@example.com');
      })

      .then(function () {
        lib.sendCoverage();
      })
      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Update'), 3, function (test) {
  casper
      .start(lib.buildUrl('users'), function () {
        lib.setDefaultViewport();
        this.searchMock = lib.mockRequestFromFile('/api/users/search', 'search.json');
        this.updateMock = lib.mockRequestFromFile('/api/users/update', 'error.json', { status: 400 });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/users/app'], function (App) {
            App.start({ el: '#users' });
          });
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForText('Bob');
      })

      .then(function () {
        casper.click('tr[data-login="smith"] .js-user-update');
        casper.waitForSelector('#create-user-form');
      })

      .then(function () {
        casper.click('#create-user-submit');
        casper.waitForSelector('.alert.alert-danger');
      })

      .then(function () {
        lib.clearRequestMock(this.searchMock);
        lib.mockRequestFromFile('/api/users/search', 'search-updated.json');
        lib.clearRequestMock(this.updateMock);
        lib.mockRequest('/api/users/update', '{}',
            { data: { login: 'smith', name: 'Mike', email: 'mike@example.com', scmAccounts: 'scm5,scm6' } });
        casper.click('#create-user-add-scm-account');
        casper.evaluate(function () {
          jQuery('#create-user-name').val('Mike');
          jQuery('#create-user-email').val('mike@example.com');
          jQuery('[name="scmAccounts"]').first().val('scm5');
          jQuery('[name="scmAccounts"]').last().val('scm6');
        });
        casper.click('#create-user-submit');
        casper.waitForText('Mike');
      })

      .then(function () {
        test.assertSelectorContains('tr[data-login="smith"]', 'smith');
        test.assertSelectorContains('tr[data-login="smith"]', 'Mike');
        test.assertSelectorContains('tr[data-login="smith"]', 'mike@example.com');
      })

      .then(function () {
        lib.sendCoverage();
      })
      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Change Password'), 1, function (test) {
  casper
      .start(lib.buildUrl('users'), function () {
        lib.setDefaultViewport();
        this.searchMock = lib.mockRequestFromFile('/api/users/search', 'search.json');
        this.updateMock = lib.mockRequestFromFile('/api/users/change_password', 'error.json', { status: 400 });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/users/app'], function (App) {
            App.start({ el: '#users' });
          });
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForText('Bob');
      })

      .then(function () {
        casper.click('tr[data-login="smith"] .js-user-change-password');
        casper.waitForSelector('#change-user-password-form');
      })

      .then(function () {
        casper.click('#change-user-password-submit');
        casper.waitForSelector('.alert.alert-danger');
      })

      .then(function () {
        casper.click('#change-user-password-cancel');
        casper.waitWhileSelector('#change-user-password-form');
      })

      .then(function () {
        casper.click('tr[data-login="smith"] .js-user-change-password');
        casper.waitForSelector('#change-user-password-form');
      })

      .then(function () {
        lib.clearRequestMock(this.updateMock);
        lib.mockRequest('/api/users/change_password', '{}', { data: { login: 'smith', password: 'secret' } });
        casper.evaluate(function () {
          jQuery('#change-user-password-password').val('secret');
          jQuery('#change-user-password-password-confirmation').val('another');
        });
        casper.click('#change-user-password-submit');
        casper.waitForSelector('.alert.alert-danger');
      })

      .then(function () {
        casper.evaluate(function () {
          jQuery('#change-user-password-password').val('secret');
          jQuery('#change-user-password-password-confirmation').val('secret');
        });
        casper.click('#change-user-password-submit');
        casper.waitWhileSelector('#change-user-password-form');
      })

      .then(function () {
        test.assert(true);
      })

      .then(function () {
        lib.sendCoverage();
      })
      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Deactivate'), 1, function (test) {
  casper
      .start(lib.buildUrl('users'), function () {
        lib.setDefaultViewport();
        this.searchMock = lib.mockRequestFromFile('/api/users/search', 'search.json');
        this.updateMock = lib.mockRequestFromFile('/api/users/deactivate', 'error.json', { status: 400 });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/users/app'], function (App) {
            App.start({ el: '#users' });
          });
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForText('Bob');
      })

      .then(function () {
        casper.click('tr[data-login="smith"] .js-user-deactivate');
        casper.waitForSelector('#deactivate-user-form');
      })

      .then(function () {
        casper.click('#deactivate-user-submit');
        casper.waitForSelector('.alert.alert-danger');
      })

      .then(function () {
        lib.clearRequestMock(this.updateMock);
        lib.mockRequest('/api/users/deactivate', '{}', { data: { login: 'smith'} });
        casper.click('#deactivate-user-submit');
        casper.waitWhileSelector('tr[data-login="smith"]');
      })

      .then(function () {
        test.assert(true);
      })

      .then(function () {
        lib.sendCoverage();
      })
      .run(function () {
        test.done();
      });
});
