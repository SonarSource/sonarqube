import _ from 'underscore';
import React from 'react';

import { DetailedMeasure } from '../components/detailed-measure';
import { filterMetricsForDomains } from '../helpers/metrics';
import { CoverageMeasure } from '../components/coverage-measure';


const TEST_DOMAINS = ['Tests', 'Tests (Integration)', 'Tests (Overall)'];
const TEST_METRICS = ['tests', 'test_execution_time', 'test_errors', 'test_failures', 'skipped_tests',
  'test_success_density'];


export const CoverageMeasuresList = React.createClass({
  renderOtherMeasures() {
    let knownMetrics = [].concat(TEST_METRICS, [
      this.props.coverageMetricPrefix + 'coverage',
      this.props.coverageMetricPrefix + 'line_coverage',
      this.props.coverageMetricPrefix + 'branch_coverage'
    ]);
    let metrics = filterMetricsForDomains(this.props.metrics, TEST_DOMAINS)
        .filter(metric => knownMetrics.indexOf(metric.key) === -1)
        .map(metric => metric.key);
    return this.renderListOfMeasures(metrics);
  },

  renderCoverage () {
    let metrics = [
      this.props.coverageMetricPrefix + 'coverage',
      this.props.coverageMetricPrefix + 'line_coverage',
      this.props.coverageMetricPrefix + 'branch_coverage'
    ];

    if (_.every(metrics, metric => this.props.measures[metric] == null)) {
      // if no measures exist
      return null;
    }

    let measures = metrics.map(metric => {
      return <CoverageMeasure key={metric}
                              metric={metric}
                              measure={this.props.measures[metric]}
                              leak={this.props.leak[metric]}
                              component={this.props.component}/>;
    });
    return <div className="overview-detailed-measures-list overview-detailed-measures-list-inline">
      {measures}
    </div>;
  },

  renderNewCoverage () {
    let metrics = [
      'new_' + this.props.coverageMetricPrefix + 'coverage',
      'new_' + this.props.coverageMetricPrefix + 'line_coverage',
      'new_' + this.props.coverageMetricPrefix + 'branch_coverage'
    ];

    if (_.every(metrics, metric => this.props.leak[metric] == null)) {
      // if no measures exist
      return null;
    }

    let measures = metrics.map(metric => {
      return <CoverageMeasure key={metric}
                              metric={metric}
                              measure={this.props.leak[metric]}
                              component={this.props.component}
                              period={this.props.leakPeriodIndex}/>;
    });
    return <div className="overview-detailed-measures-list overview-detailed-measures-list-inline">
      {measures}
    </div>;
  },

  renderListOfMeasures(list) {
    let metrics = list.map(key => _.findWhere(this.props.metrics, { key }));

    if (_.every(metrics, metric => this.props.measures[metric.key] == null)) {
      // if no measures exist
      return null;
    }

    metrics = metrics.map(metric => {
      return <DetailedMeasure key={metric.key} {...this.props} {...this.props} metric={metric.key}
                              type={metric.type}/>;
    });
    return <div className="overview-detailed-measures-list">{metrics}</div>;
  },

  render () {
    return <div>
      {this.renderCoverage()}
      {this.renderNewCoverage()}

      {this.renderListOfMeasures(TEST_METRICS)}

      {this.renderOtherMeasures()}
    </div>;
  }
});
