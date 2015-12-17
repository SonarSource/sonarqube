import _ from 'underscore';
import BaseFacet from './base-facet';
import Template from '../templates/facets/issues-mode-facet.hbs';

export default BaseFacet.extend({
  template: Template,

  events: {
    'change [name="issues-page-mode"]': 'onModeChange'
  },

  onModeChange: function () {
    var mode = this.$('[name="issues-page-mode"]:checked').val();
    this.options.app.state.updateFilter({ facetMode: mode });
  },

  serializeData: function () {
    return _.extend(BaseFacet.prototype.serializeData.apply(this, arguments), {
      mode: this.options.app.state.getFacetMode()
    });
  }
});


