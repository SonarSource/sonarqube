/* global _:false, $j:false, Backbone:false, baseUrl:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

(function() {

  var DetailsRangeFilterView = window.SS.DetailsFilterView.extend({
    template: '#rangeFilterTemplate',


    events: {
      'change input': 'change'
    },


    change: function() {
      var value = {},
          valueFrom = this.$('input').eq(0).val(),
          valueTo = this.$('input').eq(1).val();

      if (valueFrom.length > 0) {
        value[this.model.get('propertyFrom')] = valueFrom;
      }

      if (valueTo.length > 0) {
        value[this.model.get('propertyTo')] = valueTo;
      }

      this.model.set('value', value);
    }

  });



  var RangeFilterView = window.SS.BaseFilterView.extend({

    initialize: function() {
      window.SS.BaseFilterView.prototype.initialize.call(this, {
        detailsView: DetailsRangeFilterView
      });
    },


    renderValue: function() {
      var value = this.model.get('value');
      if (_.isObject(value) && (value.from || value.to)) {
        return 'From ' + (value.from || '') + ' to ' + (value.to || '');
      } else {
        return 'Any';
      }
    }

  });



  var DateRangeFilterView = RangeFilterView.extend({

    render: function() {
      RangeFilterView.prototype.render.apply(this, arguments);
      this.detailsView.$('input').prop('placeholder', '1970-01-01');
    },


    renderValue: function() {
      if (!this.isDefaultValue()) {
        var value = _.values(this.model.get('value'));
        return value.join(' — ');
      } else {
        return 'Anytime';
      }
    },


    isDefaultValue: function() {
      var value = this.model.get('value'),
          propertyFrom = this.model.get('propertyFrom'),
          propertyTo = this.model.get('propertyTo'),
          valueFrom = _.isObject(value) && value[propertyFrom],
          valueTo = _.isObject(value) && value[propertyTo];

      return !valueFrom && !valueTo;
    }

  });



  /*
   * Export public classes
   */

  _.extend(window.SS, {
    RangeFilterView: RangeFilterView,
    DateRangeFilterView: DateRangeFilterView
  });

})();
