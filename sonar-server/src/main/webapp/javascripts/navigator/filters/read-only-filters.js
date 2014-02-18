define(['backbone', 'navigator/filters/base-filters'], function (Backbone, BaseFilters) {

  return BaseFilters.BaseFilterView.extend({
    className: 'navigator-filter navigator-filter-read-only',


    events: {
      'click .navigator-filter-disable': 'disable'
    },


    isDefaultValue: function() {
      return false;
    },


    renderValue: function() {
      var value = this.model.get('value'),
          format = this.model.get('format');
      return value ? (format ? format(value) : value) : '';
    }

  });

});
