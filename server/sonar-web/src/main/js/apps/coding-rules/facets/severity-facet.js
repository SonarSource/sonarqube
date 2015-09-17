import _ from 'underscore';
import BaseFacet from './base-facet';
import '../templates';

export default BaseFacet.extend({
  template: Templates['coding-rules-severity-facet'],
  severities: ['BLOCKER', 'MINOR', 'CRITICAL', 'INFO', 'MAJOR'],

  sortValues: function (values) {
    var order = this.severities;
    return _.sortBy(values, function (v) {
      return order.indexOf(v.val);
    });
  }
});


