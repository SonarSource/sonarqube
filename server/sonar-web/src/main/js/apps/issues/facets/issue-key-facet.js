import _ from 'underscore';
import BaseFacet from './base-facet';
import Template from '../templates/facets/issues-issue-key-facet.hbs';

export default BaseFacet.extend({
  template: Template,

  onRender: function () {
    return this.$el.toggleClass('hidden', !this.options.app.state.get('query').issues);
  },

  disable: function () {
    return this.options.app.state.updateFilter({ issues: null });
  },

  serializeData: function () {
    return _.extend(BaseFacet.prototype.serializeData.apply(this, arguments), {
      issues: this.options.app.state.get('query').issues
    });
  }
});


