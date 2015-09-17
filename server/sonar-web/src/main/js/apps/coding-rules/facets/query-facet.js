import _ from 'underscore';
import BaseFacet from './base-facet';
import '../templates';

export default BaseFacet.extend({
  template: Templates['coding-rules-query-facet'],

  events: function () {
    return _.extend(BaseFacet.prototype.events.apply(this, arguments), {
      'submit form': 'onFormSubmit'
    });
  },

  onRender: function () {
    this.$el.attr('data-property', this.model.get('property'));
    var query = this.options.app.state.get('query'),
        value = query.q;
    if (value != null) {
      this.$('input').val(value);
    }
  },

  onFormSubmit: function (e) {
    e.preventDefault();
    this.applyFacet();
  },

  applyFacet: function () {
    var obj = {},
        property = this.model.get('property');
    obj[property] = this.$('input').val();
    this.options.app.state.updateFilter(obj, { force: true });
  }
});


