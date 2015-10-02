define(function (require) {
  var bdd = require('intern!bdd');
  require('../helpers/test-page');

  bdd.describe('Custom Metrics Page', function () {
    bdd.it('should show list', function () {
      return this.remote
          .open()
          .mockFromFile('/api/metrics/domains', 'metrics-spec/domains.json')
          .mockFromFile('/api/metrics/types', 'metrics-spec/types.json')
          .mockFromFile('/api/metrics/search', 'metrics-spec/search.json')
          .startAppBrowserify('metrics')
          .checkElementExist('#metrics-list li')
          .checkElementCount('#metrics-list li[data-id]', 3)
          .checkElementInclude('#metrics-list .js-metric-name', 'Business value')
          .checkElementInclude('#metrics-list .js-metric-key', 'business_value')
          .checkElementInclude('#metrics-list .js-metric-domain', 'Complexity')
          .checkElementInclude('#metrics-list .js-metric-type', 'PERCENT')
          .checkElementInclude('#metrics-list .js-metric-description', 'Description of Business value')
          .checkElementCount('#metrics-list .js-metric-update', 3)
          .checkElementCount('#metrics-list .js-metric-delete', 3)
          .checkElementInclude('#metrics-list-footer', '3/3');
    });

    bdd.it('should show more', function () {
      return this.remote
          .open()
          .mockFromFile('/api/metrics/domains', 'metrics-spec/domains.json')
          .mockFromFile('/api/metrics/types', 'metrics-spec/types.json')
          .mockFromFile('/api/metrics/search', 'metrics-spec/search-big-1.json')
          .startAppBrowserify('metrics')
          .checkElementExist('#metrics-list li')
          .checkElementCount('#metrics-list li[data-id]', 2)
          .checkElementInclude('#metrics-list-footer', '2/3')
          .clearMocks()
          .mockFromFile('/api/metrics/search', 'metrics-spec/search-big-2.json', { data: { p: 2 } })
          .clickElement('#metrics-fetch-more')
          .checkElementInclude('#metrics-list-footer', '3/3')
          .checkElementCount('#metrics-list li[data-id]', 3);
    });

    bdd.it('should create custom metric', function () {
      return this.remote
          .open()
          .mockFromFile('/api/metrics/domains', 'metrics-spec/domains.json')
          .mockFromFile('/api/metrics/types', 'metrics-spec/types.json')
          .mockFromFile('/api/metrics/search', 'metrics-spec/search.json')
          .mockFromFile('/api/metrics/create', 'metrics-spec/error.json', { status: 400 })
          .startAppBrowserify('metrics')
          .checkElementExist('#metrics-list li')
          .checkElementCount('#metrics-list li[data-id]', 3)
          .clickElement('#metrics-create')
          .checkElementExist('#create-metric-form')
          .fillElement('#create-metric-key', 'new_metric')
          .fillElement('#create-metric-name', 'New Metric')
          .fillElement('#create-metric-domain', 'Domain for New Metric')
          .fillElement('#create-metric-type', 'RATING')
          .clickElement('#create-metric-submit')
          .checkElementExist('.alert.alert-danger')
          .clearMocks()
          .mockFromFile('/api/metrics/search', 'metrics-spec/search-created.json')
          .mockFromString('/api/metrics/create', '{}',
          { data: { key: 'new_metric', name: 'New Metric', domain: 'Domain for New Metric', type: 'RATING' } })
          .fillElement('#create-metric-key', 'new_metric')
          .fillElement('#create-metric-name', 'New Metric')
          .fillElement('#create-metric-domain', 'Domain for New Metric')
          .fillElement('#create-metric-type', 'RATING')
          .clickElement('#create-metric-submit')
          .checkElementCount('#metrics-list li[data-id]', 4)
          .checkElementInclude('#metrics-list .js-metric-key', 'new_metric')
          .checkElementInclude('#metrics-list .js-metric-name', 'New Metric');
    });

    bdd.it('should update custom metric', function () {
      return this.remote
          .open()
          .mockFromFile('/api/metrics/domains', 'metrics-spec/domains.json')
          .mockFromFile('/api/metrics/types', 'metrics-spec/types.json')
          .mockFromFile('/api/metrics/search', 'metrics-spec/search.json')
          .mockFromFile('/api/metrics/update', 'metrics-spec/error.json', { status: 400 })
          .startAppBrowserify('metrics')
          .checkElementExist('#metrics-list li')
          .clickElement('[data-id="3"] .js-metric-update')
          .checkElementExist('#create-metric-form')
          .fillElement('#create-metric-key', 'updated_key')
          .fillElement('#create-metric-name', 'Updated Name')
          .fillElement('#create-metric-domain', 'Random Domain')
          .fillElement('#create-metric-type', 'WORK_DUR')
          .clickElement('#create-metric-submit')
          .checkElementExist('.alert.alert-danger')
          .clearMocks()
          .mockFromFile('/api/metrics/search', 'metrics-spec/search-updated.json')
          .mockFromString('/api/metrics/update', '{}',
          { data: { id: '3', key: 'updated_key', name: 'Updated Name', domain: 'Random Domain', type: 'WORK_DUR' } })
          .fillElement('#create-metric-key', 'updated_key')
          .fillElement('#create-metric-name', 'Updated Name')
          .fillElement('#create-metric-domain', 'Random Domain')
          .fillElement('#create-metric-type', 'WORK_DUR')
          .clickElement('#create-metric-submit')
          .checkElementInclude('#metrics-list [data-id="3"] .js-metric-key', 'updated_key')
          .checkElementInclude('#metrics-list [data-id="3"] .js-metric-name', 'Updated Name')
          .checkElementInclude('#metrics-list [data-id="3"] .js-metric-domain', 'Random Domain')
          .checkElementInclude('#metrics-list [data-id="3"] .js-metric-type', 'WORK_DUR');
    });

    bdd.it('should delete custom metric', function () {
      return this.remote
          .open()
          .mockFromFile('/api/metrics/domains', 'metrics-spec/domains.json')
          .mockFromFile('/api/metrics/types', 'metrics-spec/types.json')
          .mockFromFile('/api/metrics/search', 'metrics-spec/search.json')
          .mockFromFile('/api/metrics/delete', 'metrics-spec/error.json', { status: 400 })
          .startAppBrowserify('metrics')
          .checkElementExist('#metrics-list li')
          .clickElement('[data-id="3"] .js-metric-delete')
          .checkElementExist('#delete-metric-form')
          .clickElement('#delete-metric-submit')
          .checkElementExist('.alert.alert-danger')
          .clearMocks()
          .mockFromString('/api/metrics/delete', '{}', { data: { ids: '3' } })
          .clickElement('#delete-metric-submit')
          .checkElementNotExist('[data-id="3"]');
    });
  });
});
