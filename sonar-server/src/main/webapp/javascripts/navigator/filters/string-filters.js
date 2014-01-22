/* global _:false, $j:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

(function() {

  var DetailsStringFilterView = window.SS.DetailsFilterView.extend({
    template: '#stringFilterTemplate',


    events: {
      'change input': 'change'
    },


    change: function(e) {
      this.model.set('value', $j(e.target).val());
    },


    onShow: function() {
      window.SS.DetailsFilterView.prototype.onShow.apply(this, arguments);
      this.$(':input').focus();
    },


    serializeData: function() {
      return _.extend({}, this.model.toJSON(), {
        value: this.model.get('value') || ''
      });
    }

  });



  var StringFilterView = window.SS.BaseFilterView.extend({

    initialize: function() {
      window.SS.BaseFilterView.prototype.initialize.call(this, {
        detailsView: DetailsStringFilterView
      });
    },


    renderValue: function() {
      return this.isDefaultValue() ? '—' : this.model.get('value');
    },


    renderInput: function() {
      $j('<input>')
          .prop('name', this.model.get('property'))
          .prop('type', 'hidden')
          .css('display', 'none')
          .val(this.model.get('value') || '')
          .appendTo(this.$el);
    },


    isDefaultValue: function() {
      return !this.model.get('value');
    },


    restore: function(value) {
      this.model.set({
        value: value,
        enabled: true
      });
    },


    clear: function() {
      this.model.unset('value');
      this.detailsView.render();
    }

  });



  /*
   * Export public classes
   */

  _.extend(window.SS, {
    StringFilterView: StringFilterView
  });

})();
