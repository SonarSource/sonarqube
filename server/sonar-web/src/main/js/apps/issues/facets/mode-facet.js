import _ from 'underscore';
import BaseFacet from './base-facet';
import '../templates';

export default BaseFacet.extend({
  template: Templates['issues-mode-facet'],

  events: {
    'change [name="issues-page-mode"]': 'onModeChange'
  },

  onModeChange: function () {
    var mode = this.$('[name="issues-page-mode"]:checked').val();
    this.options.app.state.updateFilter({ facetMode: mode });
  },

  serializeData: function () {
    return _.extend(this._super(), { mode: this.options.app.state.getFacetMode() });
  }
});


