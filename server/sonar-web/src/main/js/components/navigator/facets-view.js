import Marionette from 'backbone.marionette';
import BaseFacet from './facets/base-facet';

export default Marionette.CollectionView.extend({
  className: 'search-navigator-facets-list',

  childViewOptions: function () {
    return {
      app: this.options.app
    };
  },

  getChildView: function () {
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
    this.options.app.state.set({ facets: enabledFacetIds });
  }

});


