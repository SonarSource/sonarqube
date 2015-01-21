define([
  'coding-rules/facets/base-facet'
], function (BaseFacet) {

  return BaseFacet.extend({

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
      return _.sortBy(values, function (v) {
        return v.val === 'NONE' ? -999999 : -v.count;
      });
    },

    serializeData: function () {
      return _.extend(BaseFacet.prototype.serializeData.apply(this, arguments), {
        values: this.sortValues(this.getValues())
      });
    }
  });

});
