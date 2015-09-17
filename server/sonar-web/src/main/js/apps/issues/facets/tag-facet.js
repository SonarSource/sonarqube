import _ from 'underscore';
import CustomValuesFacet from './custom-values-facet';

export default CustomValuesFacet.extend({
  prepareSearch: function () {
    var url = baseUrl + '/api/issues/tags?ps=10',
        tags = this.options.app.state.get('query').tags;
    if (tags != null) {
      url += '&tags=' + tags;
    }
    return this.$('.js-custom-value').select2({
      placeholder: 'Search...',
      minimumInputLength: 0,
      allowClear: false,
      formatNoMatches: function () {
        return t('select2.noMatches');
      },
      formatSearching: function () {
        return t('select2.searching');
      },
      width: '100%',
      ajax: {
        quietMillis: 300,
        url: url,
        data: function (term) {
          return { q: term, ps: 10 };
        },
        results: function (data) {
          var results = data.tags.map(function (tag) {
            return { id: tag, text: tag };
          });
          return { more: false, results: results };
        }
      }
    });
  },

  getValuesWithLabels: function () {
    var values = this.model.getValues();
    values.forEach(function (v) {
      v.label = v.val;
      v.extra = '';
    });
    return values;
  },

  serializeData: function () {
    return _.extend(CustomValuesFacet.prototype.serializeData.apply(this, arguments), {
      values: this.sortValues(this.getValuesWithLabels())
    });
  }
});


