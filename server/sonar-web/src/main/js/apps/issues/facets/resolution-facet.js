import $ from 'jquery';
import _ from 'underscore';
import BaseFacet from './base-facet';
import '../templates';

export default BaseFacet.extend({
  template: Templates['issues-resolution-facet'],

  onRender: function () {
    BaseFacet.prototype.onRender.apply(this, arguments);
    var value = this.options.app.state.get('query').resolved;
    if ((value != null) && (!value || value === 'false')) {
      return this.$('.js-facet').filter('[data-unresolved]').addClass('active');
    }
  },

  toggleFacet: function (e) {
    var unresolved = $(e.currentTarget).is('[data-unresolved]');
    $(e.currentTarget).toggleClass('active');
    if (unresolved) {
      var checked = $(e.currentTarget).is('.active'),
          value = checked ? 'false' : null;
      return this.options.app.state.updateFilter({
        resolved: value,
        resolutions: null
      });
    } else {
      return this.options.app.state.updateFilter({
        resolved: null,
        resolutions: this.getValue()
      });
    }
  },

  disable: function () {
    return this.options.app.state.updateFilter({
      resolved: null,
      resolutions: null
    });
  },

  sortValues: function (values) {
    var order = ['', 'FIXED', 'FALSE-POSITIVE', 'WONTFIX', 'REMOVED'];
    return _.sortBy(values, function (v) {
      return order.indexOf(v.val);
    });
  }
});


