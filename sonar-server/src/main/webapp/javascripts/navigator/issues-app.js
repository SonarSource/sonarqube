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

    this.issueDetailView = new window.SS.IssueDetailView();
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
          'BLOCKER': window.SS.phrases.severities.blocker,
          'CRITICAL': window.SS.phrases.severities.critical,
          'MAJOR': window.SS.phrases.severities.major,
          'MINOR': window.SS.phrases.severities.minor,
          'INFO': window.SS.phrases.severities.info
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
          'OPEN': window.SS.phrases.statuses.open,
          'CONFIRMED': window.SS.phrases.statuses.confirmed,
          'REOPENED': window.SS.phrases.statuses.reopened,
          'RESOLVED': window.SS.phrases.statuses.resolved,
          'CLOSED': window.SS.phrases.statuses.closed
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
        app.router.navigate(query, { trigger: true });
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
    var fullQuery = query;
    if (sorting) {
      _.extend(fullQuery, {
        sort: sorting.sort,
        asc: '' + sorting.asc
      });
    }

    var queryString = _.map(fullQuery, function(v, k) {
      return [k, encodeURIComponent(v)].join('=');
    }).join('|');
    this.router.navigate(queryString);
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

    if (this.favoriteFilter.id) {
      query['id'] = this.favoriteFilter.id;
      fetchQuery['id'] = this.favoriteFilter.id;
    }

    this.storeQuery(query, this.issues.sorting);

    var that = this;
    this.issuesView.$el.addClass('fetching');
    if (firstPage) {
      this.issues.fetch({
        data: fetchQuery,
        success: function() {
          that.issuesView.$el.removeClass('fetching');
        }
      });
    } else {
      this.issues.fetch({
        data: fetchQuery,
        remove: false,
        success: function() {
          that.issuesView.$el.removeClass('fetching');
        }
      });
    }

    this.detailsRegion.reset();
  };


  NavigatorApp.fetchFirstPage = function() {
    this.issuesPage = 1;
    this.fetchIssues(true);
  };


  NavigatorApp.fetchNextPage = function() {
    this.issuesPage++;
    this.fetchIssues(false);
  };

});
