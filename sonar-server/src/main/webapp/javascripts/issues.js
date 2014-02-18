requirejs.config({

  paths: {
    'backbone': 'third-party/backbone',
    'backbone.marionette': 'third-party/backbone.marionette',
    'handlebars': 'third-party/handlebars'
  },

  shim: {
    'backbone.marionette': {
      deps: ['backbone'],
      exports: 'Marionette'
    },
    'backbone': {
      exports: 'Backbone'
    },
    'handlebars': {
      exports: 'Handlebars'
    }
  }

});

requirejs(
    [
      'backbone', 'backbone.marionette', 'handlebars',
      'issues-extra',
      'navigator/filters/filter-bar',
      'navigator/filters/base-filters',
      'navigator/filters/checkbox-filters',
      'navigator/filters/select-filters',
      'navigator/filters/ajax-select-filters',
      'navigator/filters/resolution-filters',
      'navigator/filters/favorite-filters',
      'navigator/filters/range-filters',
      'navigator/filters/context-filters',
      'navigator/filters/read-only-filters',
      'navigator/filters/action-plan-filters',

      'handlebars-extensions'
    ],
    function (Backbone, Marionette, Handlebars, Extra, FilterBar, BaseFilters, CheckboxFilterView, SelectFilters,
              AjaxSelectFilters, ResolutionFilterView, FavoriteFilters, RangeFilters, ContextFilterView,
              ReadOnlyFilterView, ActionPlanFilterView) {
      Handlebars.registerPartial('detailInnerTemplate', jQuery('#issue-detail-inner-template').html());


      var NavigatorApp = new Marionette.Application();


      NavigatorApp.addRegions({
        headerRegion: '.navigator-header',
        filtersRegion: '.navigator-filters',
        resultsRegion: '.navigator-results',
        actionsRegion: '.navigator-actions',
        detailsRegion: '.navigator-details'
      });


      NavigatorApp.addInitializer(function () {
        jQuery('html').addClass('issues-page');

        this.appState = new Extra.AppState();
        window.SS.appState = this.appState;

        this.state = new Backbone.Model({
          query: ''
        });

        this.issues = new Extra.Issues();
        this.issues.sorting = {
          sort: 'UPDATE_DATE',
          asc: false
        };
        this.issuesPage = 1;

        this.filters = new BaseFilters.Filters();

        this.favoriteFilter = new Extra.FavoriteFilter();
        this.issuesHeaderView = new Extra.IssuesHeaderView({
          app: this,
          model: this.favoriteFilter
        });
        this.headerRegion.show(this.issuesHeaderView);

        this.issuesView = new Extra.IssuesView({
          app: this,
          collection: this.issues
        });
        this.resultsRegion.show(this.issuesView);

        this.issuesActionsView = new Extra.IssuesActionsView({
          app: this,
          collection: this.issues
        });
        this.actionsRegion.show(this.issuesActionsView);
      });


      NavigatorApp.addInitializer(function () {
        var projectFilter = new BaseFilters.Filter({
            name: window.SS.phrases.project,
            property: 'componentRoots',
            type: AjaxSelectFilters.ProjectFilterView,
            enabled: true,
            optional: false
          });
        this.filters.add(projectFilter);

        this.filters.add([
          new BaseFilters.Filter({
            name: window.SS.phrases.severity,
            property: 'severities',
            type: SelectFilters.SelectFilterView,
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

          new BaseFilters.Filter({
            name: window.SS.phrases.status,
            property: 'statuses',
            type: SelectFilters.SelectFilterView,
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

          new BaseFilters.Filter({
            name: window.SS.phrases.assignee,
            property: 'assignees',
            type: AjaxSelectFilters.AssigneeFilterView,
            enabled: true,
            optional: false
          }),

          new BaseFilters.Filter({
            name: window.SS.phrases.actionPlans,
            property: 'actionPlans',
            type: ActionPlanFilterView,
            enabled: false,
            optional: true,
            projectFilter: projectFilter
          }),

          new BaseFilters.Filter({
            name: window.SS.phrases.created,
            propertyFrom: 'createdAfter',
            propertyTo: 'createdBefore',
            type: RangeFilters.DateRangeFilterView,
            enabled: false,
            optional: true
          }),

          new BaseFilters.Filter({
            name: window.SS.phrases.createdAt,
            property: 'createdAt',
            type: ReadOnlyFilterView,
            enabled: false,
            optional: true,
            format: function(value) { return new Date(value).toLocaleString(); }
          }),

          new BaseFilters.Filter({
            name: window.SS.phrases.reporter,
            property: 'reporters',
            type: AjaxSelectFilters.ReporterFilterView,
            enabled: false,
            optional: true
          }),

          new BaseFilters.Filter({
            name: window.SS.phrases.resolution,
            property: 'resolutions',
            type: ResolutionFilterView,
            enabled: false,
            optional: true,
            choices: {
              'UNRESOLVED': window.SS.phrases.resolutions.UNRESOLVED,
              'FALSE-POSITIVE': window.SS.phrases.resolutions['FALSE-POSITIVE'],
              'FIXED': window.SS.phrases.resolutions.FIXED,
              'REMOVED': window.SS.phrases.resolutions.REMOVED
            }
          }),

        ]);


        this.filterBarView = new Extra.IssuesFilterBarView({
          app: this,
          collection: this.filters,
          extra: {
            sort: '',
            asc: false
          }
        });

        this.filtersRegion.show(this.filterBarView);
      });


      NavigatorApp.addInitializer(function () {
        var app = this;

        jQuery.when(this.appState.fetch()).done(function () {

          if (app.appState.get('favorites')) {
            app.filters.unshift(
                new BaseFilters.Filter({
                  type: Extra.IssuesFavoriteFilterView,
                  enabled: true,
                  optional: false,
                  choices: app.appState.get('favorites'),
                  manageUrl: '/issues/manage'
                })
            );
          }

          app.router = new Extra.IssuesRouter({
            app: app
          });
          Backbone.history.start();

          app.favoriteFilter.on('change:query', function (model, query) {
            app.router.navigate(query, { trigger: true, replace: true });
          });
        });
      });


      NavigatorApp.addInitializer(function () {
        var app = this;

        window.onBulkIssues = function () {
          app.fetchFirstPage();
          jQuery('.ui-dialog, .ui-widget-overlay').remove();
        };

        window.onSaveAs = window.onCopy = window.onEdit = function (id) {
          jQuery('#modal').dialog('close');
          app.appState.fetch();

          var filter = new Extra.FavoriteFilter({ id: id });
          filter.fetch({
            success: function () {
              app.state.set('search', false);
              app.favoriteFilter.set(filter.toJSON());
              app.fetchFirstPage();
            }
          });
        };
      });


      NavigatorApp.getQuery = function (withoutId) {
        var query = this.filterBarView.getQuery();
        if (!withoutId && this.favoriteFilter.id) {
          query['id'] = this.favoriteFilter.id;
        }
        return query;
      };


      NavigatorApp.storeQuery = function (query, sorting) {
        if (sorting) {
          _.extend(query, {
            sort: sorting.sort,
            asc: '' + sorting.asc
          });
        }

        var queryString = _.map(query,function (v, k) {
          return [k, encodeURIComponent(v)].join('=');
        }).join('|');
        this.router.navigate(queryString, { replace: true });
      };


      NavigatorApp.restoreSorting = function (query) {
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


      NavigatorApp.fetchIssues = function (firstPage) {
        var query = this.getQuery(),
            fetchQuery = _.extend({
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
            success: function () {
              that.issuesView.$el.removeClass('navigator-fetching');
            }
          });
          this.detailsRegion.reset();
        } else {
          this.issues.fetch({
            data: fetchQuery,
            remove: false,
            success: function () {
              that.issuesView.$el.removeClass('navigator-fetching');
            }
          });
        }
      };


      NavigatorApp.fetchFirstPage = function () {
        this.issuesPage = 1;
        this.fetchIssues(true);
      };


      NavigatorApp.fetchNextPage = function () {
        if (this.issuesPage < this.issues.paging.pages) {
          this.issuesPage++;
          this.fetchIssues(false);
        }
      };

      NavigatorApp.start();

    });
