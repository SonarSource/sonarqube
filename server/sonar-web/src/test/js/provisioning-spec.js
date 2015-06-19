/* globals casper: false */
var lib = require('../lib'),
    testName = lib.testName('Provisioning');

lib.initMessages();
lib.changeWorkingDirectory('provisioning-spec');
lib.configureCasper();

casper.test.begin(testName('List'), 5, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();
        lib.mockRequestFromFile('/api/projects/provisioned', 'search.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/provisioning/app'], function (App) {
            App.start({ el: '#content' });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#provisioning-list li');
      })

      .then(function () {
        test.assertElementCount('#provisioning-list li[data-id]', 3);
        test.assertSelectorContains('#provisioning-list .js-project-name', 'SonarQube');
        test.assertSelectorContains('#provisioning-list .js-project-key', 'sonarqube');
        test.assertElementCount('#provisioning-list .js-project-delete', 3);
        test.assertSelectorContains('#provisioning-list-footer', '3/3');
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
        this.searchMock = lib.mockRequestFromFile('/api/projects/provisioned', 'search.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/provisioning/app'], function (App) {
            App.start({ el: '#content' });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#provisioning-list li');
      })

      .then(function () {
        test.assertElementCount('#provisioning-list li[data-id]', 3);
        lib.clearRequestMock(this.searchMock);
        this.searchMock = lib.mockRequestFromFile('/api/projects/provisioned', 'search-filtered.json',
            { data: { q: 'script' } });
        casper.evaluate(function () {
          jQuery('#provisioning-search-query').val('script');
        });
        casper.click('#provisioning-search-submit');
        casper.waitForSelectorTextChange('#provisioning-list-footer');
      })

      .then(function () {
        test.assertElementCount('#provisioning-list li[data-id]', 1);
        lib.clearRequestMock(this.searchMock);
        this.searchMock = lib.mockRequestFromFile('/api/projects/provisioned', 'search.json');
        casper.evaluate(function () {
          jQuery('#provisioning-search-query').val('');
        });
        casper.click('#provisioning-search-submit');
        casper.waitForSelectorTextChange('#provisioning-list-footer');
      })

      .then(function () {
        test.assertElementCount('#provisioning-list li[data-id]', 3);
        test.assert(casper.evaluate(function () {
          return jQuery('#provisioning-search-query').val() === '';
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
        this.searchMock = lib.mockRequestFromFile('/api/projects/provisioned', 'search-big-1.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/provisioning/app'], function (App) {
            App.start({ el: '#content' });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#provisioning-list li');
      })

      .then(function () {
        test.assertElementCount('#provisioning-list li[data-id]', 2);
        test.assertSelectorContains('#provisioning-list-footer', '2/3');
        lib.clearRequestMock(this.searchMock);
        this.searchMock = lib.mockRequestFromFile('/api/projects/provisioned', 'search-big-2.json', { data: { p: '2' } });
        casper.click('#provisioning-fetch-more');
        casper.waitForSelectorTextChange('#provisioning-list-footer');
      })

      .then(function () {
        test.assertElementCount('#provisioning-list li[data-id]', 3);
        test.assertSelectorContains('#provisioning-list-footer', '3/3');
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
        this.searchMock = lib.mockRequestFromFile('/api/projects/provisioned', 'search.json');
        this.createMock = lib.mockRequestFromFile('/api/projects/create', 'error.json', { status: 400 });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/provisioning/app'], function (App) {
            App.start({ el: '#content' });
          });
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('#provisioning-list li');
      })

      .then(function () {
        test.assertElementCount('#provisioning-list li[data-id]', 3);
        casper.click('#provisioning-create');
        casper.waitForSelector('#create-project-form');
      })

      .then(function () {
        casper.click('#create-project-submit');
        casper.waitForSelector('.alert.alert-danger');
      })

      .then(function () {
        lib.clearRequestMock(this.searchMock);
        lib.mockRequestFromFile('/api/projects/provisioned', 'search-created.json');
        lib.clearRequestMock(this.createMock);
        lib.mockRequest('/api/projects/create', '{}',
            { data: { name: 'name', branch: 'branch', key: 'key' } });
        casper.evaluate(function () {
          jQuery('#create-project-name').val('name');
          jQuery('#create-project-branch').val('branch');
          jQuery('#create-project-key').val('key');
        });
        casper.click('#create-project-submit');
        casper.waitForSelectorTextChange('#provisioning-list-footer');
      })

      .then(function () {
        test.assertElementCount('#provisioning-list li[data-id]', 4);
        test.assertSelectorContains('#provisioning-list .js-project-name', 'name');
        test.assertSelectorContains('#provisioning-list .js-project-key', 'key:branch');
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
        this.searchMock = lib.mockRequestFromFile('/api/projects/provisioned', 'search.json');
        this.updateMock = lib.mockRequestFromFile('/api/projects/bulk_delete', 'delete-error.json', { status: 400 });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/provisioning/app'], function (App) {
            App.start({ el: '#content' });
          });
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('#provisioning-list li');
      })

      .then(function () {
        casper.click('[data-id="id-javascript"] .js-project-delete');
        casper.waitForSelector('#delete-project-form');
      })

      .then(function () {
        casper.click('#delete-project-submit');
        casper.waitForSelector('.alert.alert-danger');
      })

      .then(function () {
        lib.clearRequestMock(this.updateMock);
        lib.mockRequest('/api/projects/bulk_delete', '{}', { data: { ids: 'id-javascript'} });
        casper.click('#delete-project-submit');
        casper.waitWhileSelector('[data-id="id-javascript"]');
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


casper.test.begin(testName('Selection'), 22, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();
        lib.mockRequestFromFile('/api/projects/provisioned', 'search.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/provisioning/app'], function (App) {
            App.start({ el: '#content' });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#provisioning-list li');
      })

      .then(function () {
        test.assertExists('.js-toggle-selection');
        test.assertDoesntExist('.js-toggle-selection.icon-checkbox-checked');
        test.assertElementCount('.js-toggle', 3);
        test.assertDoesntExist('.js-toggle.icon-checkbox-checked');
        test.assertExists('#provisioning-bulk-delete[disabled]');
      })

      .then(function () {
        casper.click('#provisioning-list [data-id="id-sonarqube"] .js-toggle');

        test.assertExists('.js-toggle-selection.icon-checkbox-checked.icon-checkbox-single');
        test.assertExists('#provisioning-list [data-id="id-sonarqube"] .js-toggle.icon-checkbox-checked');
        test.assertExists('#provisioning-bulk-delete');
        test.assertDoesntExist('#provisioning-bulk-delete[disabled]');
      })

      .then(function () {
        casper.click('#provisioning-list [data-id="id-javascript"] .js-toggle');
        casper.click('#provisioning-list [data-id="id-sonarqube-release"] .js-toggle');

        test.assertDoesntExist('.js-toggle-selection.icon-checkbox-checked.icon-checkbox-single');
        test.assertExists('.js-toggle-selection.icon-checkbox-checked');
        test.assertExists('#provisioning-bulk-delete');
        test.assertDoesntExist('#provisioning-bulk-delete[disabled]');
      })

      .then(function () {
        casper.click('.js-toggle-selection');

        test.assertDoesntExist('.js-toggle-selection.icon-checkbox-checked');
        test.assertElementCount('.js-toggle', 3);
        test.assertDoesntExist('.js-toggle.icon-checkbox-checked');
        test.assertExists('#provisioning-bulk-delete[disabled]');
      })

      .then(function () {
        casper.click('.js-toggle-selection');

        test.assertDoesntExist('.js-toggle-selection.icon-checkbox-checked.icon-checkbox-single');
        test.assertExists('.js-toggle-selection.icon-checkbox-checked');
        test.assertElementCount('.js-toggle.icon-checkbox-checked', 3);
        test.assertExists('#provisioning-bulk-delete');
        test.assertDoesntExist('#provisioning-bulk-delete[disabled]');
      })

      .then(function () {
        lib.sendCoverage();
      })
      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Bulk Delete'), 1, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();
        lib.mockRequestFromFile('/api/projects/provisioned', 'search.json');
        lib.mockRequestFromFile('/api/projects/bulk_delete', 'delete-error.json', { status: 400 });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/provisioning/app'], function (App) {
            App.start({ el: '#content' });
          });
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('#provisioning-list li');
      })

      .then(function () {
        casper.click('#provisioning-list [data-id="id-sonarqube"] .js-toggle');
        casper.click('#provisioning-list [data-id="id-sonarqube-release"] .js-toggle');
        casper.click('#provisioning-bulk-delete');
        casper.waitForSelector('#bulk-delete-projects-form');
      })

      .then(function () {
        casper.click('#bulk-delete-projects-submit');
        casper.waitForSelector('.alert.alert-danger');
      })

      .then(function () {
        lib.clearRequestMocks();
        lib.mockRequestFromFile('/api/projects/provisioned', 'search-deleted.json');
        lib.mockRequest('/api/projects/bulk_delete', '{}', { data: { ids: 'id-sonarqube,id-sonarqube-release' } });
        casper.click('#bulk-delete-projects-submit');
        casper.waitWhileSelector('[data-id="id-sonarqube"]');
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
