/* global _:false, Backbone:false, baseUrl:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

jQuery(function() {

  var Issue = Backbone.Model.extend({});
  var Issues = Backbone.Collection.extend({
    model: Issue,


    url: function() {
      return baseUrl + '/api/issues/search';
    },


    parse: function(r) {

      function find(source, key) {
        return _.findWhere(source, { key: key });
      }

      this.paging = r.paging;

      return r.issues.map(function(issue) {
        return _.extend({}, issue, {
          component: find(r.components, issue.component),
          project: find(r.projects, issue.project),
          rule: find(r.rules, issue.rule)
        });
      });

    }
  });



  var FavoriteFilter = Backbone.Model.extend({

    url: function() {
      return baseUrl + '/api/issue_filters/show/' + this.get('id');
    },


    parse: function(r) {
      return r.filter ? r.filter : r;
    }
  });



  var FavoriteFilters = Backbone.Collection.extend({
    model: FavoriteFilter,


    url: function() {
      return baseUrl + '/api/issue_filters/favorites';
    },


    parse: function(r) {
      return r.favoriteFilters;
    }
  });



  var IssueView = Backbone.Marionette.ItemView.extend({
    template: Handlebars.compile(jQuery('#issue-template').html() || ''),
    tagName: 'li',
    events: {
      'click': 'showDetails'
    },


    showDetails: function() {
      var componentKey = this.model.get('component').key,
          issueKey = this.model.get('key'),
          url = baseUrl + '/resource/index/' + componentKey + '?&display_title=true&tab=issues',
          details = jQuery('.navigator-details');

      function collapseIssues() {
        var issues = jQuery('.code-issue', details);
        issues.addClass('code-issue-collapsed');
        issues.filter('[data-issue-key="' + issueKey + '"]').removeClass('code-issue-collapsed');
      }

      this.$el.parent().children().removeClass('active');
      this.$el.addClass('active');

      if (this.options.issuesView.componentKey !== componentKey) {
        this.options.issuesView.componentKey = componentKey;
        details.empty().addClass('loading');
        jQuery.ajax({
          type: 'GET',
          url: url
        }).done(function(r) {
              details.html(r).removeClass('loading');
              collapseIssues();
            });
      }
    }
  });



  var NoIssuesView = Backbone.Marionette.ItemView.extend({
    template: Handlebars.compile(jQuery('#no-issues-template').html() || '')
  });



  var IssuesView = Backbone.Marionette.CollectionView.extend({
    tagName: 'ol',
    className: 'navigator-results-list',
    itemView: IssueView,
    emptyView: NoIssuesView,


    itemViewOptions: function() {
      return { issuesView: this };
    },


    onRender: function() {
      var $scrollEl = jQuery('.navigator-results'),
          scrollEl = $scrollEl.get(0),
          onScroll = function() {
            if (scrollEl.offsetHeight + scrollEl.scrollTop >= scrollEl.scrollHeight) {
              window.SS.IssuesNavigatorApp.fetchNextPage();
            }
          },
          throttledScroll = _.throttle(onScroll, 300);
      $scrollEl.off('scroll').on('scroll', throttledScroll);
    },


    close: function() {
      var scrollEl = jQuery('.navigator-results');
      scrollEl.off('scroll');
      Backbone.Marionette.CollectionView.prototype.close.call(this);
    }

  });



  var IssuesActionsView = Backbone.Marionette.ItemView.extend({
    template: Handlebars.compile(jQuery('#issues-actions-template').html() || ''),


    collectionEvents: {
      'sync': 'render'
    },


    events: {
      'click .navigator-actions-order': 'toggleOrderChoices',
      'click .navigator-actions-order-choices': 'sort'
    },


    ui: {
      orderChoices: '.navigator-actions-order-choices'
    },


    onRender: function() {
      this.$el.toggle(this.collection.length > 0);
      this.$('.open-modal').modal();
    },


    toggleOrderChoices: function(e) {
      e.stopPropagation();
      this.ui.orderChoices.toggleClass('open');
      if (this.ui.orderChoices.is('.open')) {
        var that = this;
        jQuery('body').on('click.issues_actions', function() {
          that.ui.orderChoices.removeClass('open');
        });
      }
    },


    sort: function(e) {
      e.stopPropagation();
      this.ui.orderChoices.removeClass('open');
      jQuery('body').off('click.issues_actions');
      var el = jQuery(e.target),
          sort = el.data('sort'),
          asc = el.data('asc');
      this.collection.sorting = {
        sort: sort,
        sortText: el.text(),
        asc: asc
      };
      this.render();
    },


    serializeData: function() {
      var data = Backbone.Marionette.ItemView.prototype.serializeData.apply(this, arguments);
      return _.extend(data || {}, {
        paging: this.collection.paging,
        sorting: this.collection.sorting,
        query: Backbone.history.fragment || ''
      });
    }
  });



  var IssuesFilterBarView = window.SS.FilterBarView.extend({

    collectionEvents: {
      'change:enabled': 'changeEnabled'
    },


    events: {
      'click .navigator-filter-submit': 'search'
    },


    getQuery: function() {
      var query = {};
      this.collection.each(function(filter) {
        _.extend(query, filter.view.formatValue());
      });
      return query;
    },


    search: function() {
      this.options.app.fetchFirstPage();
    },


    fetchNextPage: function() {
      this.options.app.fetchNextPage();
    }

  });



  var IssuesHeaderView = Backbone.Marionette.ItemView.extend({
    template: Handlebars.compile(jQuery('#issues-header-template').html() || ''),


    modelEvents: {
      'change': 'render'
    },


    events: {
      'click #issues-new-search': 'newSearch'
    },


    newSearch: function() {
      this.options.app.router.navigate('', { trigger: true });
    }

  });



  var IssuesDetailsFavoriteFilterView = window.SS.DetailsFavoriteFilterView.extend({
    template: Handlebars.compile(jQuery('#issues-details-favorite-filter-template').html() || ''),


    applyFavorite: function(e) {
      var id = $j(e.target).data('id'),
          filter = this.model.get('choices').get(id),
          app = this.options.filterView.options.app;

      filter.fetch({
        success: function() {
          app.favoriteFilter.set(filter.toJSON());
        }
      });

      this.options.filterView.hideDetails();
    },


    serializeData: function() {
      return _.extend({}, this.model.toJSON(), {
        items: this.model.get('choices').toJSON()
      });
    }
  });



  var IssuesFavoriteFilterView = window.SS.FavoriteFilterView.extend({

    initialize: function() {
      window.SS.BaseFilterView.prototype.initialize.call(this, {
        detailsView: IssuesDetailsFavoriteFilterView
      });

      this.listenTo(this.model.get('choices'), 'reset', this.render);
    }
  });



  var IssuesRouter = Backbone.Router.extend({

    routes: {
      '': 'index',
      ':query': 'index'
    },


    initialize: function(options) {
      this.app = options.app;
    },


    index: function(query) {
      var params = (query || '').split('|').map(function(t) {
        return {
          key: t.split('=')[0],
          value: decodeURIComponent(t.split('=')[1])
        }
      });
      this.app.filterBarView.restoreFromQuery(params);
      this.app.restoreSorting(params);
      this.app.fetchFirstPage();
    }
  });



  /*
   * Export public classes
   */

  _.extend(window.SS, {
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
  });

});
