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

      function find(source, key, keyField) {
        var searchDict = {};
        searchDict[keyField || 'key'] = key;
        return _.findWhere(source, searchDict) || key;
      }

      this.paging = r.paging;

      return r.issues.map(function(issue) {
        return _.extend({}, issue, {
          component: find(r.components, issue.component),
          project: find(r.projects, issue.project),
          rule: find(r.rules, issue.rule),
          author: find(r.users, issue.author, 'login'),
          comments: (issue.comments || []).map(function(comment) {
            return _.extend({}, comment, {
              user: find(r.users, comment.login, 'login')
            });
          })
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
      this.$el.parent().children().removeClass('active');
      this.$el.addClass('active');

      this.options.app.issueDetailView.model = this.model;
      this.options.app.detailsRegion.show(this.options.app.issueDetailView);
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
      return {
        issuesView: this,
        app: this.options.app
      };
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
      this.options.app.fetchFirstPage();
    },


    serializeData: function() {
      var data = Backbone.Marionette.ItemView.prototype.serializeData.apply(this, arguments);
      return _.extend(data || {}, {
        paging: this.collection.paging,
        sorting: this.collection.sorting,
        query: (Backbone.history.fragment || '').replace(/\|/g, '&')
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
      this.options.app.state.set({
        query: this.options.app.getQuery(),
        search: true
      });
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
      'click #issues-new-search': 'newSearch',
      'click #issues-filter-save-as': 'saveAs',
      'click #issues-filter-save': 'save',
      'click #issues-filter-copy': 'copy',
      'click #issues-filter-edit': 'edit'
    },


    initialize: function(options) {
      Backbone.Marionette.ItemView.prototype.initialize.apply(this, arguments);
      this.listenTo(options.app.state, 'change', this.render);
    },


    newSearch: function() {
      this.model.clear();
      this.options.app.router.navigate('', { trigger: true });
    },


    saveAs: function() {
      var url = baseUrl + '/issues/save_as_form?' + (Backbone.history.fragment || '').replace(/\|/g, '&');
      openModalWindow(url, {});
    },


    save: function() {
      var that = this;
          url = baseUrl + '/issues/save/' + this.model.id + '?' + (Backbone.history.fragment || '').replace(/\|/g, '&');
      jQuery.ajax({
        type: 'POST',
        url: url
      }).done(function() {
            that.options.app.state.set('search', false);
          });
    },


    copy: function() {
      var url = baseUrl + '/issues/copy_form/' + this.model.id;
      openModalWindow(url, {});
    },


    edit: function() {
      var url = baseUrl + '/issues/edit_form/' + this.model.id;
      openModalWindow(url, {});
    },


    serializeData: function() {
      return _.extend({
        canSave: this.model.id && this.options.app.state.get('search')
      }, this.model.toJSON());
    }

  });



  var IssueDetailView = Backbone.Marionette.ItemView.extend({
    template: Handlebars.compile(jQuery('#issue-detail-template').html() || ''),


    onRender: function() {
      this.$('.code-issue-details').tabs();
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
          app.state.set('search', false);
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


    parseQuery: function(query) {
      return (query || '').split('|').map(function(t) {
        return {
          key: t.split('=')[0],
          value: decodeURIComponent(t.split('=')[1])
        }
      });
    },


    index: function(query) {
      var params = this.parseQuery(query);

      var idObj = _.findWhere(params, { key: 'id' });
      if (idObj) {
        var that = this,
            f = this.app.favoriteFilter;
        this.app.canSave = false;
        f.set('id', idObj.value);
        f.fetch({
          success: function() {
            params = _.extend({}, that.parseQuery(f.get('query')), params);
            that.loadResults(params);
          }
        });
      } else {
        this.loadResults(params);
      }
    },


    loadResults: function(params) {
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
    IssueDetailView: IssueDetailView,
    IssuesRouter: IssuesRouter
  });

});
