define([
    'coding-rules/facets/base-facet'
], function (BaseFacet) {

  var $ = jQuery;

  return BaseFacet.extend({

    getValues: function () {
      var values = this.model.getValues(),
          characteristics = this.options.app.characteristics;
      return values.map(function (value) {
        var label = characteristics[value.val];
        if (value.val === 'NONE') {
          label = t('coding_rules.noncharacterized');
        }
        return _.extend(value, { label: label });
      });
    },

    sortValues: function (values) {
      return _.sortBy(values, function (v) {
        return v.val === 'NONE' ? -999999 : -v.count;
      });
    },

    toggleFacet: function (e) {
      var obj = {},
          property = this.model.get('property');
      if ($(e.currentTarget).is('.active')) {
        obj[property] = null;
      } else {
        obj[property] = $(e.currentTarget).data('value');
      }
      this.options.app.state.updateFilter(obj);
    },

    serializeData: function () {
      return _.extend(BaseFacet.prototype.serializeData.apply(this, arguments), {
        values: this.sortValues(this.getValues())
      });
    }
  });

});
