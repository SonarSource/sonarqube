import _ from 'underscore';
import BaseFacet from '../../../components/navigator/facets/base-facet';
import Template from '../templates/facets/issues-base-facet.hbs';

export default BaseFacet.extend({
  template: Template,

  onRender: function () {
    BaseFacet.prototype.onRender.apply(this, arguments);
    return this.$('[data-toggle="tooltip"]').tooltip({ container: 'body' });
  },

  onDestroy: function () {
    return this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  serializeData: function () {
    return _.extend(BaseFacet.prototype.serializeData.apply(this, arguments), {
      state: this.options.app.state.toJSON()
    });
  }
});


