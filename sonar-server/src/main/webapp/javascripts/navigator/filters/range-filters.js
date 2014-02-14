/* global _:false, $j:false */

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
    },


    populateInputs: function() {
      var value = this.model.get('value'),
          propertyFrom = this.model.get('propertyFrom'),
          propertyTo = this.model.get('propertyTo'),
          valueFrom = _.isObject(value) && value[propertyFrom],
          valueTo = _.isObject(value) && value[propertyTo];

      this.$('input').eq(0).val(valueFrom || '');
      this.$('input').eq(1).val(valueTo || '');
    },


    onShow: function() {
      this.$(':input:first').focus();
    }

  });



  var RangeFilterView = window.SS.BaseFilterView.extend({

    initialize: function() {
      window.SS.BaseFilterView.prototype.initialize.call(this, {
        detailsView: DetailsRangeFilterView
      });
    },


    renderValue: function() {
      if (!this.isDefaultValue()) {
        var value = _.values(this.model.get('value'));
        return value.join(' — ');
      } else {
        return window.SS.phrases.any;
      }
    },


    renderInput: function() {
      var value = this.model.get('value'),
          propertyFrom = this.model.get('propertyFrom'),
          propertyTo = this.model.get('propertyTo'),
          valueFrom = _.isObject(value) && value[propertyFrom],
          valueTo = _.isObject(value) && value[propertyTo];

      $j('<input>')
          .prop('name', propertyFrom)
          .prop('type', 'hidden')
          .css('display', 'none')
          .val(valueFrom || '')
          .appendTo(this.$el);

      $j('<input>')
          .prop('name', propertyTo)
          .prop('type', 'hidden')
          .css('display', 'none')
          .val(valueTo || '')
          .appendTo(this.$el);
    },


    isDefaultValue: function() {
      var value = this.model.get('value'),
          propertyFrom = this.model.get('propertyFrom'),
          propertyTo = this.model.get('propertyTo'),
          valueFrom = _.isObject(value) && value[propertyFrom],
          valueTo = _.isObject(value) && value[propertyTo];

      return !valueFrom && !valueTo;
    },


    restoreFromQuery: function(q) {
      var paramFrom = _.findWhere(q, { key: this.model.get('propertyFrom') }),
          paramTo = _.findWhere(q, { key: this.model.get('propertyTo') }),
          value = {};

      if ((paramFrom && paramFrom.value) || (paramTo && paramTo.value)) {
        if (paramFrom && paramFrom.value) {
          value[this.model.get('propertyFrom')] = paramFrom.value;
        }

        if (paramTo && paramTo.value) {
          value[this.model.get('propertyTo')] = paramTo.value;
        }

        this.model.set({
          value: value,
          enabled: true
        });

        this.detailsView.populateInputs();
      }
    },


    restore: function(value) {
      if (this.choices && this.selection && value.length > 0) {
        var that = this;
        this.choices.add(this.selection.models);
        this.selection.reset([]);

        _.each(value, function(v) {
          var cModel = that.choices.findWhere({ id: v });

          if (cModel) {
            that.selection.add(cModel);
            that.choices.remove(cModel);
          }
        });

        this.detailsView.updateLists();

        this.model.set({
          value: value,
          enabled: true
        });
      }
    },


    formatValue: function() {
      return this.model.get('value');
    },


    clear: function() {
      this.model.unset('value');
      this.detailsView.render();
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
        return window.SS.phrases.anytime;
      }
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
