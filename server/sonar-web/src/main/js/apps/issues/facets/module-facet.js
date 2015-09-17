import _ from 'underscore';
import BaseFacet from './base-facet';

export default BaseFacet.extend({
  getValuesWithLabels: function () {
    var values = this.model.getValues(),
        components = this.options.app.facets.components;
    values.forEach(function (v) {
      var uuid = v.val,
          label = uuid;
      if (uuid) {
        var component = _.findWhere(components, { uuid: uuid });
        if (component != null) {
          label = component.longName;
        }
      }
      v.label = label;
    });
    return values;
  },

  serializeData: function () {
    return _.extend(BaseFacet.prototype.serializeData.apply(this, arguments), {
      values: this.sortValues(this.getValuesWithLabels())
    });
  }
});


