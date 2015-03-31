/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
define(function () {

  var $ = jQuery,
      GATE_METRIC = 'quality_gate_details',
      SIZE_METRIC = 'ncloc',
      ISSUES_METRIC = 'violations',
      DEBT_METRIC = 'sqale_index',
      COVERAGE_METRIC = 'overall_coverage',
      NEW_COVERAGE_METRIC = 'new_overall_coverage',
      DUPLICATIONS_METRIC = 'duplicated_lines_density';

  return Backbone.Model.extend({

    defaults: function () {
      return {
        qualityGateStatus: 'ERROR'
      };
    },

    hasPeriod: function (index) {
      var property = 'period' + index + 'Date';
      return !!this.get(property);
    },

    fetch: function () {
      var that = this;
      this.fetchMetrics().done(function () {
        that.fetchMeasures();
        that.fetchTrends();
        that.fetchIssues();
      });
    },

    fetchMetrics: function () {
      var that = this,
          url = baseUrl + '/api/metrics';
      return $.get(url).done(function (r) {
        that.set('metrics', r);
      });
    },

    fetchMeasures: function () {
      var that = this,
          url = baseUrl + '/api/resources/index',
          options = {
            resource: this.get('componentKey'),
            metrics: [
              GATE_METRIC,
              SIZE_METRIC,
              DEBT_METRIC,
              COVERAGE_METRIC,
              NEW_COVERAGE_METRIC,
              DUPLICATIONS_METRIC
            ].join(','),
            includetrends: true
          };
      return $.get(url, options).done(function (r) {
        if (!_.isArray(r) || !_.isArray(r[0].msr)) {
          return;
        }
        that.parseGate(r[0].msr);
        that.parseSize(r[0].msr);
        that.parseDebt(r[0].msr);
        that.parseCoverage(r[0].msr);
        that.parseDuplications(r[0].msr);
      });
    },

    parseGate: function (msr) {
      var that = this,
          measure = _.findWhere(msr, { key: GATE_METRIC });
      if (measure != null) {
        var metrics = this.get('metrics'),
            gateData = JSON.parse(measure.data),
            gateConditions = gateData.conditions,
            gateConditionsWithMetric = gateConditions.map(function (c) {
              var metric = _.findWhere(metrics, { key: c.metric }),
                  type = metric != null ? metric.val_type : null,
                  periodDate = that.get('period' + c.period + 'Date'),
                  periodName = that.get('period' + c.period + 'Name');
              return _.extend(c, {
                type: type,
                periodName: periodName,
                periodDate: periodDate
              });
            });
        this.set({
          gateStatus: gateData.level,
          gateConditions: gateConditionsWithMetric
        });
      }
    },

    parseSize: function (msr) {
      var nclocMeasure = _.findWhere(msr, { key: SIZE_METRIC });
      if (nclocMeasure != null) {
        this.set({
          ncloc: nclocMeasure.val,
          ncloc1: nclocMeasure.var1,
          ncloc2: nclocMeasure.var2,
          ncloc3: nclocMeasure.var3
        });
      }
    },

    fetchIssues: function () {
      var that = this;

      function _fetch (field, createdAfter) {
        var url = baseUrl + '/api/issues/search',
            options = {
              ps: 1,
              resolved: 'false',
              componentUuids: that.get('componentUuid')
            };
        if (createdAfter != null) {
          _.extend(options, { createdAfter: createdAfter });
        }
        return $.get(url, options).done(function (r) {
          that.set(field, r.total);
        });
      }

      _fetch('issues', null);
      if (this.hasPeriod(1)) {
        _fetch('issues1', this.get('period1Date'));
      }
      if (this.hasPeriod(2)) {
        _fetch('issues2', this.get('period2Date'));
      }
      if (this.hasPeriod(3)) {
        _fetch('issues3', this.get('period3Date'));
      }
    },

    parseDebt: function (msr) {
      var debtMeasure = _.findWhere(msr, { key: DEBT_METRIC });
      if (debtMeasure != null) {
        this.set({
          debt: debtMeasure.val,
          debt1: debtMeasure.var1,
          debt2: debtMeasure.var2,
          debt3: debtMeasure.var3
        });
      }
    },

    parseCoverage: function (msr) {
      var coverageMeasure = _.findWhere(msr, { key: COVERAGE_METRIC }),
          newCoverageMeasure = _.findWhere(msr, { key: NEW_COVERAGE_METRIC });
      if (coverageMeasure != null) {
        this.set({
          coverage: coverageMeasure.val,
          coverage1: coverageMeasure.var1,
          coverage2: coverageMeasure.var2,
          coverage3: coverageMeasure.var3
        });
      }
      if (newCoverageMeasure != null) {
        this.set({
          newCoverage1: newCoverageMeasure.var1,
          newCoverage2: newCoverageMeasure.var2,
          newCoverage3: newCoverageMeasure.var3
        });
      }
    },

    parseDuplications: function (msr) {
      var duplicationsMeasure = _.findWhere(msr, { key: DUPLICATIONS_METRIC });
      if (duplicationsMeasure != null) {
        this.set({
          duplications: duplicationsMeasure.val,
          duplications1: duplicationsMeasure.var1,
          duplications2: duplicationsMeasure.var2,
          duplications3: duplicationsMeasure.var3
        });
      }
    },

    fetchTrends: function () {
      var that = this,
          url = baseUrl + '/api/timemachine/index',
          options = {
            resource: this.get('componentKey'),
            metrics: [
              SIZE_METRIC,
              ISSUES_METRIC,
              DEBT_METRIC,
              COVERAGE_METRIC,
              DUPLICATIONS_METRIC
            ].join(',')
          };
      return $.get(url, options).done(function (r) {
        if (_.isArray(r)) {
          that.parseSizeTrend(r[0]);
          that.parseIssuesTrend(r[0]);
          that.parseDebtTrend(r[0]);
          that.parseCoverageTrend(r[0]);
          that.parseDuplicationsTrend(r[0]);
        }
      });
    },

    parseTrend: function (r, property, metric) {
      var that = this,
          index = _.pluck(r.cols, 'metric').indexOf(metric);
      if (index !== -1) {
        var trend = r.cells.map(function (cell) {
              return { val: cell.d, count: cell.v[index] };
            }),
            filteredTrend = trend.filter(function (t) {
              return t.val != null && t.count != null;
            });
        that.set(property, filteredTrend);
      }
    },

    parseSizeTrend: function (r) {
      this.parseTrend(r, 'sizeTrend', SIZE_METRIC);
    },

    parseIssuesTrend: function (r) {
      this.parseTrend(r, 'issuesTrend', ISSUES_METRIC);
    },

    parseDebtTrend: function (r) {
      this.parseTrend(r, 'debtTrend', DEBT_METRIC);
    },

    parseCoverageTrend: function (r) {
      this.parseTrend(r, 'coverageTrend', COVERAGE_METRIC);
    },

    parseDuplicationsTrend: function (r) {
      this.parseTrend(r, 'duplicationsTrend', DUPLICATIONS_METRIC);
    }
  });

});
