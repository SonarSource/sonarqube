/* global _:false, $j:false, Backbone:false, baseUrl:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

(function() {

  var DetailsSelectFilterView = window.SS.DetailsFilterView.extend({
    template: '#selectFilterTemplate',


    events: {
      'change input[type=checkbox]': 'changeSelection'
    },


    changeSelection: function() {
      var value = this.$('input[type=checkbox]:checked').map(function() {
        return $j(this).val();
      }).get();
      this.model.set('value', value);
    }
  });



  var SelectFilterView = window.SS.BaseFilterView.extend({

    initialize: function() {
      window.SS.BaseFilterView.prototype.initialize.call(this, {
        detailsView: DetailsSelectFilterView
      });
    },


    renderValue: function() {
      var choices = this.model.get('choices'),
          value = (this.model.get('value') || []).map(function(key) {
            return choices[key];
          });

      return this.isDefaultValue() ? 'All' : value.join(', ');
    },


    isDefaultValue: function() {
      var value = this.model.get('value'),
          choices = this.model.get('choices');

      return !(_.isArray(value) &&
          value.length > 0 &&
          value.length < Object.keys(choices).length);
    }

  });



  /*
   * Export public classes
   */

  _.extend(window.SS, {
    SelectFilterView: SelectFilterView
  });

})();
