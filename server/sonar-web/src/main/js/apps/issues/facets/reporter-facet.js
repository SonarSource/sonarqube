import _ from 'underscore';
import CustomValuesFacet from './custom-values-facet';

export default CustomValuesFacet.extend({
  getUrl: function () {
    return baseUrl + '/api/users/search';
  },

  prepareAjaxSearch: function () {
    return {
      quietMillis: 300,
      url: this.getUrl(),
      data: function (term, page) {
        return { q: term, p: page };
      },
      results: window.usersToSelect2
    };
  },

  getValuesWithLabels: function () {
    var values = this.model.getValues(),
        source = this.options.app.facets.users;
    values.forEach(function (v) {
      var item, key, label;
      key = v.val;
      label = null;
      if (key) {
        item = _.findWhere(source, { login: key });
        if (item != null) {
          label = item.name;
        }
      }
      v.label = label;
    });
    return values;
  },

  serializeData: function () {
    return _.extend(CustomValuesFacet.prototype.serializeData.apply(this, arguments), {
      values: this.sortValues(this.getValuesWithLabels())
    });
  }
});


