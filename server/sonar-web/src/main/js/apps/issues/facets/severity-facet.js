import _ from 'underscore';
import BaseFacet from './base-facet';
import Template from '../templates/facets/issues-severity-facet.hbs';

export default BaseFacet.extend({
  template: Template,

  sortValues: function (values) {
    var order = ['BLOCKER', 'MINOR', 'CRITICAL', 'INFO', 'MAJOR'];
    return _.sortBy(values, function (v) {
      return order.indexOf(v.val);
    });
  }
});


