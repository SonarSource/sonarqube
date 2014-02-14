/* global _:false, $j:false, Backbone:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

(function() {

  var ContextFilterView = Backbone.View.extend({

    initialize: function() {
      this.model.view = this;
    },


    render: function() {
      this.$el.hide();
    },


    renderBase: function() {},
    renderInput: function() {},
    focus: function() {},
    showDetails: function() {},
    registerShowedDetails: function() {},
    hideDetails: function() {},
    isActive: function() {},
    renderValue: function() {},
    isDefaultValue: function() {},


    restoreFromQuery: function(q) {
      var param = _.findWhere(q, { key: this.model.get('property') });
      if (param && param.value) {
        this.restore(param.value);
      } else {
        this.clear();
      }
    },


    restore: function(value) {
      this.model.set({ value: value });
    },


    clear: function() {
      this.model.unset('value');
    },


    disable: function(e) {
      e.stopPropagation();
      this.hideDetails();
      this.options.filterBarView.hideDetails();
      this.model.set({
        enabled: false,
        value: null
      });
    },


    formatValue: function() {
      var q = {};
      if (this.model.has('property') && this.model.has('value') && this.model.get('value')) {
        q[this.model.get('property')] = this.model.get('value');
      }
      return q;
    }
  });



  /*
   * Export public classes
   */

  _.extend(window.SS, {
    ContextFilterView: ContextFilterView
  });

})();
