/* global _:false, $j:false, Backbone:false, baseUrl:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

(function() {

  var PAGE_SIZE = 100;



  var Suggestions = Backbone.Collection.extend({

    initialize: function() {
      this.more = false;
      this.page = 0;
    },


    parse: function(r) {
      this.more = r.more;
      return r.results;
    },


    fetch: function(options) {
      this.data = _.extend({
            p: 1,
            ps: PAGE_SIZE
          }, options.data || {});

      var settings = _.extend({}, options, { data: this.data });
      Backbone.Collection.prototype.fetch.call(this, settings);
    },


    fetchNextPage: function(options) {
      if (this.more) {
        this.data.p += 1;
        var settings = _.extend({ remove: false }, options, { data: this.data });
        this.fetch(settings);
      }
    }

  });



  var UserSuggestions = Suggestions.extend({

    url: function() {
      return baseUrl + '/api/users/search?f=s2';
    }

  });



  var ProjectSuggestions = Suggestions.extend({

    url: function() {
      return baseUrl + '/api/resources/search?f=s2&q=TRK&display_key=true';
    }

  });



  var AjaxSelectSuggestionView = Backbone.Marionette.ItemView.extend({
    template: '#ajaxSelectSuggestionTemplate',
    tagName: 'li',


    events: {
      'change input[type=checkbox]': 'changeSelection'
    },


    changeSelection: function(e) {
      var c = $j(e.target);
      if (c.is(':checked')) {
        this.options.detailsView.addSuggestion(c.val(), c.next().text());
      } else {
        this.options.detailsView.removeSuggestion(c.val());
      }
    },


    isModelSelected: function() {
      var value = this.options.detailsView.model.get('value');
      return (_.isArray(value) && _.indexOf(value, this.model.get('id')) !== -1);
    },


    serializeData: function() {
      return _.extend({}, this.model.toJSON(), {
        selected: this.isModelSelected()
      });
    }

  });



  var AjaxSelectNoSuggestionsView = Backbone.Marionette.ItemView.extend({
    template: '#ajaxSelectNoSuggestionsTemplate',
    tagName: 'li',
    className: 'single'
  });



  var AjaxSelectSuggestionsView = Backbone.Marionette.CollectionView.extend({
    itemView: AjaxSelectSuggestionView,
    emptyView: AjaxSelectNoSuggestionsView,
    tagName: 'ul',
    className: 'navigator-filter-select-list',


    onRender: function() {
      this.$el.scrollTop(0);

      var that = this,
          scroll = function() { that.scroll(); },
          throttledScroll = _.throttle(scroll, 1000);
      this.$el.off('scroll').on('scroll', throttledScroll);
    },


    scroll: function() {
      var scrollBottom = this.$el.scrollTop() >=
          this.$el[0].scrollHeight - this.$el.outerHeight();

      if (scrollBottom) {
        this.collection.fetchNextPage();
      }
    },


    itemViewOptions: function() {
      return {
        detailsView: this.options.detailsView
      };
    }

  });



  var AjaxSelectSelectedItemsView = AjaxSelectSuggestionsView.extend({
    emptyView: null,


    collectionEvents: {
      'reset': 'toggleVisibility'
    },


    toggleVisibility: function() {
      this.$el.toggle(this.collection.length > 0);
    }

  });



  var AjaxSelectDetailsFilterView = window.SS.DetailsFilterView.extend({
    template: '#ajaxSelectFilterTemplate',


    initialize: function() {
      window.SS.DetailsFilterView.prototype.initialize.apply(this, arguments);
      this.initSuggestions();
      this.initSelected();
    },


    initSuggestions: function() {

    },


    initSelected: function() {
      this.selectedItems = new Backbone.Collection();
      this.selectedItemsView = new AjaxSelectSelectedItemsView({
        collection: this.selectedItems,
        model: this.model,
        detailsView: this
      });
    },


    bindSearchEvent: function() {
      var that = this,
          keyup = function() { that.search(); };

      this.$('.navigator-filter-search input')
          .on('keyup', $j.debounce(250, keyup));
    },


    onRender: function() {
      this.bindSearchEvent();

      this.suggestionsView.$el.insertAfter(this.$('.navigator-filter-search'));
      this.suggestions.reset([]);

      this.selectedItemsView.$el.insertAfter(this.$('.navigator-filter-search'));
      this.selectedItems.reset([]);
    },


    onShow: function() {
      var that = this,
          selectedValues = _.map(this.model.get('value') || [], function(d, i) {
            return {
              id: d,
              text: that.model.get('textValue')[i]
            };
          });

      this.selectedItems.reset(selectedValues);

      this.$('.navigator-filter-search input').focus();
    },


    onHide: function() {
      this.$('.navigator-filter-search input').val('');
      this.suggestions.reset([]);
    },


    search: function() {
      this.query = this.$('.navigator-filter-search input').val();
      if (this.query.length > 1) {
        this.suggestions.fetch({
          data: {
            s: this.query,
            ps: PAGE_SIZE
          },
          reset: true
        });
      } else {
        this.suggestions.reset([]);
      }
    },


    addSuggestion: function(key, text) {
      var value = this.model.get('value') || [],
          textValue = this.model.get('textValue') || [];
      value.push(key);
      textValue.push(text);

      this.model.set({
        value: value,
        textValue: textValue
      }, { silent: true });

      this.model.trigger('change:value');
    },


    removeSuggestion: function(key) {
      var value = this.model.get('value') || [],
          textValue = this.model.get('textValue') || [],
          index = _.indexOf(value, key);

      if (index !== -1) {
        value.splice(index, 1);
        textValue.splice(index, 1);

        this.model.set({
          value: value,
          textValue: textValue
        }, { silent: true });
      }

      this.model.trigger('change:value');
    }

  });



  var AjaxSelectFilterView = window.SS.BaseFilterView.extend({

    initialize: function() {
      window.SS.BaseFilterView.prototype.initialize.call(this, {
        detailsView: AjaxSelectDetailsFilterView
      });
    },


    renderValue: function() {
      var value = this.model.get('textValue');
      return this.isDefaultValue() ? 'All' : value.join(', ');
    },


    isDefaultValue: function() {
      var value = this.model.get('value');
      return !(_.isArray(value) && value.length > 0);
    }

  });



  var DetailsProjectFilterView = AjaxSelectDetailsFilterView.extend({

    initialize: function() {
      AjaxSelectDetailsFilterView.prototype.initialize.apply(this, arguments);

      this.suggestions = new ProjectSuggestions();
      this.suggestionsView = new AjaxSelectSuggestionsView({
        collection: this.suggestions,
        model: this.model,
        detailsView: this,
        hideSelected: true
      });
    }

  });



  var ProjectFilterView = AjaxSelectFilterView.extend({

    initialize: function() {
      window.SS.BaseFilterView.prototype.initialize.call(this, {
        detailsView: DetailsProjectFilterView
      });
    }

  });



  var DetailsAssigneeFilterView = AjaxSelectDetailsFilterView.extend({

    initSuggestions: function() {
      this.suggestions = new UserSuggestions();
      this.suggestionsView = new AjaxSelectSuggestionsView({
        collection: this.suggestions,
        model: this.model,
        detailsView: this,
        hideSelected: true
      });
    },


    isUnassignedSelected: function() {
      return (this.model.get('value') || []).indexOf('<unassigned>') !== -1;
    },


    onRender: function() {
      this.bindSearchEvent();

      this.suggestionsView.$el.insertAfter(this.$('.navigator-filter-search'));
      this.suggestions.reset([{
        id: '<unassigned>',
        text: 'Unassigned',
        selected: this.isUnassignedSelected()
      }]);

      this.selectedItemsView.$el.insertAfter(this.$('.navigator-filter-search'));
      this.selectedItems.reset([]);
    },


    onHide: function() {
      this.$('.navigator-filter-search input').val('');
      this.suggestions.reset([{
        id: '<unassigned>',
        text: 'Unassigned',
        selected: this.isUnassignedSelected()
      }]);
    },


    search: function() {
      this.query = this.$('.navigator-filter-search input').val();
      if (this.query.length > 1) {
        this.suggestions.fetch({
          data: {
            s: this.query,
            ps: PAGE_SIZE
          },
          reset: true
        });
      } else {
        this.suggestions.reset([{
          id: '<unassigned>',
          text: 'Unassigned',
          selected: this.isUnassignedSelected()
        }]);
      }
    }

  });



  var AssigneeFilterView = AjaxSelectFilterView.extend({

    initialize: function() {
      window.SS.BaseFilterView.prototype.initialize.call(this, {
        detailsView: DetailsAssigneeFilterView
      });
    }

  });



  var DetailsReporterFilterView = AjaxSelectDetailsFilterView.extend({

    initSuggestions: function() {
      this.suggestions = new UserSuggestions();
      this.suggestionsView = new AjaxSelectSuggestionsView({
        collection: this.suggestions,
        model: this.model,
        detailsView: this,
        hideSelected: true
      });
    }

  });



  var ReporterFilterView = AjaxSelectFilterView.extend({

    initialize: function() {
      window.SS.BaseFilterView.prototype.initialize.call(this, {
        detailsView: DetailsReporterFilterView
      });
    }

  });



  /*
   * Export public classes
   */

  _.extend(window.SS, {
    ProjectFilterView: ProjectFilterView,
    AssigneeFilterView: AssigneeFilterView,
    ReporterFilterView: ReporterFilterView
  });

})();
