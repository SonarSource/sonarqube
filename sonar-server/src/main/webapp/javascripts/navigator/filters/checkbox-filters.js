/* global _:false */

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


    renderInput: function() {},


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
