define(
    [
      'backbone',
      'backbone.marionette',
      'navigator/filters/filter-bar',
      'navigator/filters/base-filters',
      'navigator/filters/favorite-filters',
      'navigator/filters/read-only-filters',
      'component-viewer/main',
      'templates/issues'
    ],
    function (Backbone, Marionette, FilterBarView, BaseFilters, FavoriteFiltersModule, ReadOnlyFilterView,
              ComponentViewer, Templates) {

      var AppState = Backbone.Model.extend({

        defaults: {
          canManageFilters: false,
          canBulkChange: false
        },


        url: function () {
          return baseUrl + '/api/issue_filters/app';
        }

      });


      var Issue = Backbone.Model.extend({

        url: function () {
          return baseUrl + '/api/issues/show?key=' + this.get('key');
        },


        parse: function (r) {
          return r.issue ? r.issue : r;
        }

      });


      var Issues = Backbone.Collection.extend({
        model: Issue,


        url: function () {
          return baseUrl + '/api/issues/search';
        },


        parse: function (r) {

          function find(source, key, keyField) {
            var searchDict = {};
            searchDict[keyField || 'key'] = key;
            return _.findWhere(source, searchDict) || key;
          }

          this.paging = r.paging;
          this.maxResultsReached = r.maxResultsReached;

          return r.issues.map(function (issue) {
            var component = find(r.components, issue.component),
                project = find(r.projects, issue.project),
                rule = find(r.rules, issue.rule);

            if (component) {
              _.extend(issue, {
                componentLongName: component.longName,
                componentQualifier: component.qualifier
              });
            }

            if (project) {
              _.extend(issue, {
                projectLongName: project.longName
              });
            }

            if (rule) {
              _.extend(issue, {
                ruleName: rule.name
              });
            }

            return issue;
          });

        }
      });


      var FavoriteFilter = Backbone.Model.extend({

        url: function () {
          return baseUrl + '/api/issue_filters/show/' + this.get('id');
        },


        parse: function (r) {
          return r.filter ? r.filter : r;
        }
      });


      var FavoriteFilters = Backbone.Collection.extend({
        model: FavoriteFilter,


        url: function () {
          return baseUrl + '/api/issue_filters/favorites';
        },


        parse: function (r) {
          return r.favoriteFilters;
        }
      });


      var IssueView = Marionette.ItemView.extend({
        template: Templates['issue-detail'],
        tagName: 'li',


        ui: {
          component: '.component'
        },


        events: {
          'click': 'showDetails'
        },


        modelEvents: {
          'change': 'render'
        },


        showDetails: function () {
          key.setScope('list');
          this.options.issuesView.selected = this.$el.parent().children().index(this.$el);

          this.options.issuesView.selectIssue(this.$el, false);

          var that = this,
              app = this.options.app,
              settings = localStorage.getItem('componentViewerSettings'),
              navigatorDetails = jQuery('.navigator-details'),
              componentViewer = new ComponentViewer({
                settings: settings,
                shouldStoreSettings: true,
                elementToFit: navigatorDetails,
                component: {
                  project: this.model.get('project'),
                  projectLongName: this.model.get('projectLongName')
                }
              }),
              showCallback = function () {
                navigatorDetails.removeClass('navigator-fetching');
                app.detailsRegion.show(componentViewer);
                componentViewer.settings.set('issues', false);
                componentViewer.open(that.model.get('component'));
                componentViewer.on('loaded', function() {
                  componentViewer.off('loaded');
                  componentViewer.showIssues(false, that.model.toJSON());
                });
              };

          navigatorDetails.empty().addClass('navigator-fetching');
          var issueKey = this.model.get('key');
          this.model.clear({ silent: true });
          this.model.set({ key: issueKey }, { silent: true });
          jQuery.when(this.model.fetch()).done(showCallback);
        },


        serializeData: function () {
          var projectFilter = this.options.app.filters.findWhere({ property: 'componentRoots' }),
              singleProject = _.isArray(projectFilter.get('value')) && projectFilter.get('value').length === 1;

          return _.extend({
            singleProject: singleProject
          }, this.model.toJSON());
        }
      });


      var NoIssuesView = Marionette.ItemView.extend({
        tagName: 'li',
        className: 'navigator-results-no-results',
        template: Templates['no-issues']
      });


      var IssuesView = Marionette.CompositeView.extend({
        template: Templates['issues'],
        itemViewContainer: '.navigator-results-list',
        itemView: IssueView,
        emptyView: NoIssuesView,


        initialize: function() {
          var openIssue = function(el) {
            el.click();
          };
          this.openIssue = _.debounce(openIssue, 300);
        },


        itemViewOptions: function () {
          return {
            issuesView: this,
            app: this.options.app
          };
        },


        selectIssue: function(el, open) {
          this.$('.active').removeClass('active');
          el.addClass('active');
          if (open) {
            this.openIssue(el);
          }
        },


        selectFirst: function() {
          this.selected = -1;
          this.selectNext();
        },


        selectNext: function() {
          if (this.selected < this.collection.length - 1) {
            this.selected++;
            var child = this.$(this.itemViewContainer).children().eq(this.selected),
                container = jQuery('.navigator-results'),
                containerHeight = container.height(),
                bottom = child.position().top + child.outerHeight();
            if (bottom > containerHeight) {
              container.scrollTop(container.scrollTop() - containerHeight + bottom);
            }
            this.selectIssue(child, true);
          }
        },


        selectPrev: function() {
          if (this.selected > 0) {
            this.selected--;
            var child = this.$(this.itemViewContainer).children().eq(this.selected),
                container = jQuery('.navigator-results'),
                top = child.position().top;
            if (top < 0) {
              container.scrollTop(container.scrollTop() + top);
            }
            this.selectIssue(child, true);
          }
        },


        onRender: function () {
          var that = this,
              $scrollEl = jQuery('.navigator-results'),
              scrollEl = $scrollEl.get(0),
              onScroll = function () {
                if (scrollEl.offsetHeight + scrollEl.scrollTop >= scrollEl.scrollHeight) {
                  that.options.app.fetchNextPage();
                }
              },
              throttledScroll = _.throttle(onScroll, 300);
          $scrollEl.off('scroll').on('scroll', throttledScroll);
          this.bindShortcuts();
        },


        onAfterItemAdded: function () {
          var showLimitNotes = this.collection.maxResultsReached != null && this.collection.maxResultsReached;
          jQuery('.navigator').toggleClass('navigator-with-notes', showLimitNotes);
          jQuery('.navigator-notes').toggle(showLimitNotes);
        },


        close: function () {
          var scrollEl = jQuery('.navigator-results');
          scrollEl.off('scroll');
          Marionette.CollectionView.prototype.close.call(this);
        },


        bindShortcuts: function () {
          var that = this;
          key('up', 'list', function() {
            that.selectPrev();
          });
          key('down', 'list', function() {
            that.selectNext();
          });
        }

      });


      var IssuesActionsView = Marionette.ItemView.extend({
        template: Templates['issues-actions'],


        collectionEvents: {
          'sync': 'render'
        },


        events: {
          'click .navigator-actions-order': 'toggleOrderChoices',
          'click .navigator-actions-order-choices': 'sort',
          'click .navigator-actions-bulk': 'bulkChange'
        },


        ui: {
          orderChoices: '.navigator-actions-order-choices'
        },


        onRender: function () {
          if (!this.collection.sorting.sortText) {
            this.collection.sorting.sortText = this.$('[data-sort=' + this.collection.sorting.sort + ']:first').text();
            this.$('.navigator-actions-ordered-by').text(this.collection.sorting.sortText);
          }
        },


        toggleOrderChoices: function (e) {
          e.stopPropagation();
          this.ui.orderChoices.toggleClass('open');
          if (this.ui.orderChoices.is('.open')) {
            var that = this;
            jQuery('body').on('click.issues_actions', function () {
              that.ui.orderChoices.removeClass('open');
            });
          }
        },


        sort: function (e) {
          e.stopPropagation();
          this.ui.orderChoices.removeClass('open');
          jQuery('body').off('click.issues_actions');
          var el = jQuery(e.target),
              sort = el.data('sort'),
              asc = el.data('asc');

          if (sort != null && asc != null) {
            this.collection.sorting = {
              sort: sort,
              sortText: el.text(),
              asc: asc
            };
            this.options.app.fetchFirstPage();
          }
        },


        bulkChange: function(e) {
          e.preventDefault();
          openModalWindow(jQuery(e.currentTarget).prop('href'), {});
        },


        serializeData: function () {
          var data = Marionette.ItemView.prototype.serializeData.apply(this, arguments);
          return _.extend(data || {}, {
            paging: this.collection.paging,
            sorting: this.collection.sorting,
            maxResultsReached: this.collection.maxResultsReached,
            appState: window.SS.appState.toJSON(),
            bulkChangeUrl: baseUrl + '/issues/bulk_change_form',
            query: (Backbone.history.fragment || '').replace(/\|/g, '&')
          });
        }
      });



      var IssuesDetailsFavoriteFilterView = FavoriteFiltersModule.DetailsFavoriteFilterView.extend({
        template: Templates['issues-details-favorite-filter'],


        applyFavorite: function (e) {
          var id = $j(e.target).data('id'),
              filter = new FavoriteFilter({ id: id }),
              app = this.options.filterView.options.app;

          filter.fetch({
            success: function () {
              app.state.set('search', false);
              app.favoriteFilter.clear({ silent: true });
              app.favoriteFilter.set(filter.toJSON());
            }
          });

          this.options.filterView.hideDetails();
        },


        serializeData: function () {
          return _.extend({}, this.model.toJSON(), {
            items: _.sortBy(this.model.get('choices'), function(item) {
              return item.name.toLowerCase();
            })
          });
        }
      });



      var IssuesFavoriteFilterView = FavoriteFiltersModule.FavoriteFilterView.extend({

        initialize: function () {
          BaseFilters.BaseFilterView.prototype.initialize.call(this, {
            detailsView: IssuesDetailsFavoriteFilterView
          });

          this.listenTo(window.SS.appState, 'change:favorites', this.updateFavorites);
        },


        updateFavorites: function () {
          this.model.set('choices', window.SS.appState.get('favorites'));
          this.render();
        }
      });



      var IssuesFilterBarView = FilterBarView.extend({
        template: Templates['filter-bar'],

        collectionEvents: {
          'change:enabled': 'changeEnabled'
        },


        events: {
          'click .navigator-filter-submit': 'search'
        },


        getQuery: function () {
          var query = {};
          this.collection.each(function (filter) {
            _.extend(query, filter.view.formatValue());
          });
          return query;
        },


        onAfterItemAdded: function (itemView) {
          if (itemView.model.get('type') === FavoriteFiltersModule.FavoriteFilterView ||
              itemView.model.get('type') === IssuesFavoriteFilterView) {
            jQuery('.navigator-header').addClass('navigator-header-favorite');
          }
        },


        addMoreCriteriaFilter: function() {
          var readOnlyFilters = this.collection.where({ type: ReadOnlyFilterView }),
              disabledFilters = _.difference(this.collection.where({ enabled: false }), readOnlyFilters);
          this.moreCriteriaFilter = new BaseFilters.Filter({
            type: require('navigator/filters/more-criteria-filters').MoreCriteriaFilterView,
            enabled: true,
            optional: false,
            filters: disabledFilters
          });
          this.collection.add(this.moreCriteriaFilter);
        },


        changeEnabled: function () {
          var disabledFilters = _.reject(this.collection.where({ enabled: false }), function (filter) {
                return filter.get('type') ===
                       require('navigator/filters/more-criteria-filters').MoreCriteriaFilterView ||
                       filter.get('type') === ReadOnlyFilterView;
              });

          if (disabledFilters.length === 0) {
            this.moreCriteriaFilter.set({ enabled: false }, { silent: true });
          } else {
            this.moreCriteriaFilter.set({ enabled: true }, { silent: true });
          }
          this.moreCriteriaFilter.set({ filters: disabledFilters }, { silent: true });
          this.moreCriteriaFilter.trigger('change:filters');
        },


        search: function () {
          this.$('.navigator-filter-submit').blur();
          this.options.app.state.set({
            query: this.options.app.getQuery(),
            search: true
          });
          this.options.app.fetchFirstPage();
        },


        fetchNextPage: function () {
          this.options.app.fetchNextPage();
        }

      });


      var IssuesHeaderView = Marionette.ItemView.extend({
        template: Templates['issues-header'],


        modelEvents: {
          'change': 'render'
        },


        events: {
          'click #issues-new-search': 'newSearch',
          'click #issues-filter-save-as': 'saveAs',
          'click #issues-filter-save': 'save',
          'click #issues-filter-copy': 'copy',
          'click #issues-filter-edit': 'edit'
        },


        initialize: function (options) {
          Marionette.ItemView.prototype.initialize.apply(this, arguments);
          this.listenTo(options.app.state, 'change', this.render);
        },


        newSearch: function () {
          this.model.clear();
          this.options.app.router.navigate('resolved=false', { trigger: true, replace: true });
        },


        saveAs: function () {
          var url = baseUrl + '/issues/save_as_form?' + (Backbone.history.fragment || '').replace(/\|/g, '&');
          openModalWindow(url, {});
        },


        save: function () {
          var that = this;
          url = baseUrl + '/issues/save/' + this.model.id + '?' + (Backbone.history.fragment || '').replace(/\|/g, '&');
          jQuery.ajax({
            type: 'POST',
            url: url
          }).done(function () {
                that.options.app.state.set('search', false);
              });
        },


        copy: function () {
          var url = baseUrl + '/issues/copy_form/' + this.model.id;
          openModalWindow(url, {});
        },


        edit: function () {
          var url = baseUrl + '/issues/edit_form/' + this.model.id;
          openModalWindow(url, {});
        },


        serializeData: function () {
          return _.extend({
            canSave: this.model.id && this.options.app.state.get('search'),
            appState: window.SS.appState.toJSON(),
            currentUser: window.SS.currentUser
          }, this.model.toJSON());
        }

      });



      var IssuesRouter = Backbone.Router.extend({

        routes: {
          '': 'emptyQuery',
          ':query': 'index'
        },


        initialize: function (options) {
          this.app = options.app;
        },


        parseQuery: function (query, separator) {
          return (query || '').split(separator || '|').map(function (t) {
            var tokens = t.split('=');
            return {
              key: tokens[0],
              value: decodeURIComponent(tokens[1])
            };
          });
        },


        emptyQuery: function () {
          this.navigate('resolved=false', { trigger: true, replace: true });
        },


        index: function (query) {
          var params = this.parseQuery(query);

          var idObj = _.findWhere(params, { key: 'id' });
          if (idObj) {
            var that = this,
              f = this.app.favoriteFilter;
            this.app.canSave = false;
            f.set('id', idObj.value);
            f.fetch({
              success: function () {
                var parsedFilter = that.parseQuery(f.get('query'));
                params = _.extend({}, params);
                params = _.extent(params, parsedFilter);
                that.loadResults(params);
              }
            });
          } else {
            this.loadResults(params);
          }
        },


        loadResults: function (params) {
          this.app.filterBarView.restoreFromQuery(params);
          this.app.restoreSorting(params);
          this.app.fetchFirstPage();
        }

      });


      /*
       * Export public classes
       */

      return {
        AppState: AppState,
        Issue: Issue,
        Issues: Issues,
        FavoriteFilter: FavoriteFilter,
        FavoriteFilters: FavoriteFilters,
        IssueView: IssueView,
        IssuesView: IssuesView,
        IssuesActionsView: IssuesActionsView,
        IssuesFilterBarView: IssuesFilterBarView,
        IssuesHeaderView: IssuesHeaderView,
        IssuesFavoriteFilterView: IssuesFavoriteFilterView,
        IssuesRouter: IssuesRouter
      };

    });
