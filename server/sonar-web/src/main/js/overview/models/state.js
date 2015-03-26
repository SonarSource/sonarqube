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
          this.fetchSizeTreemap(),
          this.fetchSizeTrend(),

          this.fetchIssues(),
          this.fetchIssues1(),
          this.fetchIssues2(),
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
        var gateData = JSON.parse(r[0].msr[0].data),
            gateConditions = gateData.conditions,
            urlMetrics = baseUrl + '/api/metrics';
        $.get(urlMetrics).done(function (r) {
          that.set({
            gateStatus: gateData.level,
            gateConditions: gateConditions.map(function (c) {
              var metric = _.findWhere(r, { key: c.metric }),
                  type = metric != null ? metric.val_type : null,
                  periodName = that.get('period' + c.period + 'Name');
              return _.extend(c, { type: type, periodName: periodName });
            })
          });
        });
      });
    },

    fetchSize: function () {
      var that = this,
          url = baseUrl + '/api/resources/index',
          options = {
            resource: this.get('componentKey'),
            metrics: 'ncloc,ncloc_language_distribution',
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
            nclocLang = _.first(nclocLangSorted, 2);
        that.set({
          ncloc: nclocMeasure.val,
          ncloc1: nclocMeasure.var3,
          ncloc2: nclocMeasure.var1,
          nclocLang: nclocLang
        });
      });
    },

    fetchSizeTreemap: function () {
      var that = this,
          url = baseUrl + '/api/resources/index',
          options = {
            resource: this.get('componentKey'),
            depth: 1,
            metrics: 'ncloc,sqale_debt_ratio'
          };
      return $.get(url, options).done(function (r) {
        var components = r.map(function (component) {
          var measures = component.msr.map(function (measure) {
                return {
                  key: measure.key,
                  val: measure.val,
                  fval: measure.frmt_val
                };
              }),
              indexedMeasures = _.indexBy(measures, 'key');
          return {
            key: component.key,
            name: component.name,
            longName: component.lname,
            qualifier: component.qualifier,
            measures: indexedMeasures
          };
        });

        that.set({
          treemapComponents: components,
          treemapMetrics: {
            'sqale_debt_ratio': {
              name: t('metric.sqale_debt_ratio.name'),
              direction: '1',
              type: 'PERCENT'
            },
            ncloc: {
              name: t('metric.ncloc.name')
            }
          },
          treemapMetricsPriority: ['sqale_debt_ratio', 'ncloc']
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
            facets: 'severities,tags'
          };
      return $.get(url, options).done(function (r) {
        var severityFacet = _.findWhere(r.facets, { property: 'severities' }),
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
          issuesTags: sizedTags
        });
      });
    },

    fetchIssues1: function () {
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
          issues1: r.total,
          blockerIssues1: _.findWhere(severityFacet.values, { val: 'BLOCKER' }).count,
          criticalIssues1: _.findWhere(severityFacet.values, { val: 'CRITICAL' }).count,
          openIssues1: _.findWhere(statusFacet.values, { val: 'OPEN' }).count +
          _.findWhere(statusFacet.values, { val: 'REOPENED' }).count
        });
      });
    },

    fetchIssues2: function () {
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
          issues2: r.total,
          blockerIssues2: _.findWhere(severityFacet.values, { val: 'BLOCKER' }).count,
          criticalIssues2: _.findWhere(severityFacet.values, { val: 'CRITICAL' }).count,
          openIssues2: _.findWhere(statusFacet.values, { val: 'OPEN' }).count +
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
            metrics: 'sqale_index,blocker_remediation_cost,critical_remediation_cost',
            includetrends: true
          };
      return $.get(url, options).done(function (r) {
        var msr = r[0].msr,
            debtMeasure = _.findWhere(msr, { key: 'sqale_index' }),
            blockerDebtMeasure = _.findWhere(msr, { key: 'blocker_remediation_cost' }),
            criticalDebtMeasure = _.findWhere(msr, { key: 'critical_remediation_cost' });
        that.set({
          debt: debtMeasure.val,
          debt1: debtMeasure.var3,
          debt2: debtMeasure.var1,
          blockerDebt: blockerDebtMeasure != null ? blockerDebtMeasure.val : null,
          blockerDebt1: blockerDebtMeasure != null ? blockerDebtMeasure.var3 : null,
          blockerDebt2: blockerDebtMeasure != null ? blockerDebtMeasure.var1 : null,
          criticalDebt: criticalDebtMeasure != null ? criticalDebtMeasure.val : null,
          criticalDebt1: criticalDebtMeasure != null ? criticalDebtMeasure.var3 : null,
          criticalDebt2: criticalDebtMeasure != null ? criticalDebtMeasure.var1 : null
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
          coverage: coverageMeasure.val,
          coverage1: coverageMeasure.var3,
          coverage2: coverageMeasure.var1,
          newCoverage1: newCoverageMeasure.var3,
          newCoverage2: newCoverageMeasure.var1
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
          duplications: duplicationsMeasure.val,
          duplications1: duplicationsMeasure.var3,
          duplications2: duplicationsMeasure.var1
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
