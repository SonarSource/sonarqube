/* global _:false, $j:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

(function() {

  var DetailsMetricFilterView = window.SS.DetailsFilterView.extend({
    template: '#metricFilterTemplate',


    events: {
      'change :input': 'inputChanged'
    },


    inputChanged: function() {
      var value = {
        metric: this.$('[name=metric]').val(),
        metricText: this.$('[name=metric] option:selected').text(),
        period: this.$('[name=period]').val(),
        periodText: this.$('[name=period] option:selected').text(),
        op: this.$('[name=op]').val(),
        opText: this.$('[name=op] option:selected').text(),
        val: this.$('[name=val]').val()
      };
      this.model.set('value', value);
    },


    onRender: function() {
      var value = this.model.get('value') || {};
      this.$('[name=metric]').val(value.metric).select2({
        width: '100%',
        placeholder: window.SS.phrases.metric
      });
      this.$('[name=period]').val(value.period || 0).select2({
        width: '100%',
        minimumResultsForSearch: 100
      });
      this.$('[name=op]').val(value.op || 'eq').select2({
        width: '60px',
        placeholder: '=',
        minimumResultsForSearch: 100
      });
      this.$('[name=val]').val(value.val);
      this.inputChanged();
    },


    onShow: function() {
      var select = this.$('[name=metric]');
      if (!select.val()) {
        select.select2('open');
      }
    }

  });



  var MetricFilterView = window.SS.BaseFilterView.extend({

    initialize: function() {
      window.SS.BaseFilterView.prototype.initialize.call(this, {
        detailsView: DetailsMetricFilterView
      });

      this.groupMetrics();
    },


    groupMetrics: function() {
      var metrics = _.map(this.model.get('metrics'), function (metric) {
            return metric.metric;
          }),
          groupedMetrics =
              _.sortBy(
                  _.map(
                      _.groupBy(metrics, 'domain'),
                      function (metrics, domain) {
                        return {
                          domain: domain,
                          metrics: _.sortBy(metrics, 'short_name')
                        };
                      }),
                  'domain'
              );
      this.model.set('groupedMetrics', groupedMetrics);
    },


    renderValue: function() {
      return this.isDefaultValue() ?
          window.SS.phrases.notSet :
          this.model.get('value').metricText + ' ' + this.model.get('value').opText + ' ' + this.model.get('value').val;
    },


    renderInput: function() {
      var that = this,
          value = this.model.get('value');

      if (_.isObject(value) && value.metric && value.op && value.val) {
        _.each(['metric', 'period', 'op', 'val'], function(key) {
          var v = value[key];
          if (key === 'period' && v === '0') {
            v = '';
          }

          $j('<input>')
              .prop('name', that.model.get('property') + '_' + key)
              .prop('type', 'hidden')
              .css('display', 'none')
              .val(v)
              .appendTo(that.$el);
        });
      }
    },


    isDefaultValue: function() {
      var value = this.model.get('value');
      if (!_.isObject(value)) {
        return true;
      }
      return !(value.metric && value.op && value.val);
    },


    restoreFromQuery: function(q) {
      var that = this,
          value = {};
      _.each(['metric', 'period', 'op', 'val'], function(p) {
        var property = that.model.get('property') + '_' + p,
            pValue = _.findWhere(q, { key: property });

        if (pValue && pValue.value) {
          value[p] = pValue.value;
        }
      });

      if (value && value.metric && value.op && value.val) {
        this.model.set({
          value: value,
          enabled: true
        });
      }
    }

  });



  /*
   * Export public classes
   */

  _.extend(window.SS, {
    MetricFilterView: MetricFilterView
  });

})();
