/* globals casper: false */
var lib = require('../lib'),
    testName = lib.testName('Metrics');

lib.initMessages();
lib.changeWorkingDirectory('metrics-spec');
lib.configureCasper();

casper.test.begin(testName('List'), 9, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();
        lib.mockRequestFromFile('/api/metrics/domains', 'domains.json');
        lib.mockRequestFromFile('/api/metrics/types', 'types.json');
        lib.mockRequestFromFile('/api/metrics/search', 'search.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/metrics/app'], function (App) {
            App.start({ el: '#content' });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#metrics-list li');
      })

      .then(function () {
        test.assertElementCount('#metrics-list li[data-id]', 3);
        test.assertSelectorContains('#metrics-list .js-metric-name', 'Business value');
        test.assertSelectorContains('#metrics-list .js-metric-key', 'business_value');
        test.assertSelectorContains('#metrics-list .js-metric-domain', 'Complexity');
        test.assertSelectorContains('#metrics-list .js-metric-type', 'PERCENT');
        test.assertSelectorContains('#metrics-list .js-metric-description', 'Description of Business value');
        test.assertElementCount('#metrics-list .js-metric-update', 3);
        test.assertElementCount('#metrics-list .js-metric-delete', 3);
        test.assertSelectorContains('#metrics-list-footer', '3/3');
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
        lib.mockRequestFromFile('/api/metrics/domains', 'domains.json');
        lib.mockRequestFromFile('/api/metrics/types', 'types.json');
        this.searchMock = lib.mockRequestFromFile('/api/metrics/search', 'search-big-1.json');
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/metrics/app'], function (App) {
            App.start({ el: '#content' });
          });
        });
      })

      .then(function () {
        casper.waitForSelector('#metrics-list li');
      })

      .then(function () {
        test.assertElementCount('#metrics-list li[data-id]', 2);
        test.assertSelectorContains('#metrics-list-footer', '2/3');
        lib.clearRequestMock(this.searchMock);
        this.searchMock = lib.mockRequestFromFile('/api/metrics/search', 'search-big-2.json', { data: { p: '2' } });
        casper.click('#metrics-fetch-more');
        casper.waitForSelectorTextChange('#metrics-list-footer');
      })

      .then(function () {
        test.assertElementCount('#metrics-list li[data-id]', 3);
        test.assertSelectorContains('#metrics-list-footer', '3/3');
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
        lib.mockRequestFromFile('/api/metrics/domains', 'domains.json');
        lib.mockRequestFromFile('/api/metrics/types', 'types.json');
        this.searchMock = lib.mockRequestFromFile('/api/metrics/search', 'search.json');
        this.createMock = lib.mockRequestFromFile('/api/metrics/create', 'error.json', { status: 400 });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/metrics/app'], function (App) {
            App.start({ el: '#content' });
          });
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('#metrics-list li');
      })

      .then(function () {
        test.assertElementCount('#metrics-list li[data-id]', 3);
        casper.click('#metrics-create');
        casper.waitForSelector('#create-metric-form');
      })

      .then(function () {
        casper.click('#create-metric-submit');
        casper.waitForSelector('.alert.alert-danger');
      })

      .then(function () {
        lib.clearRequestMock(this.searchMock);
        lib.mockRequestFromFile('/api/metrics/search', 'search-created.json');
        lib.clearRequestMock(this.createMock);
        lib.mockRequest('/api/metrics/create', '{}',
            { data: { key: 'new_metric', name: 'New Metric', domain: 'Domain for New Metric', type: 'RATING' } });
        casper.evaluate(function () {
          jQuery('#create-metric-key').val('new_metric');
          jQuery('#create-metric-name').val('New Metric');
          jQuery('#create-metric-domain').val('Domain for New Metric');
          jQuery('#create-metric-type').val('RATING');
        });
        casper.click('#create-metric-submit');
        casper.waitForSelectorTextChange('#metrics-list-footer');
      })

      .then(function () {
        test.assertElementCount('#metrics-list li[data-id]', 4);
        test.assertSelectorContains('#metrics-list .js-metric-key', 'new_metric');
        test.assertSelectorContains('#metrics-list .js-metric-name', 'New Metric');
      })

      .then(function () {
        lib.sendCoverage();
      })
      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Update'), 4, function (test) {
  casper
      .start(lib.buildUrl('base'), function () {
        lib.setDefaultViewport();
        lib.mockRequestFromFile('/api/metrics/domains', 'domains.json');
        lib.mockRequestFromFile('/api/metrics/types', 'types.json');
        this.searchMock = lib.mockRequestFromFile('/api/metrics/search', 'search.json');
        this.updateMock = lib.mockRequestFromFile('/api/metrics/update', 'error.json', { status: 400 });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/metrics/app'], function (App) {
            App.start({ el: '#content' });
          });
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('#metrics-list li');
      })

      .then(function () {
        casper.click('[data-id="3"] .js-metric-update');
        casper.waitForSelector('#create-metric-form');
      })

      .then(function () {
        casper.click('#create-metric-submit');
        casper.waitForSelector('.alert.alert-danger');
      })

      .then(function () {
        lib.clearRequestMock(this.searchMock);
        lib.mockRequestFromFile('/api/metrics/search', 'search-updated.json');
        lib.clearRequestMock(this.createMock);
        lib.mockRequest('/api/metrics/update', '{}',
            { data: { id: '3', key: 'updated_key', name: 'Updated Name', domain: 'Random Domain', type: 'WORK_DUR' } });
        casper.evaluate(function () {
          jQuery('#create-metric-key').val('updated_key');
          jQuery('#create-metric-name').val('Updated Name');
          jQuery('#create-metric-domain').val('Random Domain');
          jQuery('#create-metric-type').val('WORK_DUR');
        });
        casper.click('#create-metric-submit');
        casper.waitForSelectorTextChange('#metrics-list [data-id="3"] .js-metric-name');
      })

      .then(function () {
        test.assertSelectorContains('#metrics-list [data-id="3"] .js-metric-key', 'updated_key');
        test.assertSelectorContains('#metrics-list [data-id="3"] .js-metric-name', 'Updated Name');
        test.assertSelectorContains('#metrics-list [data-id="3"] .js-metric-domain', 'Random Domain');
        test.assertSelectorContains('#metrics-list [data-id="3"] .js-metric-type', 'WORK_DUR');
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
        lib.mockRequestFromFile('/api/metrics/domains', 'domains.json');
        lib.mockRequestFromFile('/api/metrics/types', 'types.json');
        this.searchMock = lib.mockRequestFromFile('/api/metrics/search', 'search.json');
        this.updateMock = lib.mockRequestFromFile('/api/metrics/delete', 'error.json', { status: 400 });
      })

      .then(function () {
        casper.evaluate(function () {
          require(['apps/metrics/app'], function (App) {
            App.start({ el: '#content' });
          });
          jQuery.ajaxSetup({ dataType: 'json' });
        });
      })

      .then(function () {
        casper.waitForSelector('#metrics-list li');
      })

      .then(function () {
        casper.click('[data-id="3"] .js-metric-delete');
        casper.waitForSelector('#delete-metric-form');
      })

      .then(function () {
        casper.click('#delete-metric-submit');
        casper.waitForSelector('.alert.alert-danger');
      })

      .then(function () {
        lib.clearRequestMock(this.updateMock);
        lib.mockRequest('/api/metrics/delete', '{}', { data: { ids: '3'} });
        casper.click('#delete-metric-submit');
        casper.waitWhileSelector('[data-id="3"]');
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

