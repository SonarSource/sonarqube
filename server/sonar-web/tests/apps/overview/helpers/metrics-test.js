import { expect } from 'chai';

import { filterMetrics, filterMetricsForDomains, getShortType, getMetricName } from
    '../../../../src/main/js/apps/overview/helpers/metrics';


const METRICS = [
  { key: 'normal_metric', type: 'INT', hidden: false },
  { key: 'hidden_metric', type: 'INT', hidden: true },
  { key: 'DATA_metric', type: 'DATA', hidden: false },
  { key: 'DISTRIB_metric', type: 'DISTRIB', hidden: false },
  { key: 'new_metric', type: 'FLOAT', hidden: false }
];


describe('Overview Helpers', function () {
  describe('Metrics', function () {

    describe('#filterMetrics', function () {
      it('should filter out hidden metrics', function () {
        let metrics = [
          { key: 'normal_metric', type: 'INT', hidden: false },
          { key: 'hidden_metric', type: 'INT', hidden: true }
        ];
        expect(filterMetrics(metrics)).to.have.length(1);
      });

      it('should filter out DATA and DISTRIB metrics', function () {
        let metrics = [
          { key: 'normal_metric', type: 'INT', hidden: false },
          { key: 'DATA_metric', type: 'DATA', hidden: false },
          { key: 'DISTRIB_metric', type: 'DISTRIB', hidden: false }
        ];
        expect(filterMetrics(metrics)).to.have.length(1);
      });

      it('should filter out differential metrics', function () {
        let metrics = [
          { key: 'normal_metric', type: 'INT', hidden: false },
          { key: 'new_metric', type: 'FLOAT', hidden: false }
        ];
        expect(filterMetrics(metrics)).to.have.length(1);
      });
    });

    describe('#filterMetricsForDomains', function () {
      it('should filter out hidden metrics', function () {
        let metrics = [
          { key: 'normal_metric', type: 'INT', hidden: false, domain: 'first' },
          { key: 'hidden_metric', type: 'INT', hidden: true, domain: 'first' }
        ];
        expect(filterMetricsForDomains(metrics, ['first'])).to.have.length(1);
      });

      it('should filter out DATA and DISTRIB metrics', function () {
        let metrics = [
          { key: 'normal_metric', type: 'INT', hidden: false, domain: 'first' },
          { key: 'DATA_metric', type: 'DATA', hidden: false, domain: 'first' },
          { key: 'DISTRIB_metric', type: 'DISTRIB', hidden: false, domain: 'first' }
        ];
        expect(filterMetricsForDomains(metrics, ['first'])).to.have.length(1);
      });

      it('should filter out differential metrics', function () {
        let metrics = [
          { key: 'normal_metric', type: 'INT', hidden: false, domain: 'first' },
          { key: 'new_metric', type: 'FLOAT', hidden: false, domain: 'first' }
        ];
        expect(filterMetricsForDomains(metrics, ['first'])).to.have.length(1);
      });

      it('should filter metrics by domains', function () {
        let metrics = [
          { key: 'normal_metric', type: 'INT', hidden: false, domain: 'first' },
          { key: 'normal_metric1', type: 'INT', hidden: false, domain: 'second' },
          { key: 'normal_metric2', type: 'INT', hidden: false, domain: 'third' },
          { key: 'normal_metric3', type: 'INT', hidden: false, domain: 'second' }
        ];
        expect(filterMetricsForDomains(metrics, ['first', 'second'])).to.have.length(3);
      });
    });


    describe('#getShortType', function () {
      it('should shorten INT', function () {
        expect(getShortType('INT')).to.equal('SHORT_INT');
      });

      it('should shorten WORK_DUR', function () {
        expect(getShortType('WORK_DUR')).to.equal('SHORT_WORK_DUR');
      });

      it('should not shorten FLOAT', function () {
        expect(getShortType('FLOAT')).to.equal('FLOAT');
      });

      it('should not shorten PERCENT', function () {
        expect(getShortType('PERCENT')).to.equal('PERCENT');
      });
    });


    describe('#getMetricName', function () {
      it('should return metric name', function () {
        expect(getMetricName('metric_name')).to.equal('overview.metric.metric_name');
      });
    });

  });
});
