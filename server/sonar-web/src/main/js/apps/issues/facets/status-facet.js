import _ from 'underscore';
import BaseFacet from './base-facet';
import Template from '../templates/facets/issues-status-facet.hbs';

export default BaseFacet.extend({
  template: Template,

  sortValues: function (values) {
    var order = ['OPEN', 'RESOLVED', 'REOPENED', 'CLOSED', 'CONFIRMED'];
    return _.sortBy(values, function (v) {
      return order.indexOf(v.val);
    });
  }
});


