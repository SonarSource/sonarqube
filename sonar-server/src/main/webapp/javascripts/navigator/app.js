/* global _:false, Backbone:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

(function() {

  var NavigatorApp = new Backbone.Marionette.Application();
  window.SS.NavigatorApp = NavigatorApp;



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
        name: 'Project',
        property: 'componentRoots',
        type: window.SS.ProjectFilterView,
        enabled: true,
        optional: false
      }),

      new window.SS.Filter({
        name: 'Severity',
        property: 'severities[]',
        type: window.SS.SelectFilterView,
        enabled: true,
        optional: false,
        choices: {
          'BLOCKER': 'Blocker',
          'CRITICAL': 'Critical',
          'MAJOR': 'Major',
          'MINOR': 'Minor',
          'INFO': 'Info'
        },
        choiceIcons: {
          'BLOCKER': '/images/priority/BLOCKER.png',
          'CRITICAL': '/images/priority/CRITICAL.png',
          'MAJOR': '/images/priority/MAJOR.png',
          'MINOR': '/images/priority/MINOR.png',
          'INFO': '/images/priority/INFO.png'
        }
      }),

      new window.SS.Filter({
        name: 'Status',
        property: 'statuses[]',
        type: window.SS.SelectFilterView,
        enabled: true,
        optional: false,
        choices: {
          'OPEN': 'Open',
          'CONFIRMED': 'Confirmed',
          'REOPENED': 'Reopened',
          'RESOLVED': 'Resolved',
          'CLOSED': 'Closed'
        }
      }),

      new window.SS.Filter({
        name: 'Resolution',
        property: 'resolutions[]',
        type: window.SS.SelectFilterView,
        enabled: false,
        optional: true,
        choices: {
          'FALSE-POSITIVE': 'False positive',
          'FIXED': 'Fixed',
          'REMOVED': 'Removed'
        }
      }),

      new window.SS.Filter({
        name: 'Assignee',
        property: 'assignees',
        type: window.SS.AssigneeFilterView,
        enabled: true,
        optional: false
      }),

      new window.SS.Filter({
        name: 'Reporter',
        property: 'reporters',
        type: window.SS.ReporterFilterView,
        enabled: false,
        optional: true
      }),

      new window.SS.Filter({
        name: 'Created',
        propertyFrom: 'createdAfter',
        propertyTo: 'createdBefore',
        type: window.SS.DateRangeFilterView,
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
