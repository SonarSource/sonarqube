import _ from 'underscore';
import BaseFacet from './base-facet';
import Template from '../templates/facets/coding-rules-severity-facet.hbs';

export default BaseFacet.extend({
  template: Template,
  severities: ['BLOCKER', 'MINOR', 'CRITICAL', 'INFO', 'MAJOR'],

  sortValues: function (values) {
    var order = this.severities;
    return _.sortBy(values, function (v) {
      return order.indexOf(v.val);
    });
  }
});
