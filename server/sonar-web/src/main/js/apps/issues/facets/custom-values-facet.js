import _ from 'underscore';
import BaseFacet from './base-facet';
import '../templates';

export default BaseFacet.extend({
  template: Templates['issues-custom-values-facet'],

  events: function () {
    return _.extend(BaseFacet.prototype.events.apply(this, arguments), {
      'change .js-custom-value': 'addCustomValue'
    });
  },

  getUrl: function () {

  },

  onRender: function () {
    BaseFacet.prototype.onRender.apply(this, arguments);
    return this.prepareSearch();
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
    return this.options.app.state.updateFilter(obj);
  }
});


