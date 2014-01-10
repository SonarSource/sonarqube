/* global _:false, Backbone:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

(function() {

  var NavigatorApp = new Backbone.Marionette.Application();
  window.SS.IssuesNavigatorAppOld = NavigatorApp;



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
          favoriteUrl: '/issues/filter',
          manageUrl: '/issues/manage'
        })]);
    }

    this.filters.add([
      new window.SS.Filter({
        name: window.SS.phrases.project,
        property: 'componentRoots',
        type: window.SS.ProjectFilterView,
        enabled: true,
        optional: false
      }),

      new window.SS.Filter({
        name: window.SS.phrases.severity,
        property: 'severities[]',
        type: window.SS.SelectFilterView,
        enabled: true,
        optional: false,
        choices: {
          'BLOCKER': window.SS.phrases.severities.blocker,
          'CRITICAL': window.SS.phrases.severities.critical,
          'MAJOR': window.SS.phrases.severities.major,
          'MINOR': window.SS.phrases.severities.minor,
          'INFO': window.SS.phrases.severities.info
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
        name: window.SS.phrases.status,
        property: 'statuses[]',
        type: window.SS.SelectFilterView,
        enabled: true,
        optional: false,
        choices: {
          'OPEN': window.SS.phrases.statuses.open,
          'CONFIRMED': window.SS.phrases.statuses.confirmed,
          'REOPENED': window.SS.phrases.statuses.reopened,
          'RESOLVED': window.SS.phrases.statuses.resolved,
          'CLOSED': window.SS.phrases.statuses.closed
        }
      }),

      new window.SS.Filter({
        name: window.SS.phrases.assignee,
        property: 'assignees',
        type: window.SS.AssigneeFilterView,
        enabled: true,
        optional: false
      }),

      new window.SS.Filter({
        name: window.SS.phrases.resolution,
        property: 'resolutions[]',
        type: window.SS.SelectFilterView,
        enabled: false,
        optional: true,
        choices: {
          'FALSE-POSITIVE': window.SS.phrases.resolutions.falsePositive,
          'FIXED': window.SS.phrases.resolutions.fixed,
          'REMOVED': window.SS.phrases.resolutions.removed
        }
      }),

      new window.SS.Filter({
        name: window.SS.phrases.reporter,
        property: 'reporters',
        type: window.SS.ReporterFilterView,
        enabled: false,
        optional: true
      }),

      new window.SS.Filter({
        name: window.SS.phrases.created,
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
