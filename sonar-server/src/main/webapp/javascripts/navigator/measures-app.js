/* global _:false, Backbone:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

(function() {

  var NavigatorApp = new Backbone.Marionette.Application();
  window.SS.MeasuresNavigatorApp = NavigatorApp;



  NavigatorApp.addRegions({
    filtersRegion: '.navigator-filters'
  });


  NavigatorApp.addInitializer(function() {
    this.filters = new window.SS.Filters();

    if (_.isObject(window.SS.favorites)) {
      this.filters.add([
        new window.SS.Filter({
          type: window.SS.FavoriteFilterView,
          enabled: true,
          optional: false,
          choices: window.SS.favorites,
          favoriteUrl: '/measures/filter',
          manageUrl: '/measures/manage'
        })]);
    }

    this.filters.add([
      new window.SS.Filter({
        name: 'Components',
        property: 'qualifiers[]',
        type: window.SS.SelectFilterView,
        enabled: true,
        optional: false,
        choices: {
          'TRK': 'Projects',
          'BRC': 'Sub-projects',
          'DIR': 'Directories',
          'FIL': 'Files',
          'UTS': 'Unit Test Files'
        }
      }),

      new window.SS.Filter({
        name: 'Age',
        propertyFrom: 'ageMinDays',
        propertyTo: 'ageMaxDays',
        type: window.SS.RangeFilterView,
        placeholder: 'in days',
        enabled: false,
        optional: true
      }),

      new window.SS.Filter({
        name: 'Alert',
        property: 'alertLevels[]',
        type: window.SS.SelectFilterView,
        enabled: false,
        optional: true,
        choices: {
          'error': 'Error',
          'warn': 'Warning',
          'ok': 'Ok'
        }
      }),

      new window.SS.Filter({
        name: 'Components of',
        property: 'base',
        type: window.SS.ProjectFilterView,
        multiple: false,
        enabled: false,
        optional: true
      }),

      new window.SS.Filter({
        name: 'Favorites only',
        property: 'onFavourites',
        type: window.SS.CheckboxFilterView,
        enabled: false,
        optional: true
      }),

      new window.SS.Filter({
        name: 'Date',
        propertyFrom: 'fromDate',
        propertyTo: 'toDate',
        type: window.SS.DateRangeFilterView,
        enabled: false,
        optional: true
      }),

      new window.SS.Filter({
        name: 'Key contains',
        property: 'keySearch',
        type: window.SS.StringFilterView,
        enabled: false,
        optional: true
      })
    ]);

    if (_.isObject(window.SS.languages) && _.size(window.SS.languages) > 1) {
      this.filters.add([
        new window.SS.Filter({
          name: 'Language',
          property: 'languages[]',
          type: window.SS.SelectFilterView,
          enabled: false,
          optional: true,
          choices: window.SS.languages
        })
      ]);
    }

    this.filters.add([
      new window.SS.Filter({
        name: 'Name contains',
        property: 'nameSearch',
        type: window.SS.StringFilterView,
        enabled: false,
        optional: true
      }),

      new window.SS.Filter({
        name: 'Metric',
        property: 'c3',
        type: window.SS.MetricFilterView,
        metrics: window.SS.metrics,
        periods: window.SS.metricPeriods,
        operations: { 'eq': '=', 'lt': '<', 'lte': '≤', 'gt': '>', 'gte': '≥' },
        enabled: false,
        optional: true
      }),

      new window.SS.Filter({
        name: 'Metric',
        property: 'c2',
        type: window.SS.MetricFilterView,
        metrics: window.SS.metrics,
        periods: window.SS.metricPeriods,
        operations: { 'eq': '=', 'lt': '<', 'lte': '≤', 'gt': '>', 'gte': '≥' },
        enabled: false,
        optional: true
      }),

      new window.SS.Filter({
        name: 'Metric',
        property: 'c1',
        type: window.SS.MetricFilterView,
        metrics: window.SS.metrics,
        periods: window.SS.metricPeriods,
        operations: { 'eq': '=', 'lt': '<', 'lte': '≤', 'gt': '>', 'gte': '≥' },
        enabled: false,
        optional: true
      })
    ]);


    this.filterBarView = new window.SS.FilterBarView({
      collection: this.filters,
      extra: {
        sort: '',
        asc: false
      }
    });


    this.filtersRegion.show(this.filterBarView);
  });

})();
