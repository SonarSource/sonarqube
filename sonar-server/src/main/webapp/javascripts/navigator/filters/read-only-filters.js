define(['backbone', 'navigator/filters/base-filters'], function (Backbone, BaseFilters) {

  return BaseFilters.BaseFilterView.extend({
    className: 'navigator-filter navigator-filter-read-only',


    events: {
      'click .navigator-filter-disable': 'disable'
    },


    isDefaultValue: function() {
      return false;
    }

  });

});
