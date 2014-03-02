define(['backbone', 'backbone.marionette', 'navigator/filters/base-filters', 'common/handlebars-extensions'], function (Backbone, Marionette, BaseFilters) {

  return BaseFilters.BaseFilterView.extend({
    template: getTemplate('#checkbox-filter-template'),
    className: 'navigator-filter navigator-filter-inline',


    events: function() {
      return {
        'click .navigator-filter-disable': 'disable'
      };
    },


    showDetails: function() {},


    renderInput: function() {
      if (this.model.get('enabled')) {
        $j('<input>')
            .prop('name', this.model.get('property'))
            .prop('type', 'checkbox')
            .prop('value', 'true')
            .prop('checked', true)
            .css('display', 'none')
            .appendTo(this.$el);
      }
    },


    renderValue: function() {
      return this.model.get('value') || false;
    },


    isDefaultValue: function() {
      return false;
    },


    restore: function(value) {
      this.model.set({
        value: value,
        enabled: true
      });
    }

  });

});
