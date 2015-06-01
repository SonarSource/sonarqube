define([
  './base-facet',
  '../templates'
], function (BaseFacet) {

  return BaseFacet.extend({
    template: Templates['issues-severity-facet'],

    sortValues: function (values) {
      var order = ['BLOCKER', 'MINOR', 'CRITICAL', 'INFO', 'MAJOR'];
      return _.sortBy(values, function (v) {
        return order.indexOf(v.val);
      });
    }
  });

});
