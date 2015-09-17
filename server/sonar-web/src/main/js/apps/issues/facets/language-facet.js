import _ from 'underscore';
import CustomValuesFacet from './custom-values-facet';

export default CustomValuesFacet.extend({
  getUrl: function () {
    return baseUrl + '/api/languages/list';
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
          return { q: term, ps: 0 };
        },
        results: function (data) {
          return {
            more: false,
            results: data.languages.map(function (lang) {
              return { id: lang.key, text: lang.name };
            })
          };
        }
      }
    });
  },

  getValuesWithLabels: function () {
    var values = this.model.getValues(),
        source = this.options.app.facets.languages;
    values.forEach(function (v) {
      var key = v.val,
          label = null;
      if (key) {
        var item = _.findWhere(source, { key: key });
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


