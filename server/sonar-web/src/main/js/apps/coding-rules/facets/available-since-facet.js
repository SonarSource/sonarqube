import _ from 'underscore';
import BaseFacet from './base-facet';
import '../templates';

export default BaseFacet.extend({
  template: Templates['coding-rules-available-since-facet'],

  events: function () {
    return _.extend(BaseFacet.prototype.events.apply(this, arguments), {
      'change input': 'applyFacet'
    });
  },

  onRender: function () {
    this.$el.toggleClass('search-navigator-facet-box-collapsed', !this.model.get('enabled'));
    this.$el.attr('data-property', this.model.get('property'));
    this.$('input').datepicker({
      dateFormat: 'yy-mm-dd',
      changeMonth: true,
      changeYear: true
    });
    var value = this.options.app.state.get('query').available_since;
    if (value) {
      this.$('input').val(value);
    }
  },

  applyFacet: function () {
    var obj = {},
        property = this.model.get('property');
    obj[property] = this.$('input').val();
    this.options.app.state.updateFilter(obj);
  },

  getLabelsSource: function () {
    return this.options.app.languages;
  }

});


