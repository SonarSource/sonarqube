define([
    'coding-rules/facets/base-facet'
], function (BaseFacet) {

  return BaseFacet.extend({

    getValues: function () {
      return ['BETA', 'DEPRECATED', 'READY'].map(function (s) {
        return {
          label: t('rules.status', s.toLowerCase()),
          val: s
        };
      });
    },

    serializeData: function () {
      return _.extend(BaseFacet.prototype.serializeData.apply(this, arguments), {
        values: this.sortValues(this.getValues())
      });
    }
  });

});
