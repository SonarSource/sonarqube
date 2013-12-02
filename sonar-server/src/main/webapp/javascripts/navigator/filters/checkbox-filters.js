/* global _:false, $j:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

(function() {

  var CheckboxFilterView = window.SS.BaseFilterView.extend({
    template: '#checkboxFilterTemplate',
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



  /*
   * Export public classes
   */

  _.extend(window.SS, {
    CheckboxFilterView: CheckboxFilterView
  });

})();
