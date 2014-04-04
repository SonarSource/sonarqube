requirejs.config({
  baseUrl: baseUrl + '/js',

  paths: {
    'backbone': 'third-party/backbone',
    'backbone.marionette': 'third-party/backbone.marionette',
    'handlebars': 'third-party/handlebars'
  },

  shim: {
    'backbone.marionette': {
      deps: ['backbone'],
      exports: 'Marionette'
    },
    'backbone': {
      exports: 'Backbone'
    },
    'handlebars': {
      exports: 'Handlebars'
    }
  }

});

requirejs(
    [
      'backbone', 'backbone.marionette',
      'navigator/filters/filter-bar',
      'navigator/filters/base-filters',
      'navigator/filters/checkbox-filters',
      'navigator/filters/choice-filters',
      'navigator/filters/ajax-select-filters',
      'navigator/filters/favorite-filters',
      'navigator/filters/range-filters',
      'navigator/filters/string-filters',
      'navigator/filters/metric-filters'
    ],
    function (Backbone, Marionette, FilterBar, BaseFilters, CheckboxFilterView, ChoiceFilters, AjaxSelectFilters,
              FavoriteFilters, RangeFilters, StringFilterView, MetricFilterView) {

      var NavigatorApp = new Marionette.Application();


      NavigatorApp.addRegions({
        filtersRegion: '.navigator-filters'
      });


      NavigatorApp.addInitializer(function () {
        this.filters = new BaseFilters.Filters();

        if (_.isObject(window.SS.favorites)) {
          this.filters.add([
            new BaseFilters.Filter({
              type: FavoriteFilters.FavoriteFilterView,
              enabled: true,
              optional: false,
              choices: window.SS.favorites,
              favoriteUrl: '/measures/filter',
              manageUrl: '/measures/manage'
            })]);
        }

        this.filters.add([
          new BaseFilters.Filter({
            name: window.SS.phrases.components,
            property: 'qualifiers[]',
            type: ChoiceFilters.ChoiceFilterView,
            enabled: true,
            optional: false,
            choices: window.SS.qualifiers,
            defaultValue: window.SS.phrases.any
          }),

          new BaseFilters.Filter({
            name: window.SS.phrases.alert,
            property: 'alertLevels[]',
            type: ChoiceFilters.ChoiceFilterView,
            enabled: false,
            optional: true,
            choices: {
              'error': window.SS.phrases.error,
              'warn': window.SS.phrases.warning,
              'ok': window.SS.phrases.ok
            }
          }),

          new BaseFilters.Filter({
            name: window.SS.phrases.componentsOf,
            property: 'base',
            type: AjaxSelectFilters.ComponentFilterView,
            multiple: false,
            enabled: false,
            optional: true
          }),

          new BaseFilters.Filter({
            name: window.SS.phrases.favoritesOnly,
            property: 'onFavourites',
            type: CheckboxFilterView,
            enabled: false,
            optional: true
          }),

          new BaseFilters.Filter({
            name: window.SS.phrases.date,
            propertyFrom: 'fromDate',
            propertyTo: 'toDate',
            type: RangeFilters.DateRangeFilterView,
            enabled: false,
            optional: true
          }),

          new BaseFilters.Filter({
            name: window.SS.phrases.keyContains,
            property: 'keySearch',
            type: StringFilterView,
            enabled: false,
            optional: true
          })
        ]);

        this.filters.add([
          new BaseFilters.Filter({
            name: window.SS.phrases.lastAnalysis,
            propertyFrom: 'ageMinDays',
            propertyTo: 'ageMaxDays',
            type: RangeFilters.RangeFilterView,
            placeholder: window.SS.phrases.days,
            enabled: false,
            optional: true
          }),

          new BaseFilters.Filter({
            name: window.SS.phrases.metric,
            property: 'c3',
            type: MetricFilterView,
            metrics: window.SS.metrics,
            periods: window.SS.metricPeriods,
            operations: { 'eq': '=', 'lt': '<', 'lte': '≤', 'gt': '>', 'gte': '≥' },
            enabled: false,
            optional: true
          }),

          new BaseFilters.Filter({
            name: window.SS.phrases.metric,
            property: 'c2',
            type: MetricFilterView,
            metrics: window.SS.metrics,
            periods: window.SS.metricPeriods,
            operations: { 'eq': '=', 'lt': '<', 'lte': '≤', 'gt': '>', 'gte': '≥' },
            enabled: false,
            optional: true
          }),

          new BaseFilters.Filter({
            name: window.SS.phrases.metric,
            property: 'c1',
            type: MetricFilterView,
            metrics: window.SS.metrics,
            periods: window.SS.metricPeriods,
            operations: { 'eq': '=', 'lt': '<', 'lte': '≤', 'gt': '>', 'gte': '≥' },
            enabled: false,
            optional: true
          }),

          new BaseFilters.Filter({
            name: window.SS.phrases.nameContains,
            property: 'nameSearch',
            type: StringFilterView,
            enabled: false,
            optional: true
          })
        ]);


        this.filterBarView = new FilterBar({
          collection: this.filters,
          extra: {
            sort: '',
            asc: false
          }
        });


        this.filtersRegion.show(this.filterBarView);
      });


      NavigatorApp.start();
      if (window.queryParams) {
        NavigatorApp.filterBarView.restoreFromQuery(window.queryParams);
      }
      key.setScope('list');

    });
