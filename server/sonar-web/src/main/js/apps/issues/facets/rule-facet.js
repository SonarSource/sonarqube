import _ from 'underscore';
import CustomValuesFacet from './custom-values-facet';

export default CustomValuesFacet.extend({
  prepareSearch: function () {
    var url = baseUrl + '/api/rules/search?f=name,langName',
        languages = this.options.app.state.get('query').languages;
    if (languages != null) {
      url += '&languages=' + languages;
    }
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
        url: url,
        data: function (term, page) {
          return { q: term, p: page };
        },
        results: function (data) {
          var results;
          results = data.rules.map(function (rule) {
            var lang = rule.langName || window.t('manual');
            return {
              id: rule.key,
              text: '(' + lang + ') ' + rule.name
            };
          });
          return {
            more: data.p * data.ps < data.total,
            results: results
          };
        }
      }
    });
  },

  getValuesWithLabels: function () {
    var values = this.model.getValues(),
        rules = this.options.app.facets.rules;
    values.forEach(function (v) {
      var key = v.val,
          label = '',
          extra = '';
      if (key) {
        var rule = _.findWhere(rules, { key: key });
        if (rule != null) {
          label = rule.name;
        }
        if (rule != null) {
          extra = rule.langName;
        }
      }
      v.label = label;
      v.extra = extra;
    });
    return values;
  },

  serializeData: function () {
    return _.extend(CustomValuesFacet.prototype.serializeData.apply(this, arguments), {
      values: this.sortValues(this.getValuesWithLabels())
    });
  }
});


