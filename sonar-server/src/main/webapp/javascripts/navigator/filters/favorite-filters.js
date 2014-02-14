/* global _:false, $j:false, baseUrl:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

(function() {

  var DetailsFavoriteFilterView = window.SS.DetailsFilterView.extend({
    template: '#detailsFavoriteFilterTemplate',


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



  var FavoriteFilterView = window.SS.SelectFilterView.extend({
    template: '#favoriteFilterTemplate',
    className: 'navigator-filter navigator-filter-favorite',


    initialize: function() {
      window.SS.BaseFilterView.prototype.initialize.call(this, {
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

  _.extend(window.SS, {
    DetailsFavoriteFilterView: DetailsFavoriteFilterView,
    FavoriteFilterView: FavoriteFilterView
  });

})();
