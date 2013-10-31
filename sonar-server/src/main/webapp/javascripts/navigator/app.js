/* global $j:false, Backbone:false */

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
          name: 'Project',
          property: 'componentRoots',
          type: window.SS.AjaxSelectFilterView,
          select2 : {
            allowClear: true,
            ajax: {
              quietMillis: 300,
              url: '/dev/api/resources/search?f=s2&q=TRK&display_key=true',
              data: function (term, page) { return { s: term, p: page }; },
              results: function (data) { return { more: data.more, results: data.results }; }
            }
          }
        }),

        new window.SS.Filter({
          name: 'Severity',
          property: 'severities[]',
          type: window.SS.SelectFilterView,
          choices: window.SS.severities
        }),

        new window.SS.Filter({
          name: 'Status',
          property: 'statuses[]',
          type: window.SS.SelectFilterView,
          choices: window.SS.statuses
        }),

        new window.SS.Filter({
          name: 'Resolution',
          property: 'resolutions[]',
          type: window.SS.SelectFilterView,
          choices: window.SS.resolutions
        }),

        new window.SS.Filter({
          name: 'Assignee',
          property: 'assignees',
          type: window.SS.AjaxSelectFilterView,
          select2: {
            allowClear: true,
            query:
                function(query) {
                  if (query.term.length === 0) {
                    query.callback({results: [{id:'<unassigned>',text:'Unassigned'}]});
                  } else if (query.term.length >= 2) {
                    $j.ajax('/dev/api/users/search?f=s2', {
                      data: {s: query.term},
                      dataType: 'jsonp'
                    }).done(function(data) {
                          query.callback(data);
                        });
                  }
                }
          }
        }),

        new window.SS.Filter({
          name: 'Created',
          propertyFrom: 'createdAfter',
          propertyTo: 'createdBefore',
          type: window.SS.RangeFilterView
        })
    ]);


    this.filterBarView = new window.SS.FilterBarView({
      collection: this.filters
    });

    this.filtersRegion.show(this.filterBarView);
  });

})();
