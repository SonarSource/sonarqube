import $ from 'jquery';
import _ from 'underscore';
import BaseFilters from './base-filters';
import Template from '../templates/metric-filter.hbs';

var DetailsMetricFilterView = BaseFilters.DetailsFilterView.extend({
  template: Template,


  events: {
    'change :input': 'inputChanged'
  },


  inputChanged: function () {
    var metric = this.$('[name=metric]').val(),
        isDifferentialMetric = metric.indexOf('new_') === 0,
        periodSelect = this.$('[name=period]'),
        period = periodSelect.val(),
        optionZero = periodSelect.children('[value="0"]'),
        value = {
          metric: metric,
          metricText: this.$('[name=metric] option:selected').text(),
          period: period,
          periodText: this.$('[name=period] option:selected').text(),
          op: this.$('[name=op]').val(),
          opText: this.$('[name=op] option:selected').text(),
          val: this.$('[name=val]').val(),
          valText: this.$('[name=val]').originalVal()
        };

    if (isDifferentialMetric) {
      optionZero.remove();
      if (period === '0') {
        period = '1';
      }
    } else {
      if (optionZero.length === 0) {
        periodSelect.prepend(this.periodZeroOption);
      }
    }
    periodSelect.select2('destroy').val(period).select2({
      width: '100%',
      minimumResultsForSearch: 100
    });

    this.updateDataType(value);
    this.model.set('value', value);
  },


  updateDataType: function (value) {
    var metric = _.find(window.SS.metrics, function (m) {
      return m.metric.name === value.metric;
    });
    if (metric) {
      this.$('[name=val]').data('type', metric.metric.val_type);
      if (metric.metric.val_type === 'WORK_DUR') {
        this.$('[name=val]').prop('placeholder', '1d 7h 59min');
      }
      if (metric.metric.val_type === 'RATING') {
        this.$('[name=val]').prop('placeholder', 'A');
      }
    }
  },


  onRender: function () {
    var periodZeroLabel = this.$('[name=period]').children('[value="0"]').html();
    this.periodZeroOption = '<option value="0">' + periodZeroLabel + '</option>';

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


  onShow: function () {
    var select = this.$('[name=metric]');
    if (this.model.get('value').metric === '') {
      select.select2('open');
    } else {
      select.select2('focus');
    }
  }

});


export default BaseFilters.BaseFilterView.extend({

  initialize: function () {
    BaseFilters.BaseFilterView.prototype.initialize.call(this, {
      detailsView: DetailsMetricFilterView
    });

    this.groupMetrics();
  },


  groupMetrics: function () {
    var metrics = _.map(this.model.get('metrics'), function (metric) {
          return metric.metric;
        }),
        groupedMetrics =
            _.sortBy(
                _.map(
                    _.groupBy(metrics, 'domain'),
                    function (metricList, domain) {
                      return {
                        domain: domain,
                        metrics: _.sortBy(metricList, 'short_name')
                      };
                    }),
                'domain'
            );
    this.model.set('groupedMetrics', groupedMetrics);
  },


  renderValue: function () {
    return this.isDefaultValue() ?
        window.SS.phrases.notSet :
    this.model.get('value').metricText + ' ' + this.model.get('value').opText + ' ' +
    this.model.get('value').valText;
  },


  renderInput: function () {
    var that = this,
        value = this.model.get('value');

    if (_.isObject(value) && value.metric && value.op && (value.val != null)) {
      _.each(['metric', 'period', 'op', 'val'], function (key) {
        var v = value[key];
        if (key === 'period' && v === '0') {
          v = '';
        }

        $('<input>')
            .prop('name', that.model.get('property') + '_' + key)
            .prop('type', 'hidden')
            .css('display', 'none')
            .val(v)
            .appendTo(that.$el);
      });
    }
  },


  isDefaultValue: function () {
    var value = this.model.get('value');
    if (!_.isObject(value)) {
      return true;
    }
    return !(value.metric && value.op && (value.val != null));
  },


  restoreFromQuery: function (q) {
    var that = this,
        value = {};
    _.each(['metric', 'period', 'op', 'val'], function (p) {
      var property = that.model.get('property') + '_' + p,
          pValue = _.findWhere(q, { key: property });

      if (pValue && pValue.value) {
        value[p] = pValue.value;
      }
    });

    if (value && value.metric && value.op && (value.val != null)) {
      this.model.set({
        value: value,
        enabled: true
      });
    }
  }

});


