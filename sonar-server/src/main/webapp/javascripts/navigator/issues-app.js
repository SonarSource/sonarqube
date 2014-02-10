/* global _:false, Backbone:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

jQuery(function() {

  var NavigatorApp = new Backbone.Marionette.Application();
  window.SS.IssuesNavigatorApp = NavigatorApp;


  NavigatorApp.addRegions({
    headerRegion: '.navigator-header',
    filtersRegion: '.navigator-filters',
    resultsRegion: '.navigator-results',
    actionsRegion: '.navigator-actions',
    detailsRegion: '.navigator-details'
  });


  NavigatorApp.addInitializer(function() {
    jQuery('html').addClass('issues-page');

    this.appState = new window.SS.AppState();
    window.SS.appState = this.appState;

    this.state = new Backbone.Model({
      query: ''
    });

    this.issues = new window.SS.Issues();
    this.issues.sorting = {
      sort: 'UPDATE_DATE',
      asc: false
    };
    this.issuesPage = 1;

    this.filters = new window.SS.Filters();

    this.favoriteFilter = new window.SS.FavoriteFilter();
    this.issuesHeaderView = new window.SS.IssuesHeaderView({
      app: this,
      model: this.favoriteFilter
    });
    this.headerRegion.show(this.issuesHeaderView);

    this.issuesView = new window.SS.IssuesView({
      app: this,
      collection: this.issues
    });
    this.resultsRegion.show(this.issuesView);

    this.issuesActionsView = new window.SS.IssuesActionsView({
      app: this,
      collection: this.issues
    });
    this.actionsRegion.show(this.issuesActionsView);
  });


  NavigatorApp.addInitializer(function() {
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
        property: 'severities',
        type: window.SS.SelectFilterView,
        enabled: true,
        optional: false,
        choices: {
          'BLOCKER': window.SS.phrases.severities.BLOCKER,
          'CRITICAL': window.SS.phrases.severities.CRITICAL,
          'MAJOR': window.SS.phrases.severities.MAJOR,
          'MINOR': window.SS.phrases.severities.MINOR,
          'INFO': window.SS.phrases.severities.INFO
        },
        choiceIcons: {
          'BLOCKER': 'severity-blocker',
          'CRITICAL': 'severity-critical',
          'MAJOR': 'severity-major',
          'MINOR': 'severity-minor',
          'INFO': 'severity-info'
        }
      }),

      new window.SS.Filter({
        name: window.SS.phrases.status,
        property: 'statuses',
        type: window.SS.SelectFilterView,
        enabled: true,
        optional: false,
        choices: {
          'OPEN': window.SS.phrases.statuses.OPEN,
          'CONFIRMED': window.SS.phrases.statuses.CONFIRMED,
          'REOPENED': window.SS.phrases.statuses.REOPENED,
          'RESOLVED': window.SS.phrases.statuses.RESOLVED,
          'CLOSED': window.SS.phrases.statuses.CLOSED
        },
        choiceIcons: {
          'OPEN': 'status-open',
          'CONFIRMED': 'status-confirmed',
          'REOPENED': 'status-reopened',
          'RESOLVED': 'status-resolved',
          'CLOSED': 'status-closed'
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
        property: 'resolutions',
        type: window.SS.SelectFilterView,
        enabled: false,
        optional: true,
        choices: {
          'FALSE-POSITIVE': window.SS.phrases.resolutions['FALSE-POSITIVE'],
          'FIXED': window.SS.phrases.resolutions.FIXED,
          'REMOVED': window.SS.phrases.resolutions.REMOVED
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
      }),

      new window.SS.Filter({
        property: 'resolved',
        type: window.SS.ContextFilterView,
        enabled: true,
        optional: false
      }),

      new window.SS.Filter({
        property: 'actionPlans',
        type: window.SS.ContextFilterView,
        enabled: true,
        optional: false
      }),

      new window.SS.Filter({
        property: 'createdAt',
        type: window.SS.ContextFilterView,
        enabled: true,
        optional: false
      })
    ]);


    this.filterBarView = new window.SS.IssuesFilterBarView({
      app: this,
      collection: this.filters,
      extra: {
        sort: '',
        asc: false
      }
    });

    this.filtersRegion.show(this.filterBarView);
  });


  NavigatorApp.addInitializer(function() {
    var app = this;

    jQuery.when(this.appState.fetch()).done(function() {

      if (app.appState.get('favorites')) {
        app.filters.unshift(
          new window.SS.Filter({
            type: window.SS.IssuesFavoriteFilterView,
            enabled: true,
            optional: false,
            choices: app.appState.get('favorites'),
            manageUrl: '/issues/manage'
          })
        );
      }

      app.router = new window.SS.IssuesRouter({
        app: app
      });
      Backbone.history.start();

      app.favoriteFilter.on('change:query', function(model, query) {
        app.router.navigate(query, { trigger: true, replace: true });
      });
    });
  });


  NavigatorApp.addInitializer(function() {
    var app = this;

    window.onBulkIssues = function() {
      app.fetchFirstPage();
      jQuery('.ui-dialog, .ui-widget-overlay').remove();
    };

    window.onSaveAs = window.onCopy = window.onEdit = function(id) {
      jQuery('#modal').dialog('close');
      app.appState.fetch();

      var filter = new window.SS.FavoriteFilter({ id: id });
      filter.fetch({
        success: function() {
          app.state.set('search', false);
          app.favoriteFilter.set(filter.toJSON());
          app.fetchFirstPage();
        }
      });
    };
  });


  NavigatorApp.getQuery = function(withoutId) {
    var query = this.filterBarView.getQuery();
    if (!withoutId && this.favoriteFilter.id) {
      query['id'] = this.favoriteFilter.id;
    }
    return query;
  };


  NavigatorApp.storeQuery = function(query, sorting) {
    if (sorting) {
      _.extend(query, {
        sort: sorting.sort,
        asc: '' + sorting.asc
      });
    }

    var queryString = _.map(query, function(v, k) {
      return [k, encodeURIComponent(v)].join('=');
    }).join('|');
    this.router.navigate(queryString, { replace: true });
  };


  NavigatorApp.restoreSorting = function(query) {
    var sort = _.findWhere(query, { key: 'sort' }),
        asc = _.findWhere(query, { key: 'asc' });

    if (sort && asc) {
      this.issues.sorting = {
        sort: sort.value,
        sortText: jQuery('[data-sort=' + sort.value + ']:first').text(),
        asc: asc.value === 'true'
      }
    }
  };


  NavigatorApp.fetchIssues = function(firstPage) {
    var query = this.getQuery(),
        fetchQuery =_.extend({
          pageIndex: this.issuesPage
        }, query);

    if (this.issues.sorting) {
      _.extend(fetchQuery, {
        sort: this.issues.sorting.sort,
        asc: this.issues.sorting.asc
      });
    }

    _.extend(fetchQuery, {
      hideRules: true
    });

    if (this.favoriteFilter.id) {
      query['id'] = this.favoriteFilter.id;
      fetchQuery['id'] = this.favoriteFilter.id;
    }

    this.storeQuery(query, this.issues.sorting);

    var that = this;
    this.issuesView.$el.addClass('navigator-fetching');
    if (firstPage) {
      this.issues.fetch({
        data: fetchQuery,
        success: function() {
          that.issuesView.$el.removeClass('navigator-fetching');
        }
      });
      this.detailsRegion.reset();
    } else {
      this.issues.fetch({
        data: fetchQuery,
        remove: false,
        success: function() {
          that.issuesView.$el.removeClass('navigator-fetching');
        }
      });
    }
  };


  NavigatorApp.fetchFirstPage = function() {
    this.issuesPage = 1;
    this.fetchIssues(true);
  };


  NavigatorApp.fetchNextPage = function() {
    if (this.issuesPage < this.issues.paging.pages) {
      this.issuesPage++;
      this.fetchIssues(false);
    }
  };

});
