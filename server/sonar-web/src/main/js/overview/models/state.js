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

  var $ = jQuery;

  return Backbone.Model.extend({
    defaults: function () {
      return {
        qualityGateStatus: 'ERROR'
      };
    },

    fetch: function () {
      return $.when(
          this.fetchGate(),

          this.fetchSize(),
          this.fetchSizeTrend(),

          this.fetchIssues(),
          this.fetchIssues1(),
          this.fetchIssues2(),
          this.fetchIssues3(),
          this.fetchIssuesTrend(),

          this.fetchDebt(),
          this.fetchDebtTrend(),

          this.fetchCoverage(),
          this.fetchCoverageTrend(),

          this.fetchDuplications(),
          this.fetchDuplicationsTrend()
      );
    },

    fetchGate: function () {
      var that = this,
          url = baseUrl + '/api/resources/index',
          options = {
            resource: this.get('componentKey'),
            metrics: 'quality_gate_details'
          };
      return $.get(url, options).done(function (r) {
        if (!r || !r[0] || !r[0].msr || !r[0].msr[0] || !r[0].msr[0].data) {
          return;
        }
        var gateData = JSON.parse(r[0].msr[0].data),
            gateConditions = gateData.conditions,
            urlMetrics = baseUrl + '/api/metrics';
        $.get(urlMetrics).done(function (r) {
          var gateConditionsWithMetric = gateConditions.map(function (c) {
            var metric = _.findWhere(r, { key: c.metric }),
                type = metric != null ? metric.val_type : null,
                periodDate = that.get('period' + c.period + 'Date'),
                periodName = that.get('period' + c.period + 'Name');
            return _.extend(c, {
              type: type,
              periodName: periodName,
              periodDate: periodDate
            });
          });
          that.set({
            gateStatus: gateData.level,
            gateConditions: gateConditionsWithMetric
          });
        });
      });
    },

    fetchSize: function () {
      var that = this,
          url = baseUrl + '/api/resources/index',
          options = {
            resource: this.get('componentKey'),
            metrics: 'ncloc,ncloc_language_distribution,function_complexity,file_complexity',
            includetrends: true
          };
      return $.get(url, options).done(function (r) {
        var msr = r[0].msr,
            nclocMeasure = _.findWhere(msr, { key: 'ncloc' }),
            nclocLangMeasure = _.findWhere(msr, { key: 'ncloc_language_distribution' }),
            nclocLangParsed = nclocLangMeasure.data.split(';').map(function (token) {
              var tokens = token.split('=');
              return { key: tokens[0], value: +tokens[1] };
            }),
            nclocLangSorted = _.sortBy(nclocLangParsed, function (item) {
              return -item.value;
            }),
            nclocLang = _.first(nclocLangSorted, 2),
            functionComplexityMeasure =  _.findWhere(msr, { key: 'function_complexity' }),
            fileComplexityMeasure =  _.findWhere(msr, { key: 'file_complexity' });
        that.set({
          ncloc: nclocMeasure && nclocMeasure.val,
          ncloc1: nclocMeasure && nclocMeasure.var1,
          ncloc2: nclocMeasure && nclocMeasure.var2,
          ncloc3: nclocMeasure && nclocMeasure.var3,
          nclocLang: nclocLang,

          functionComplexity: functionComplexityMeasure && functionComplexityMeasure.val,
          functionComplexity1: functionComplexityMeasure && functionComplexityMeasure.var1,
          functionComplexity2: functionComplexityMeasure && functionComplexityMeasure.var2,
          functionComplexity3: functionComplexityMeasure && functionComplexityMeasure.var3,

          fileComplexity: fileComplexityMeasure && fileComplexityMeasure.val,
          fileComplexity1: fileComplexityMeasure && fileComplexityMeasure.var1,
          fileComplexity2: fileComplexityMeasure && fileComplexityMeasure.var2,
          fileComplexity3: fileComplexityMeasure && fileComplexityMeasure.var3
        });
      });
    },

    fetchSizeTrend: function () {
      var that = this,
          url = baseUrl + '/api/timemachine/index',
          options = {
            resource: this.get('componentKey'),
            metrics: 'ncloc'
          };
      return $.get(url, options).done(function (r) {
        var trend = r[0].cells.map(function (cell) {
          return { val: cell.d, count: cell.v[0] };
        });
        that.set({ sizeTrend: trend });
      });
    },

    fetchIssues: function () {
      var that = this,
          url = baseUrl + '/api/issues/search',
          options = {
            ps: 1,
            resolved: 'false',
            componentUuids: this.get('componentUuid'),
            facets: 'severities,statuses,tags'
          };
      return $.get(url, options).done(function (r) {
        var severityFacet = _.findWhere(r.facets, { property: 'severities' }),
            statusFacet = _.findWhere(r.facets, { property: 'statuses' }),
            tagFacet = _.findWhere(r.facets, { property: 'tags' }),
            tags = _.first(tagFacet.values, 10),
            minTagCount = _.min(tags, function (t) {
              return t.count;
            }).count,
            maxTagCount = _.max(tags, function (t) {
              return t.count;
            }).count,
            tagScale = d3.scale.linear().domain([minTagCount, maxTagCount]).range([10, 24]),
            sizedTags = tags.map(function (tag) {
              return _.extend(tag, { size: tagScale(tag.count) });
            });
        that.set({
          issues: r.total,
          blockerIssues: _.findWhere(severityFacet.values, { val: 'BLOCKER' }).count,
          criticalIssues: _.findWhere(severityFacet.values, { val: 'CRITICAL' }).count,
          majorIssues: _.findWhere(severityFacet.values, { val: 'MAJOR' }).count,
          openIssues: _.findWhere(statusFacet.values, { val: 'OPEN' }).count +
          _.findWhere(statusFacet.values, { val: 'REOPENED' }).count,
          issuesTags: sizedTags
        });
      });
    },

    fetchIssues1: function () {
      if (!this.get('period1Date')) {
        return;
      }
      var that = this,
          url = baseUrl + '/api/issues/search',
          options = {
            ps: 1,
            resolved: 'false',
            createdAfter: this.get('period1Date'),
            componentUuids: this.get('componentUuid'),
            facets: 'severities,statuses'
          };
      return $.get(url, options).done(function (r) {
        var severityFacet = _.findWhere(r.facets, { property: 'severities' }),
            statusFacet = _.findWhere(r.facets, { property: 'statuses' });
        that.set({
          issues1: r.total,
          blockerIssues1: _.findWhere(severityFacet.values, { val: 'BLOCKER' }).count,
          criticalIssues1: _.findWhere(severityFacet.values, { val: 'CRITICAL' }).count,
          majorIssues1: _.findWhere(severityFacet.values, { val: 'MAJOR' }).count,
          openIssues1: _.findWhere(statusFacet.values, { val: 'OPEN' }).count +
          _.findWhere(statusFacet.values, { val: 'REOPENED' }).count
        });
      });
    },

    fetchIssues2: function () {
      if (!this.get('period2Date')) {
        return;
      }
      var that = this,
          url = baseUrl + '/api/issues/search',
          options = {
            ps: 1,
            resolved: 'false',
            createdAfter: this.get('period2Date'),
            componentUuids: this.get('componentUuid'),
            facets: 'severities,statuses'
          };
      return $.get(url, options).done(function (r) {
        var severityFacet = _.findWhere(r.facets, { property: 'severities' }),
            statusFacet = _.findWhere(r.facets, { property: 'statuses' });
        that.set({
          issues2: r.total,
          blockerIssues2: _.findWhere(severityFacet.values, { val: 'BLOCKER' }).count,
          criticalIssues2: _.findWhere(severityFacet.values, { val: 'CRITICAL' }).count,
          majorIssues2: _.findWhere(severityFacet.values, { val: 'MAJOR' }).count,
          openIssues2: _.findWhere(statusFacet.values, { val: 'OPEN' }).count +
          _.findWhere(statusFacet.values, { val: 'REOPENED' }).count
        });
      });
    },

    fetchIssues3: function () {
      if (!this.get('period3Date')) {
        return;
      }
      var that = this,
          url = baseUrl + '/api/issues/search',
          options = {
            ps: 1,
            resolved: 'false',
            createdAfter: this.get('period3Date'),
            componentUuids: this.get('componentUuid'),
            facets: 'severities,statuses'
          };
      return $.get(url, options).done(function (r) {
        var severityFacet = _.findWhere(r.facets, { property: 'severities' }),
            statusFacet = _.findWhere(r.facets, { property: 'statuses' });
        that.set({
          issues3: r.total,
          blockerIssues3: _.findWhere(severityFacet.values, { val: 'BLOCKER' }).count,
          criticalIssues3: _.findWhere(severityFacet.values, { val: 'CRITICAL' }).count,
          majorIssues3: _.findWhere(severityFacet.values, { val: 'MAJOR' }).count,
          openIssues3: _.findWhere(statusFacet.values, { val: 'OPEN' }).count +
          _.findWhere(statusFacet.values, { val: 'REOPENED' }).count
        });
      });
    },

    fetchIssuesTrend: function () {
      var that = this,
          url = baseUrl + '/api/timemachine/index',
          options = {
            resource: this.get('componentKey'),
            metrics: 'violations'
          };
      return $.get(url, options).done(function (r) {
        var trend = r[0].cells.map(function (cell) {
          return { val: cell.d, count: cell.v[0] };
        });
        that.set({ issuesTrend: trend });
      });
    },

    fetchDebt: function () {
      var that = this,
          url = baseUrl + '/api/resources/index',
          options = {
            resource: this.get('componentKey'),
            metrics: 'sqale_index',
            includetrends: true
          };
      return $.get(url, options).done(function (r) {
        var msr = r[0].msr,
            debtMeasure = _.findWhere(msr, { key: 'sqale_index' });
        that.set({
          debt: debtMeasure && debtMeasure.val,
          debt1: debtMeasure && debtMeasure.var1,
          debt2: debtMeasure && debtMeasure.var2,
          debt3: debtMeasure && debtMeasure.var3
        });
      });
    },

    fetchDebtTrend: function () {
      var that = this,
          url = baseUrl + '/api/timemachine/index',
          options = {
            resource: this.get('componentKey'),
            metrics: 'sqale_index'
          };
      return $.get(url, options).done(function (r) {
        var trend = r[0].cells.map(function (cell) {
          return { val: cell.d, count: cell.v[0] };
        });
        that.set({ debtTrend: trend });
      });
    },

    fetchCoverage: function () {
      var that = this,
          url = baseUrl + '/api/resources/index',
          options = {
            resource: this.get('componentKey'),
            metrics: 'overall_coverage,new_overall_coverage',
            includetrends: true
          };
      return $.get(url, options).done(function (r) {
        var msr = r[0].msr,
            coverageMeasure = _.findWhere(msr, { key: 'overall_coverage' }),
            newCoverageMeasure = _.findWhere(msr, { key: 'new_overall_coverage' });
        that.set({
          coverage: coverageMeasure && coverageMeasure.val,
          coverage1: coverageMeasure && coverageMeasure.var1,
          coverage2: coverageMeasure && coverageMeasure.var2,
          coverage3: coverageMeasure && coverageMeasure.var3,
          newCoverage1: newCoverageMeasure && newCoverageMeasure.var1,
          newCoverage2: newCoverageMeasure && newCoverageMeasure.var2,
          newCoverage3: newCoverageMeasure && newCoverageMeasure.var3
        });
      });
    },

    fetchCoverageTrend: function () {
      var that = this,
          url = baseUrl + '/api/timemachine/index',
          options = {
            resource: this.get('componentKey'),
            metrics: 'coverage'
          };
      return $.get(url, options).done(function (r) {
        var trend = r[0].cells.map(function (cell) {
          return { val: cell.d, count: cell.v[0] };
        });
        that.set({ coverageTrend: trend });
      });
    },

    fetchDuplications: function () {
      var that = this,
          url = baseUrl + '/api/resources/index',
          options = {
            resource: this.get('componentKey'),
            metrics: 'duplicated_lines_density',
            includetrends: true
          };
      return $.get(url, options).done(function (r) {
        var msr = r[0].msr,
            duplicationsMeasure = _.findWhere(msr, { key: 'duplicated_lines_density' });
        that.set({
          duplications: duplicationsMeasure && duplicationsMeasure.val,
          duplications1: duplicationsMeasure && duplicationsMeasure.var1,
          duplications2: duplicationsMeasure && duplicationsMeasure.var2,
          duplications3: duplicationsMeasure && duplicationsMeasure.var3
        });
      });
    },

    fetchDuplicationsTrend: function () {
      var that = this,
          url = baseUrl + '/api/timemachine/index',
          options = {
            resource: this.get('componentKey'),
            metrics: 'duplicated_lines_density'
          };
      return $.get(url, options).done(function (r) {
        var trend = r[0].cells.map(function (cell) {
          return { val: cell.d, count: cell.v[0] };
        });
        that.set({ duplicationsTrend: trend });
      });
    }
  });

});
