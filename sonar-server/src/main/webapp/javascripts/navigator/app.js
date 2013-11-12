/* global $j:false, Backbone:false, baseUrl:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

(function() {

  var NavigatorApp = new Backbone.Marionette.Application();
  window.SS.NavigatorApp = NavigatorApp;



  NavigatorApp.addRegions({
    filtersRegion: '.navigator-filters'
  });



  NavigatorApp.addInitializer(function() {
    this.filters = new window.SS.Filters([
      new window.SS.Filter({
        type: window.SS.FavoriteFilterView,
        enabled: true,
        choices: window.SS.favorites
      }),

        new window.SS.Filter({ 
          name: 'Project',
          property: 'componentRoots',
          type: window.SS.ProjectFilterView,
          enabled: true
        }),

        new window.SS.Filter({
          name: 'Severity',
          property: 'severities[]',
          type: window.SS.SelectFilterView,
          enabled: true,
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
          enabled: true,
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
          enabled: true//,
//          select2: {
//            allowClear: true,
//            query:
//                function(query) {
//                  if (query.term.length === 0) {
//                    query.callback({results: [{id:'<unassigned>',text:'Unassigned'}]});
//                  } else if (query.term.length >= 2) {
//                    $j.ajax(baseUrl + '/api/users/search?f=s2', {
//                      data: {s: query.term},
//                      dataType: 'jsonp'
//                    }).done(function(data) {
//                          query.callback(data);
//                        });
//                  }
//                }
//          }
        }),

        new window.SS.Filter({
          name: 'Created',
          propertyFrom: 'createdAfter',
          propertyTo: 'createdBefore',
          type: window.SS.DateRangeFilterView,
          enabled: true
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
