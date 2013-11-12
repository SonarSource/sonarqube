/* global _:false, $j:false, Backbone:false, baseUrl:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

(function() {

  var AssigneeSuggestions = Backbone.Collection.extend({

    parse: function(r) {
      return r.results;
    },


    url: function() {
      return baseUrl + '/api/users/search?f=s2';
    }

  });



  var ProjectSuggestions = Backbone.Collection.extend({

    parse: function(r) {
      return r.results;
    },


    url: function() {
      return baseUrl + '/api/resources/search?f=s2&q=TRK&display_key=true';
    }

  });



  var AjaxSelectSuggestionView = Backbone.Marionette.ItemView.extend({
    template: '#projectSuggestionTemplate',
    tagName: 'li',


    events: {
      'change input[type=checkbox]': 'changeSelection'
    },


    changeSelection: function() {
      this.options.detailsView.updateSelection();
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
    template: '#projectNoSuggestionsTemplate',
    tagName: 'li',
    className: 'single'
  });



  var AjaxSelectSuggestionsView = Backbone.Marionette.CollectionView.extend({
    itemView: AjaxSelectSuggestionView,
    emptyView: AjaxSelectNoSuggestionsView,
    tagName: 'ul',
    className: 'navigator-filter-select-list',


    itemViewOptions: function() {
      return {
        detailsView: this.options.detailsView
      };
    }

  });



  var AjaxSelectDetailsFilterView = window.SS.DetailsFilterView.extend({
    template: '#projectFilterTemplate',


    initialize: function() {
      window.SS.DetailsFilterView.prototype.initialize.apply(this, arguments);

      this.suggestions = new ProjectSuggestions();
      this.suggestionsView = new AjaxSelectSuggestionsView({
        collection: this.suggestions,
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
      this.suggestionsView.$el.appendTo(this.$el);
      this.suggestions.reset([]);
    },


    onShow: function() {
      this.$('.navigator-filter-search input').focus();
    },


    search: function() {
      var query = this.$('.navigator-filter-search input').val();
      if (query.length > 1) {
        this.suggestions.fetch({
          data: { s: query },
          reset: true
        });
      } else {
        this.suggestions.reset([]);
      }
    },


    updateSelection: function() {
      var value = this.$('input[type=checkbox]:checked').map(function () {
            return $j(this).val();
          }).get(),

          textValue = this.$('input[type=checkbox]:checked').map(function () {
            return $j(this).next().text();
          }).get();

      this.model.set({
        value: value,
        textValue: textValue
      });
    },


    serializeData: function() {
      return _.extend({}, this.model.toJSON(), {
        suggestions: this.suggestions.toJSON()
      });
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
        detailsView: this
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

    initialize: function() {
      AjaxSelectDetailsFilterView.prototype.initialize.apply(this, arguments);

      this.suggestions = new AssigneeSuggestions();
      this.suggestionsView = new AjaxSelectSuggestionsView({
        collection: this.suggestions,
        model: this.model,
        detailsView: this
      });
    }

  });



  var AssigneeFilterView = AjaxSelectFilterView.extend({

    initialize: function() {
      window.SS.BaseFilterView.prototype.initialize.call(this, {
        detailsView: DetailsAssigneeFilterView
      });
    }

  });



  /*
   * Export public classes
   */

  _.extend(window.SS, {
    ProjectFilterView: ProjectFilterView,
    AssigneeFilterView: AssigneeFilterView
  });

})();
