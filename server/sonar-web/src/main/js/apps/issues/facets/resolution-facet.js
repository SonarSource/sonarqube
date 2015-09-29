import _ from 'underscore';
import BaseFacet from './base-facet';
import '../templates';

export default BaseFacet.extend({
  template: Templates['issues-resolution-facet'],

  sortValues: function (values) {
    var order = ['FIXED', 'FALSE-POSITIVE', 'REMOVED', 'WONTFIX'];
    return _.sortBy(values, function (v) {
      return order.indexOf(v.val);
    });
  }
});


