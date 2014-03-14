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
        name: window.SS.phrases.components,
        property: 'qualifiers[]',
        type: window.SS.SelectFilterView,
        enabled: true,
        optional: false,
        choices: window.SS.qualifiers,
        defaultValue: window.SS.phrases.any
      }),

      new window.SS.Filter({
        name: window.SS.phrases.alert,
        property: 'alertLevels[]',
        type: window.SS.SelectFilterView,
        enabled: false,
        optional: true,
        choices: {
          'error': window.SS.phrases.error,
          'warn': window.SS.phrases.warning,
          'ok': window.SS.phrases.ok
        }
      }),

      new window.SS.Filter({
        name: window.SS.phrases.componentsOf,
        property: 'base',
        type: window.SS.ComponentFilterView,
        multiple: false,
        enabled: false,
        optional: true
      }),

      new window.SS.Filter({
        name: window.SS.phrases.favoritesOnly,
        property: 'onFavourites',
        type: window.SS.CheckboxFilterView,
        enabled: false,
        optional: true
      }),

      new window.SS.Filter({
        name: window.SS.phrases.date,
        propertyFrom: 'fromDate',
        propertyTo: 'toDate',
        type: window.SS.DateRangeFilterView,
        enabled: false,
        optional: true
      }),

      new window.SS.Filter({
        name: window.SS.phrases.keyContains,
        property: 'keySearch',
        type: window.SS.StringFilterView,
        enabled: false,
        optional: true
      })
    ]);

    this.filters.add([
      new window.SS.Filter({
        name: window.SS.phrases.lastAnalysis,
        propertyFrom: 'ageMinDays',
        propertyTo: 'ageMaxDays',
        type: window.SS.RangeFilterView,
        placeholder: window.SS.phrases.days,
        enabled: false,
        optional: true
      }),

      new window.SS.Filter({
        name: window.SS.phrases.metric,
        property: 'c3',
        type: window.SS.MetricFilterView,
        metrics: window.SS.metrics,
        periods: window.SS.metricPeriods,
        operations: { 'eq': '=', 'lt': '<', 'lte': '≤', 'gt': '>', 'gte': '≥' },
        enabled: false,
        optional: true
      }),

      new window.SS.Filter({
        name: window.SS.phrases.metric,
        property: 'c2',
        type: window.SS.MetricFilterView,
        metrics: window.SS.metrics,
        periods: window.SS.metricPeriods,
        operations: { 'eq': '=', 'lt': '<', 'lte': '≤', 'gt': '>', 'gte': '≥' },
        enabled: false,
        optional: true
      }),

      new window.SS.Filter({
        name: window.SS.phrases.metric,
        property: 'c1',
        type: window.SS.MetricFilterView,
        metrics: window.SS.metrics,
        periods: window.SS.metricPeriods,
        operations: { 'eq': '=', 'lt': '<', 'lte': '≤', 'gt': '>', 'gte': '≥' },
        enabled: false,
        optional: true
      }),

      new window.SS.Filter({
        name: window.SS.phrases.nameContains,
        property: 'nameSearch',
        type: window.SS.StringFilterView,
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
