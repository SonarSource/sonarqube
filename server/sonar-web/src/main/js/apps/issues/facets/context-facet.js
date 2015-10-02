import _ from 'underscore';
import BaseFacet from './base-facet';
import Template from '../templates/facets/issues-context-facet.hbs';

export default BaseFacet.extend({
  template: Template,

  serializeData: function () {
    return _.extend(BaseFacet.prototype.serializeData.apply(this, arguments), {
      state: this.options.app.state.toJSON()
    });
  }
});


