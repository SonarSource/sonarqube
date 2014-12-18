define([
  'coding-rules/facets/base-facet'
], function (BaseFacet) {

  var $ = jQuery;

  return BaseFacet.extend({

    getValues: function () {
      var that = this,
          values = this.options.app.qualityProfiles.map(function (profile) {
            return {
              label: profile.name,
              extra: that.options.app.languages[profile.lang],
              val: profile.key
            };
          });
      return _.sortBy(values, 'label');
    },

    toggleFacet: function (e) {
      var obj = {},
          property = this.model.get('property');
      if ($(e.currentTarget).is('.active')) {
        obj.activation = null;
        obj[property] = null;
      } else {
        obj.activation = true;
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
