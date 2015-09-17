import _ from 'underscore';
import CustomValuesFacet from './custom-values-facet';

export default CustomValuesFacet.extend({

  getUrl: function () {
    return baseUrl + '/api/languages/list';
  },

  prepareAjaxSearch: function () {
    return {
      quietMillis: 300,
      url: this.getUrl(),
      data: function (term) {
        return { q: term, ps: 10000 };
      },
      results: function (data) {
        return {
          more: false,
          results: data.languages.map(function (lang) {
            return { id: lang.key, text: lang.name };
          })
        };
      }
    };
  },

  getLabelsSource: function () {
    return this.options.app.languages;
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
    return _.extend(CustomValuesFacet.prototype.serializeData.apply(this, arguments), {
      values: this.getValues()
    });
  }

});


