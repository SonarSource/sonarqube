import _ from 'underscore';
import React from 'react';

import { DetailedMeasure } from './detailed-measure';
import { CoverageMeasures } from './coverage-measures';
import { filterMetricsForDomains } from '../helpers/metrics';


const TEST_DOMAINS = ['Tests', 'Tests (Integration)', 'Tests (Overall)'];
const TEST_METRICS = ['tests', 'test_execution_time', 'test_errors', 'test_failures', 'skipped_tests',
  'test_success_density'];


export const CoverageMeasuresList = React.createClass({
  shouldRenderOverallCoverage() {
    return this.props.measures['overall_coverage'] != null &&
        this.shouldRenderUTCoverage() &&
        this.shouldRenderITCoverage();
  },

  shouldRenderUTCoverage() {
    return this.props.measures['coverage'] != null;
  },

  shouldRenderITCoverage() {
    return this.props.measures['it_coverage'] != null;
  },

  renderOtherMeasures() {
    let knownMetrics = [].concat(TEST_METRICS, [
      'coverage', 'line_coverage', 'branch_coverage',
      'uncovered_lines', 'uncovered_conditions',

      'it_coverage', 'it_line_coverage', 'it_branch_coverage',
      'it_uncovered_lines', 'it_uncovered_conditions',

      'overall_coverage', 'overall_line_coverage', 'overall_branch_coverage',
      'overall_uncovered_lines', 'overall_uncovered_conditions',

      'lines_to_cover', 'conditions_to_cover'
    ]);
    let metrics = filterMetricsForDomains(this.props.metrics, TEST_DOMAINS)
        .filter(metric => knownMetrics.indexOf(metric.key) === -1)
        .map(metric => metric.key);
    return this.renderListOfMeasures(metrics);
  },

  renderListOfMeasures(list) {
    let metrics = list.map(key => _.findWhere(this.props.metrics, { key }));

    if (_.every(metrics, metric => this.props.measures[metric.key] == null)) {
      // if no measures exist
      return null;
    }

    metrics = metrics.map(metric => {
      return <DetailedMeasure key={metric.key}
                              {...this.props}
                              {...this.props}
                              metric={metric.key}
                              type={metric.type}/>;
    });
    return <div className="overview-detailed-measures-list">{metrics}</div>;
  },

  render () {
    return <div>
      {this.shouldRenderOverallCoverage() && <CoverageMeasures {...this.props} prefix="overall_"/>}
      {this.shouldRenderUTCoverage() && <CoverageMeasures {...this.props} prefix=""/>}
      {this.shouldRenderITCoverage() && <CoverageMeasures {...this.props} prefix="it_"/>}
      {this.renderListOfMeasures(TEST_METRICS)}
      {this.renderOtherMeasures()}
    </div>;
  }
});
