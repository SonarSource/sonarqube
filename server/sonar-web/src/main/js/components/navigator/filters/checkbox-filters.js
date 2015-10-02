import $ from 'jquery';
import BaseFilters from './base-filters';
import Template from '../templates/checkbox-filter.hbs';

export default BaseFilters.BaseFilterView.extend({
  template: Template,
  className: 'navigator-filter navigator-filter-inline',


  events: function () {
    return {
      'click .navigator-filter-disable': 'disable'
    };
  },


  showDetails: function () {
  },


  renderInput: function () {
    if (this.model.get('enabled')) {
      $('<input>')
          .prop('name', this.model.get('property'))
          .prop('type', 'checkbox')
          .prop('value', 'true')
          .prop('checked', true)
          .css('display', 'none')
          .appendTo(this.$el);
    }
  },


  renderValue: function () {
    return this.model.get('value');
  },


  isDefaultValue: function () {
    return false;
  },


  restore: function (value) {
    this.model.set({
      value: value,
      enabled: true
    });
  }

});


