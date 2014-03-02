define(['backbone', 'backbone.marionette', 'navigator/filters/base-filters', 'navigator/filters/choice-filters', 'common/handlebars-extensions'], function (Backbone, Marionette, BaseFilters, ChoiceFilters) {

  var DetailsFavoriteFilterView = BaseFilters.DetailsFilterView.extend({
    template: getTemplate('#favorite-details-filter-template'),


    events: {
      'click label[data-id]': 'applyFavorite',
      'click .manage label': 'manage'
    },


    applyFavorite: function(e) {
      var id = $j(e.target).data('id');
      window.location = baseUrl + this.model.get('favoriteUrl') + '/' + id;
    },


    manage: function() {
      window.location = baseUrl + this.model.get('manageUrl');
    },


    serializeData: function() {
      var choices = this.model.get('choices'),
          choicesArray =
              _.sortBy(
                  _.map(choices, function (v, k) {
                    return { v: v, k: k };
                  }),
                  'v');

      return _.extend({}, this.model.toJSON(), {
        choicesArray: choicesArray
      });
    }

  });



  var FavoriteFilterView = ChoiceFilters.ChoiceFilterView.extend({
    template: getTemplate('#favorite-filter-template'),
    className: 'navigator-filter navigator-filter-favorite',


    initialize: function() {
      ChoiceFilters.ChoiceFilterView.prototype.initialize.call(this, {
        detailsView: DetailsFavoriteFilterView
      });
    },


    renderValue: function() {
      return '';
    },


    renderInput: function() {},


    isDefaultValue: function() {
      return false;
    }

  });



  /*
   * Export public classes
   */

  return {
    DetailsFavoriteFilterView: DetailsFavoriteFilterView,
    FavoriteFilterView: FavoriteFilterView
  };

});
