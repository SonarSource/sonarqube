/* globals casper: false */
var lib = require('../lib'),
    testName = lib.testName('Groups');

lib.initMessages();
lib.changeWorkingDirectory('groups-spec');
lib.configureCasper();

casper.test.begin(testName('List'), 7, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();
        lib.mockRequestFromFile('/api/usergroups/search', 'search.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/groups/app'], function (App) {
            App.start({ el: '#content' });
          });
        });
      })

      .then(function () {
        casper.waitForText('sonar-users');
      })

      .then(function () {
        test.assertExists('#groups-list ul');
        test.assertElementCount('#groups-list li[data-id]', 2);
        test.assertSelectorContains('#groups-list .js-group-name', 'sonar-users');
        test.assertSelectorContains('#groups-list .js-group-description',
            'Any new users created will automatically join this group');
        test.assertElementCount('#groups-list .js-group-update', 2);
        test.assertElementCount('#groups-list .js-group-delete', 2);
        test.assertSelectorContains('#groups-list-footer', '2/2');
      })

      .then(function () {
        lib.sendCoverage();
      })
      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Search'), 4, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();
        this.searchMock = lib.mockRequestFromFile('/api/usergroups/search', 'search.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/groups/app'], function (App) {
            App.start({ el: '#content' });
          });
        });
      })

      .then(function () {
        casper.waitForText('sonar-users');
      })

      .then(function () {
        test.assertElementCount('#groups-list li[data-id]', 2);
        lib.clearRequestMock(this.searchMock);
        this.searchMock = lib.mockRequestFromFile('/api/usergroups/search', 'search-filtered.json',
            { data: { q: 'adm' } });
        casper.evaluate(function () {
          jQuery('#groups-search-query').val('adm');
        });
        casper.click('#groups-search-submit');
        casper.waitForSelectorTextChange('#groups-list-footer');
      })

      .then(function () {
        test.assertElementCount('#groups-list li[data-id]', 1);
        lib.clearRequestMock(this.searchMock);
        this.searchMock = lib.mockRequestFromFile('/api/usergroups/search', 'search.json');
        casper.evaluate(function () {
          jQuery('#groups-search-query').val('');
        });
        casper.click('#groups-search-submit');
        casper.waitForSelectorTextChange('#groups-list-footer');
      })

      .then(function () {
        test.assertElementCount('#groups-list li[data-id]', 2);
        test.assert(casper.evaluate(function () {
          return jQuery('#groups-search-query').val() === '';
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
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();
        this.searchMock = lib.mockRequestFromFile('/api/usergroups/search', 'search-big-1.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/groups/app'], function (App) {
            App.start({ el: '#content' });
          });
        });
      })

      .then(function () {
        casper.waitForText('sonar-users');
      })

      .then(function () {
        test.assertElementCount('#groups-list li[data-id]', 1);
        test.assertSelectorContains('#groups-list-footer', '1/2');
        lib.clearRequestMock(this.searchMock);
        this.searchMock = lib.mockRequestFromFile('/api/usergroups/search', 'search-big-2.json', { data: { p: '2' } });
        casper.click('#groups-fetch-more');
        casper.waitForSelectorTextChange('#groups-list-footer');
      })

      .then(function () {
        test.assertElementCount('#groups-list li[data-id]', 2);
        test.assertSelectorContains('#groups-list-footer', '2/2');
      })

      .then(function () {
        lib.sendCoverage();
      })
      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Show Users'), 2, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();
        this.searchMock = lib.mockRequestFromFile('/api/usergroups/search', 'search.json');
        this.searchMock = lib.mockRequestFromFile('/api/usergroups/users*', 'users.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/groups/app'], function (App) {
            App.start({ el: '#content' });
          });
        });
      })

      .then(function () {
        casper.waitForText('sonar-users');
      })

      .then(function () {
        test.assertTextDoesntExist('Bob');
        casper.click('[data-id="1"] .js-group-users');
        casper.waitForText('Bob');
      })

      .then(function () {
        test.assertTextExist('John');
      })

      .then(function () {
        lib.sendCoverage();
      })
      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Create'), 4, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();
        this.searchMock = lib.mockRequestFromFile('/api/usergroups/search', 'search.json');
        this.createMock = lib.mockRequestFromFile('/api/usergroups/create', 'error.json', { status: 400 });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/groups/app'], function (App) {
            App.start({ el: '#content' });
          });
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForText('sonar-users');
      })

      .then(function () {
        test.assertElementCount('#groups-list li[data-id]', 2);
        casper.click('#groups-create');
        casper.waitForSelector('#create-group-form');
      })

      .then(function () {
        casper.click('#create-group-submit');
        casper.waitForSelector('.alert.alert-danger');
      })

      .then(function () {
        lib.clearRequestMock(this.searchMock);
        lib.mockRequestFromFile('/api/usergroups/search', 'search-created.json');
        lib.clearRequestMock(this.createMock);
        lib.mockRequest('/api/usergroups/create', '{}',
            { data: { name: 'name', description: 'description' } });
        casper.evaluate(function () {
          jQuery('#create-group-name').val('name');
          jQuery('#create-group-description').val('description');
        });
        casper.click('#create-group-submit');
        casper.waitForSelectorTextChange('#groups-list-footer');
      })

      .then(function () {
        test.assertElementCount('#groups-list li[data-id]', 3);
        test.assertSelectorContains('#groups-list .js-group-name', 'name');
        test.assertSelectorContains('#groups-list .js-group-description', 'description');
      })

      .then(function () {
        lib.sendCoverage();
      })
      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Update'), 2, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();
        this.searchMock = lib.mockRequestFromFile('/api/usergroups/search', 'search.json');
        this.updateMock = lib.mockRequestFromFile('/api/usergroups/update', 'error.json', { status: 400 });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/groups/app'], function (App) {
            App.start({ el: '#content' });
          });
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForText('sonar-users');
      })

      .then(function () {
        casper.click('[data-id="2"] .js-group-update');
        casper.waitForSelector('#create-group-form');
      })

      .then(function () {
        casper.click('#create-group-submit');
        casper.waitForSelector('.alert.alert-danger');
      })

      .then(function () {
        lib.clearRequestMock(this.searchMock);
        lib.mockRequestFromFile('/api/usergroups/search', 'search-updated.json');
        lib.clearRequestMock(this.updateMock);
        lib.mockRequest('/api/usergroups/update', '{}',
            { data: { id: '2', name: 'guys', description: 'cool guys' } });
        casper.evaluate(function () {
          jQuery('#create-group-name').val('guys');
          jQuery('#create-group-description').val('cool guys');
        });
        casper.click('#create-group-submit');
        casper.waitForText('guys');
      })

      .then(function () {
        test.assertSelectorContains('[data-id="2"] .js-group-name', 'guys');
        test.assertSelectorContains('[data-id="2"] .js-group-description', 'cool guys');
      })

      .then(function () {
        lib.sendCoverage();
      })
      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Delete'), 1, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();
        this.searchMock = lib.mockRequestFromFile('/api/usergroups/search', 'search.json');
        this.updateMock = lib.mockRequestFromFile('/api/usergroups/delete', 'error.json', { status: 400 });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/groups/app'], function (App) {
            App.start({ el: '#content' });
          });
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForText('sonar-users');
      })

      .then(function () {
        casper.click('[data-id="1"] .js-group-delete');
        casper.waitForSelector('#delete-group-form');
      })

      .then(function () {
        casper.click('#delete-group-submit');
        casper.waitForSelector('.alert.alert-danger');
      })

      .then(function () {
        casper.click('.js-modal-close');
        casper.waitWhileSelector('#delete-group-form');
      })

      .then(function () {
        casper.click('[data-id="1"] .js-group-delete');
        casper.waitForSelector('#delete-group-form');
      })

      .then(function () {
        lib.clearRequestMock(this.updateMock);
        lib.mockRequest('/api/usergroups/delete', '{}', { data: { id: '1'} });
        casper.click('#delete-group-submit');
        casper.waitWhileSelector('[data-id="1"]');
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
