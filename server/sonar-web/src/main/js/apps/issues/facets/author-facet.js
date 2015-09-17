import CustomValuesFacet from './custom-values-facet';

export default CustomValuesFacet.extend({
  getUrl: function () {
    return baseUrl + '/api/issues/authors';
  },

  prepareSearch: function () {
    return this.$('.js-custom-value').select2({
      placeholder: 'Search...',
      minimumInputLength: 2,
      allowClear: false,
      formatNoMatches: function () {
        return t('select2.noMatches');
      },
      formatSearching: function () {
        return t('select2.searching');
      },
      formatInputTooShort: function () {
        return tp('select2.tooShort', 2);
      },
      width: '100%',
      ajax: {
        quietMillis: 300,
        url: this.getUrl(),
        data: function (term) {
          return { q: term, ps: 25 };
        },
        results: function (data) {
          return {
            more: false,
            results: data.authors.map(function (author) {
              return { id: author, text: author };
            })
          };
        }
      }
    });
  }
});


