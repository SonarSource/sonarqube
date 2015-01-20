define([
    'coding-rules/facets/base-facet',
    'templates/coding-rules'
], function (BaseFacet) {

  return BaseFacet.extend({
    template: Templates['coding-rules-severity-facet'],
    severities: ['BLOCKER', 'MINOR', 'CRITICAL', 'INFO', 'MAJOR'],

    sortValues: function (values) {
      var order = this.severities;
      return _.sortBy(values, function (v) {
        return order.indexOf(v.val);
      });
    }
  });

});
