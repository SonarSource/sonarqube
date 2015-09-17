import $ from 'jquery';
import _ from 'underscore';
import BaseFacet from './base-facet';
import '../templates';

export default BaseFacet.extend({
  template: Templates['issues-file-facet'],

  onRender: function () {
    BaseFacet.prototype.onRender.apply(this, arguments);
    var maxValueWidth = _.max(this.$('.facet-stat').map(function () {
      return $(this).outerWidth();
    }).get());
    return this.$('.facet-name').css('padding-right', maxValueWidth);
  },

  getValuesWithLabels: function () {
    var values = this.model.getValues(),
        source = this.options.app.facets.components;
    values.forEach(function (v) {
      var key = v.val,
          label = null;
      if (key) {
        var item = _.findWhere(source, { uuid: key });
        if (item != null) {
          label = item.longName;
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


