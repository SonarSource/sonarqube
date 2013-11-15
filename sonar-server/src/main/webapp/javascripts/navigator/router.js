/* global Backbone:false, _:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

(function() {

  var NavigatorRouter = Backbone.Router.extend({

    routes: {
      'filter=:query': 'filter',
      '': 'filter'
    },


    filter: function(query) {
      var filterJSON = JSON.parse(decodeURIComponent(query || '{}'));

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
        restoreData: filterJSON,
        extra: {
          sort: '',
          asc: false
        }
      });


      window.SS.NavigatorApp.filtersRegion.show(this.filterBarView);

    }

  });



  /*
   * Export public classes
   */

  _.extend(window.SS, {
    NavigatorRouter: NavigatorRouter
  });

})();
