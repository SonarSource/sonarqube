define([
  'backbone.marionette',
  'components/navigator/facets/base-facet'
], function (Marionette, BaseFacet) {

  return Marionette.CollectionView.extend({
    className: 'search-navigator-facets-list',

    itemViewOptions: function () {
      return {
        app: this.options.app
      };
    },

    getItemView: function () {
      return BaseFacet;
    },

    collectionEvents: function () {
      return {
        'change:enabled': 'updateState'
      };
    },

    updateState: function () {
      var enabledFacets = this.collection.filter(function (model) {
            return model.get('enabled');
          }),
          enabledFacetIds = enabledFacets.map(function (model) {
            return model.id;
          });
      this.options.app.state.set({facets: enabledFacetIds});
    }

  });

});
