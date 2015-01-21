define([
  'coding-rules/facets/base-facet',
  'templates/coding-rules'
], function (BaseFacet) {

  return BaseFacet.extend({
    template: Templates['coding-rules-characteristic-facet'],

    getValues: function () {
      var values = this.model.getValues(),
          characteristics = this.options.app.characteristics;
      return values.map(function (value) {
        var ch = _.findWhere(characteristics, { key: value.val });
        if (ch != null) {
          _.extend(value, ch, { label: ch.name });
        } else {
          _.extend(value, { label: t('coding_rules.noncharacterized') });
        }
        return value;
      });
    },

    sortValues: function (values) {
      return _.sortBy(values, 'index');
    },

    serializeData: function () {
      return _.extend(BaseFacet.prototype.serializeData.apply(this, arguments), {
        values: this.sortValues(this.getValues())
      });
    }
  });

});
