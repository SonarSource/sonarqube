import _ from 'underscore';
import BaseFacet from './base-facet';

export default BaseFacet.extend({
  statuses: ['READY', 'DEPRECATED', 'BETA'],

  getValues: function () {
    var values = this.model.getValues();
    var x = values.map(function (value) {
      return _.extend(value, { label: t('rules.status', value.val.toLowerCase()) });
    });
    return x;
  },

  sortValues: function (values) {
    var order = this.statuses;
    return _.sortBy(values, function (v) {
      return order.indexOf(v.val);
    });
  },

  serializeData: function () {
    return _.extend(BaseFacet.prototype.serializeData.apply(this, arguments), {
      values: this.sortValues(this.getValues())
    });
  }
});


