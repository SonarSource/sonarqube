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
          choices: window.SS.favorites
        })]);
    }

    this.filters.add([
      new window.SS.Filter({
        name: 'Qualifiers',
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
