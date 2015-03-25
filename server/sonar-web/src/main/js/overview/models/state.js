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
          this.fetchSizeTrend()
      );
    },

    fetchGate: function () {
      var that = this,
          url = baseUrl + '/api/resources/index',
          options = {
            resource: this.get('resource'),
            metrics: 'quality_gate_details'
          };
      return $.get(url, options).done(function (r) {
        var gateData = JSON.parse(r[0].msr[0].data);
        that.set({
          gateStatus: gateData.level,
          gateConditions: gateData.conditions
        });
      });
    },

    fetchSize: function () {
      var that = this,
          url = baseUrl + '/api/resources/index',
          options = {
            resource: this.get('resource'),
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
          ncloc: nclocMeasure.frmt_val,
          ncloc1: nclocMeasure.fvar3,
          ncloc2: nclocMeasure.fvar1,
          nclocLang: nclocLang
        });
      });
    },

    fetchSizeTreemap: function () {
      var that = this,
          url = baseUrl + '/api/resources/index',
          options = {
            resource: this.get('resource'),
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
            resource: this.get('resource'),
            metrics: 'ncloc'
          };
      return $.get(url, options).done(function (r) {
        var trend = r[0].cells.map(function (cell) {
          return { val: cell.d, count: cell.v[0] };
        });
        that.set({ sizeTrend: trend });
      });
    }
  });

});
