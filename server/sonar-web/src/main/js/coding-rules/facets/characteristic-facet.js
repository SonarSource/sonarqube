define([
    'coding-rules/facets/base-facet'
], function (BaseFacet) {

  var $ = jQuery;

  return BaseFacet.extend({

    getValues: function () {
      var values = _.map(this.options.app.characteristics, function (value, key) {
        return {
          label: value,
          val: key
        };
      });
      return _.sortBy(values, 'label');
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
        values: this.getValues()
      });
    }
  });

});
