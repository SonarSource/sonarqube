import _ from 'underscore';
import BaseFacet from 'components/navigator/facets/base-facet';
import '../templates';

export default BaseFacet.extend({
  template: Templates['issues-base-facet'],

  onRender: function () {
    BaseFacet.prototype.onRender.apply(this, arguments);
    return this.$('[data-toggle="tooltip"]').tooltip({ container: 'body' });
  },

  onDestroy: function () {
    return this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  serializeData: function () {
    return _.extend(this._super(), {
      state: this.options.app.state.toJSON()
    });
  }
});


