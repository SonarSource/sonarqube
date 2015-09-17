import CustomValuesFacet from './custom-values-facet';

export default CustomValuesFacet.extend({

  getUrl: function () {
    return baseUrl + '/api/rules/tags';
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
          results: data.tags.map(function (tag) {
            return { id: tag, text: tag };
          })
        };
      }
    };
  }

});


