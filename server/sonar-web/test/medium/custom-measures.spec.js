define(function (require) {
  var bdd = require('intern!bdd');
  require('../helpers/test-page');

  bdd.describe('Custom Measures Page', function () {
    var projectId = 'eb294572-a6a4-43cf-acc2-33c2fe37c02e';

    bdd.it('should show list', function () {
      return this.remote
          .open()
          .mockFromFile('/api/custom_measures/search', 'custom-measures-spec/search.json',
          { data: { projectId: projectId } })
          .startAppBrowserify('custom-measures')
          .checkElementCount('#custom-measures-list tr[data-id]', 4)
          .checkElementInclude('#custom-measures-list .js-custom-measure-value', '35')
          .checkElementInclude('#custom-measures-list .js-custom-measure-metric-name', 'Distribution')
          .checkElementInclude('#custom-measures-list .js-custom-measure-domain', 'Management')
          .checkElementInclude('#custom-measures-list .js-custom-measure-description', 'Description...')
          .checkElementInclude('#custom-measures-list .js-custom-measure-created-at', '2015')
          .checkElementInclude('#custom-measures-list .js-custom-measure-user', 'Administrator')
          .checkElementCount('#custom-measures-list .js-custom-measure-pending', 4)
          .checkElementCount('#custom-measures-list .js-custom-measure-update', 4)
          .checkElementCount('#custom-measures-list .js-custom-measure-delete', 4)
          .checkElementInclude('#custom-measures-list-footer', '4/4');
    });

    bdd.it('should show more', function () {
      return this.remote
          .open()
          .mockFromFile('/api/custom_measures/search', 'custom-measures-spec/search-big-1.json',
          { data: { projectId: projectId } })
          .startAppBrowserify('custom-measures')
          .checkElementCount('#custom-measures-list tr[data-id]', 2)
          .checkElementNotExist('[data-id="3"]')
          .clearMocks()
          .mockFromFile('/api/custom_measures/search', 'custom-measures-spec/search-big-2.json',
          { data: { projectId: projectId, p: 2 } })
          .clickElement('#custom-measures-fetch-more')
          .checkElementExist('[data-id="3"]')
          .checkElementCount('#custom-measures-list tr[data-id]', 4);
    });

    bdd.it('should create a new custom measure', function () {
      return this.remote
          .open()
          .mockFromFile('/api/custom_measures/search', 'custom-measures-spec/search.json',
          { data: { projectId: projectId } })
          .mockFromFile('/api/metrics/search', 'custom-measures-spec/metrics.json', { data: { isCustom: true } })
          .startAppBrowserify('custom-measures')
          .checkElementCount('#custom-measures-list tr[data-id]', 4)
          .clickElement('#custom-measures-create')
          .checkElementExist('#create-custom-measure-form')
          .clearMocks()
          .mockFromFile('/api/custom_measures/search', 'custom-measures-spec/search-created.json',
          { data: { projectId: projectId } })
          .mockFromString('/api/custom_measures/create', '{}', { data: {
            metricId: '156',
            value: '17',
            description: 'example',
            projectId: projectId
          } })
          .fillElement('#create-custom-measure-metric', '156')
          .fillElement('#create-custom-measure-value', '17')
          .fillElement('#create-custom-measure-description', 'example')
          .clickElement('#create-custom-measure-submit')
          .checkElementExist('[data-id="6"]')
          .checkElementCount('#custom-measures-list tr[data-id]', 5)
          .checkElementInclude('[data-id="6"] .js-custom-measure-value', '17');
    });

    bdd.it('should filter available metrics', function () {
      return this.remote
          .open()
          .mockFromFile('/api/custom_measures/search', 'custom-measures-spec/search.json',
          { data: { projectId: projectId } })
          .mockFromFile('/api/metrics/search', 'custom-measures-spec/metrics.json', { data: { isCustom: true } })
          .startAppBrowserify('custom-measures')
          .clickElement('#custom-measures-create')
          .checkElementExist('#create-custom-measure-form')
          .checkElementCount('#create-custom-measure-metric option', 1)
          .checkElementExist('#create-custom-measure-metric option[value="156"]');
    });

    bdd.it('should show warning when there are no available metrics', function () {
      return this.remote
          .open()
          .mockFromFile('/api/custom_measures/search', 'custom-measures-spec/search.json',
          { data: { projectId: projectId } })
          .mockFromFile('/api/metrics/search', 'custom-measures-spec/metrics-limited.json',
          { data: { isCustom: true } })
          .startAppBrowserify('custom-measures')
          .clickElement('#custom-measures-create')
          .checkElementExist('#create-custom-measure-form')
          .checkElementNotExist('#create-custom-measure-metric')
          .checkElementExist('#create-custom-measure-form .alert-warning')
          .checkElementExist('#create-custom-measure-submit[disabled]');
    });

    bdd.it('should update a custom measure', function () {
      return this.remote
          .open()
          .mockFromFile('/api/custom_measures/search', 'custom-measures-spec/search.json',
          { data: { projectId: projectId } })
          .mockFromFile('/api/metrics/search', 'custom-measures-spec/metrics.json', { data: { isCustom: true } })
          .startAppBrowserify('custom-measures')
          .clickElement('[data-id="5"] .js-custom-measure-update')
          .checkElementExist('#create-custom-measure-form')
          .clearMocks()
          .mockFromFile('/api/custom_measures/search', 'custom-measures-spec/search-updated.json',
          { data: { projectId: projectId } })
          .mockFromString('/api/custom_measures/update', '{}', { data: {
            id: '5',
            value: '2',
            description: 'new!'
          } })
          .fillElement('#create-custom-measure-value', '2')
          .fillElement('#create-custom-measure-description', 'new!')
          .clickElement('#create-custom-measure-submit')
          .checkElementExist('[data-id="5"]')
          .checkElementInclude('[data-id="5"] .js-custom-measure-value', 'B')
          .checkElementInclude('[data-id="5"] .js-custom-measure-description', 'new!');
    });

    bdd.it('should delete a custom measure', function () {
      return this.remote
          .open()
          .mockFromFile('/api/custom_measures/search', 'custom-measures-spec/search.json',
          { data: { projectId: projectId } })
          .startAppBrowserify('custom-measures')
          .clickElement('[data-id="5"] .js-custom-measure-delete')
          .checkElementExist('#delete-custom-measure-form', 1)
          .clearMocks()
          .mockFromFile('/api/custom_measures/search', 'custom-measures-spec/search-deleted.json',
          { data: { projectId: projectId } })
          .mockFromString('/api/custom_measures/delete', '{}', { data: { id: '5' } })
          .clickElement('#delete-custom-measure-submit')
          .checkElementNotExist('[data-id="5"]');
    });
  });

});
