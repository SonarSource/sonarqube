import _ from 'underscore';
import BaseFacet from './base-facet';
import Template from '../templates/facets/coding-rules-custom-values-facet.hbs';

export default BaseFacet.extend({
  template: Template,

  events: function () {
    return _.extend(BaseFacet.prototype.events.apply(this, arguments), {
      'change .js-custom-value': 'addCustomValue'
    });
  },

  getUrl: function () {
    return baseUrl;
  },

  onRender: function () {
    BaseFacet.prototype.onRender.apply(this, arguments);
    this.prepareSearch();
  },

  prepareSearch: function () {
    this.$('.js-custom-value').select2({
      placeholder: window.t('search_verb'),
      minimumInputLength: 1,
      allowClear: false,
      formatNoMatches: function () {
        return window.t('select2.noMatches');
      },
      formatSearching: function () {
        return window.t('select2.searching');
      },
      formatInputTooShort: function () {
        return window.tp('select2.tooShort', 1);
      },
      width: '100%',
      ajax: this.prepareAjaxSearch()
    });
  },

  prepareAjaxSearch: function () {
    return {
      quietMillis: 300,
      url: this.getUrl(),
      data: function (term, page) {
        return { s: term, p: page };
      },
      results: function (data) {
        return { more: data.more, results: data.results };
      }
    };
  },

  addCustomValue: function () {
    var property = this.model.get('property'),
        customValue = this.$('.js-custom-value').select2('val'),
        value = this.getValue();
    if (value.length > 0) {
      value += ',';
    }
    value += customValue;
    var obj = {};
    obj[property] = value;
    this.options.app.state.updateFilter(obj);
  }
});
