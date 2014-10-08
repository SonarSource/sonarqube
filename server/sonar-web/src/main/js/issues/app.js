requirejs.config({
  baseUrl: baseUrl + '/js',

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
      'issues/extra',
      'navigator/filters/filter-bar',
      'navigator/filters/base-filters',
      'navigator/filters/checkbox-filters',
      'navigator/filters/choice-filters',
      'navigator/filters/ajax-select-filters',
      'navigator/filters/favorite-filters',
      'navigator/filters/range-filters',
      'navigator/filters/context-filters',
      'navigator/filters/read-only-filters',
      'navigator/filters/action-plan-filters',
      'navigator/filters/rule-filters',

      'common/handlebars-extensions'
    ],
    function (Backbone, Marionette, Handlebars, Extra, FilterBar, BaseFilters, CheckboxFilterView,
              ChoiceFilters, AjaxSelectFilters, FavoriteFilters, RangeFilters, ContextFilterView,
              ReadOnlyFilterView, ActionPlanFilterView, RuleFilterView) {
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
        jQuery('html').addClass('navigator-page issues-page');

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
        this.projectFilter = new BaseFilters.Filter({
          name: window.SS.phrases.project,
          property: 'componentRoots',
          type: AjaxSelectFilters.ProjectFilterView,
          enabled: true,
          optional: false
        });
        this.filters.add(this.projectFilter);

        this.assigneeChoices = {
          '!assigned': window.SS.phrases.unassigned
        };
        this.reporterChoices = {};
        if (window.SS.currentUser) {
          this.assigneeChoices[window.SS.currentUser] = window.SS.currentUserName + ' (' + window.SS.currentUser + ')';
          this.reporterChoices[window.SS.currentUser] = window.SS.currentUserName + ' (' + window.SS.currentUser + ')';
        }
      });


      NavigatorApp.addInitializer(function () {
        this.filters.add([
          new BaseFilters.Filter({
            name: window.SS.phrases.severity,
            property: 'severities',
            type: ChoiceFilters.ChoiceFilterView,
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
          })
        ]);
      });


      NavigatorApp.addInitializer(function () {
        this.filters.add([
          new BaseFilters.Filter({
            name: window.SS.phrases.status,
            property: 'statuses',
            type: ChoiceFilters.ChoiceFilterView,
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
          })
        ]);
      });


      NavigatorApp.addInitializer(function () {
        this.filters.add([
          new BaseFilters.Filter({
            name: window.SS.phrases.assignee,
            property: 'assignees',
            type: AjaxSelectFilters.AssigneeFilterView,
            enabled: true,
            optional: false,
            choices: this.assigneeChoices
          })
        ]);
      });


      NavigatorApp.addInitializer(function () {
        this.filters.add([
          new BaseFilters.Filter({
            name: window.SS.phrases.resolution,
            property: 'resolutions',
            type: ChoiceFilters.ChoiceFilterView,
            enabled: true,
            optional: false,
            choices: {
              '!resolved=true': window.SS.phrases.resolutions.RESOLVED,
              '!resolved=false': window.SS.phrases.resolutions.UNRESOLVED,
              'FALSE-POSITIVE': window.SS.phrases.resolutions['FALSE-POSITIVE'],
              'FIXED': window.SS.phrases.resolutions.FIXED,
              'REMOVED': window.SS.phrases.resolutions.REMOVED
            }
          })
        ]);
      });


      NavigatorApp.addInitializer(function () {
        this.filters.add([
          new BaseFilters.Filter({
            name: window.SS.phrases.actionPlan,
            property: 'actionPlans',
            type: ActionPlanFilterView,
            enabled: false,
            optional: true,
            projectFilter: this.projectFilter,
            choices: {
              '!planned': window.SS.phrases.unplanned
            }
          })
        ]);
      });


      NavigatorApp.addInitializer(function () {
        this.filters.add([
          new BaseFilters.Filter({
            name: window.SS.phrases.created,
            propertyFrom: 'createdAfter',
            propertyTo: 'createdBefore',
            type: RangeFilters.DateRangeFilterView,
            enabled: false,
            optional: true
          })
        ]);
      });


      NavigatorApp.addInitializer(function () {
        this.filters.add([
          new BaseFilters.Filter({
            name: window.SS.phrases.createdAt,
            property: 'createdAt',
            type: ReadOnlyFilterView,
            enabled: false,
            optional: true,
            format: function(value) { return moment(value).format('YYYY-MM-DD HH:mm'); }
          })
        ]);
      });


      NavigatorApp.addInitializer(function () {
        this.filters.add([
          new BaseFilters.Filter({
            name: window.SS.phrases.language,
            property: 'languages',
            type: ChoiceFilters.ChoiceFilterView,
            enabled: false,
            optional: true,
            choices: window.SS.languages
          })
        ]);
      });


      NavigatorApp.addInitializer(function () {
        this.filters.add([
          new BaseFilters.Filter({
            name: window.SS.phrases.reporter,
            property: 'reporters',
            type: AjaxSelectFilters.ReporterFilterView,
            enabled: false,
            optional: true,
            choices: this.reporterChoices
          })
        ]);
      });


      NavigatorApp.addInitializer(function () {
        this.filters.add([
          new BaseFilters.Filter({
            name: window.SS.phrases.rule,
            property: 'rules',
            type: RuleFilterView,
            enabled: false,
            optional: true
          })
        ]);
      });


      NavigatorApp.addInitializer(function () {
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
          jQuery('#modal').dialog('close');
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


      NavigatorApp.addInitializer(function () {
        this.onResize = function() {
          var footerEl = jQuery('#footer'),
              footerHeight = footerEl.outerHeight(true);

          var resultsEl = jQuery('.navigator-results'),
              resultsHeight = jQuery(window).height() - resultsEl.offset().top -
                  parseInt(resultsEl.css('margin-bottom'), 10) - footerHeight;
          resultsEl.height(resultsHeight);

          var detailsEl = jQuery('.navigator-details'),
              detailsWidth = jQuery(window).width() - detailsEl.offset().left -
                  parseInt(detailsEl.css('margin-right'), 10) - 20,
              detailsHeight = jQuery(window).height() - detailsEl.offset().top -
                  parseInt(detailsEl.css('margin-bottom'), 10) - footerHeight;
          detailsEl.width(detailsWidth).height(detailsHeight);

          var resultsLoadingEl = jQuery('.navigator-results-loader');
          resultsLoadingEl
              .css('top', resultsEl.offset().top)
              .css('left', resultsEl.offset().left)
              .width(resultsEl.width() + 10)
              .height(resultsEl.height() + 10);
        };
        jQuery(window).on('resize', this.onResize);
        this.onResize();
      });


      NavigatorApp.addInitializer(function () {
        var that = this;
        jQuery('body')
            .on('mousemove', function (e) {
              that.processResize(e);
            })
            .on('mouseup', function () {
              that.stopResize();
            });
        jQuery('.navigator-resizer').on('mousedown', function (e) {
          that.startResize(e);
        });

        var resultsWidth = localStorage.getItem('issuesResultsWidth');
        if (resultsWidth) {
          jQuery('.navigator-results').width(+resultsWidth);
          jQuery('.navigator-side').width(+resultsWidth + 20);
          this.onResize();
        }
      });


      NavigatorApp.startResize = function (e) {
        this.isResize = true;
        this.originalWidth = jQuery('.navigator-results').width();
        this.x = e.clientX;
        jQuery('html').attr('unselectable', 'on').css('user-select', 'none').on('selectstart', false);
      };


      NavigatorApp.processResize = function (e) {
        if (this.isResize) {
          var delta = e.clientX - this.x;
          jQuery('.navigator-results').width(this.originalWidth + delta);
          jQuery('.navigator-side').width(this.originalWidth + 20 + delta);
          localStorage.setItem('issuesResultsWidth', jQuery('.navigator-results').width());
          this.onResize();
        }
      };


      NavigatorApp.stopResize = function() {
        if (this.isResize) {
          jQuery('html').attr('unselectable', 'off').css('user-select', 'text').off('selectstart');
        }
        this.isResize = false;
      };


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
          };
        }
      };


      NavigatorApp.fetchIssues = function (firstPage) {
        var query = this.getQuery(),
            fetchQuery = _.extend({
              pageIndex: this.issuesPage
            }, query);

        // SONAR-5086
        if (fetchQuery['actionPlans'] && fetchQuery['componentRoots']) {
          delete fetchQuery['componentRoots'];
        }

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
        jQuery('.navigator-results').addClass('fetching');
        if (firstPage) {
          this.issues.fetch({
            data: fetchQuery,
            success: function () {
              jQuery('.navigator-results').removeClass('fetching');
              that.issuesView.selectFirst();
            }
          });
          this.detailsRegion.reset();
        } else {
          this.issues.fetch({
            data: fetchQuery,
            remove: false,
            success: function () {
              jQuery('.navigator-results').removeClass('fetching');
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

      window.requestMessages().done(function () {
        NavigatorApp.start();
      });

    });
