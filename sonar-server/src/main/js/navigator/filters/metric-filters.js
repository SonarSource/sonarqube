define(['navigator/filters/base-filters', 'common/handlebars-extensions'], function (BaseFilters) {

  var DetailsMetricFilterView = BaseFilters.DetailsFilterView.extend({
    template: getTemplate('#metric-filter-template'),


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
        val: this.$('[name=val]').val(),
        valText: this.$('[name=val]').originalVal()
      };
      this.updateDataType(value);
      this.model.set('value', value);
    },


    updateDataType: function(value) {
      var metric = _.find(window.SS.metrics, function(m) {
        return m.metric.name === value.metric;
      });
      if (metric) {
        this.$('[name=val]').data('type', metric.metric.val_type);
        switch (metric.metric.val_type) {
            case 'WORK_DUR':
                this.$('[name=val]').prop('placeholder', '1d 7h 59min');
                break;
            case 'RATING':
                this.$('[name=val]').prop('placeholder', 'A');
                break;
        }
      }
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
      this.updateDataType(value);
      this.$('[name=val]').val(value.val);
      this.inputChanged();
    },


    onShow: function() {
      var select = this.$('[name=metric]');
      select.select2('open');
    }

  });



  return BaseFilters.BaseFilterView.extend({

    initialize: function() {
      BaseFilters.BaseFilterView.prototype.initialize.call(this, {
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
          this.model.get('value').metricText + ' ' + this.model.get('value').opText + ' ' + this.model.get('value').valText;
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

});
