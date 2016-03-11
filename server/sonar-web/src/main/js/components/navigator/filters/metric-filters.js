/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import $ from 'jquery';
import _ from 'underscore';
import BaseFilters from './base-filters';
import Template from '../templates/metric-filter.hbs';
import { translate } from '../../../helpers/l10n';

const DetailsMetricFilterView = BaseFilters.DetailsFilterView.extend({
  template: Template,


  events: {
    'change :input': 'inputChanged'
  },


  inputChanged () {
    const metric = this.$('[name=metric]').val();
    const isDifferentialMetric = metric.indexOf('new_') === 0;
    const periodSelect = this.$('[name=period]');
    let period = periodSelect.val();
    const optionZero = periodSelect.children('[value="0"]');
    const value = {
      metric,
      period,
      metricText: this.$('[name=metric] option:selected').text(),
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


  updateDataType (value) {
    const metric = _.find(window.SS.metrics, function (m) {
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


  onRender () {
    const periodZeroLabel = this.$('[name=period]').children('[value="0"]').html();
    this.periodZeroOption = `<option value="0">${periodZeroLabel}</option>`;

    const value = this.model.get('value') || {};
    this.$('[name=metric]').val(value.metric).select2({
      width: '100%',
      placeholder: translate('measure_filter.criteria.metric')
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


  onShow () {
    const select = this.$('[name=metric]');
    if (this.model.get('value').metric === '') {
      select.select2('open');
    } else {
      select.select2('focus');
    }
  }

});


export default BaseFilters.BaseFilterView.extend({

  initialize () {
    BaseFilters.BaseFilterView.prototype.initialize.call(this, {
      projectsView: DetailsMetricFilterView
    });

    this.groupMetrics();
  },


  groupMetrics () {
    const metrics = _.map(this.model.get('metrics'), function (metric) {
      return metric.metric;
    });
    const groupedMetrics =
        _.sortBy(
            _.map(
                _.groupBy(metrics, 'domain'),
                function (metricList, domain) {
                  return {
                    domain,
                    metrics: _.sortBy(metricList, 'short_name')
                  };
                }),
            'domain'
        );
    this.model.set('groupedMetrics', groupedMetrics);
  },


  renderValue () {
    return this.isDefaultValue() ?
        translate('measure_filter.criteria.metric.not_set') :
    this.model.get('value').metricText + ' ' + this.model.get('value').opText + ' ' +
    this.model.get('value').valText;
  },


  renderInput () {
    const that = this;
    const value = this.model.get('value');

    if (_.isObject(value) && value.metric && value.op && (value.val != null)) {
      _.each(['metric', 'period', 'op', 'val'], function (key) {
        let v = value[key];
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


  isDefaultValue () {
    const value = this.model.get('value');
    if (!_.isObject(value)) {
      return true;
    }
    return !(value.metric && value.op && (value.val != null));
  },


  restoreFromQuery (q) {
    const that = this;
    const value = {};
    _.each(['metric', 'period', 'op', 'val'], function (p) {
      const property = that.model.get('property') + '_' + p;
      const pValue = _.findWhere(q, { key: property });

      if (pValue && pValue.value) {
        value[p] = pValue.value;
      }
    });

    if (value && value.metric && value.op && (value.val != null)) {
      this.model.set({
        value,
        enabled: true
      });
    }
  }

});


