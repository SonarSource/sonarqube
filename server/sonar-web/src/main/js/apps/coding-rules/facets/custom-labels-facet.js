import _ from 'underscore';
import BaseFacet from './base-facet';

export default BaseFacet.extend({

  getLabelsSource: function () {
    return [];
  },

  getValues: function () {
    var that = this,
        labels = that.getLabelsSource();
    return this.model.getValues().map(function (item) {
      return _.extend(item, {
        label: labels[item.val]
      });
    });
  },

  serializeData: function () {
    return _.extend(BaseFacet.prototype.serializeData.apply(this, arguments), {
      values: this.getValues()
    });
  }
});


