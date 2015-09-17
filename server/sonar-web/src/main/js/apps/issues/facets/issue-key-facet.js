import _ from 'underscore';
import BaseFacet from './base-facet';
import '../templates';

export default BaseFacet.extend({
  template: Templates['issues-issue-key-facet'],

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


