import _ from 'underscore';
import React from 'react';

import { DetailedMeasure } from '../components/detailed-measure';
import { filterMetricsForDomains } from '../helpers/metrics';
import { CoverageMeasure } from '../components/coverage-measure';


const TEST_DOMAINS = ['Tests', 'Tests (Integration)', 'Tests (Overall)'];

const UT_COVERAGE_METRICS = ['coverage', 'line_coverage', 'branch_coverage'];
const UT_NEW_COVERAGE_METRICS = ['new_coverage', 'new_line_coverage', 'new_branch_coverage'];

const IT_COVERAGE_METRICS = ['it_coverage', 'it_line_coverage', 'it_branch_coverage'];
const IT_NEW_COVERAGE_METRICS = ['new_it_coverage', 'new_it_line_coverage', 'new_it_branch_coverage'];

const OVERALL_COVERAGE_METRICS = ['overall_coverage', 'overall_line_coverage', 'overall_branch_coverage'];
const OVERALL_NEW_COVERAGE_METRICS = ['new_overall_coverage', 'new_overall_line_coverage',
  'new_overall_branch_coverage'];

const TEST_METRICS = ['tests', 'test_execution_time', 'test_errors', 'test_failures', 'skipped_tests',
  'test_success_density'];

const KNOWN_METRICS = [].concat(TEST_METRICS, OVERALL_COVERAGE_METRICS, UT_COVERAGE_METRICS, IT_COVERAGE_METRICS);


export const CoverageMeasuresList = React.createClass({
  renderOtherMeasures() {
    let metrics = filterMetricsForDomains(this.props.metrics, TEST_DOMAINS)
        .filter(metric => KNOWN_METRICS.indexOf(metric.key) === -1)
        .map(metric => metric.key);
    return this.renderListOfMeasures(metrics);
  },

  renderCoverage (metrics) {
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

  shouldRenderTypedCoverage () {
    return this.props.measures['coverage'] != null && this.props.measures['it_coverage'] != null;
  },

  renderTypedCoverage (metrics) {
    return this.shouldRenderTypedCoverage() ? this.renderCoverage(metrics) : null;
  },

  renderNewCoverage (metrics) {
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

  renderTypedNewCoverage (metrics) {
    return this.shouldRenderTypedCoverage() ? this.renderNewCoverage(metrics) : null;
  },

  renderUTCoverage () {
    return this.renderTypedCoverage(UT_COVERAGE_METRICS);
  },

  renderUTNewCoverage () {
    return this.renderTypedNewCoverage(UT_NEW_COVERAGE_METRICS);
  },

  renderITCoverage () {
    return this.renderTypedCoverage(IT_COVERAGE_METRICS);
  },

  renderITNewCoverage () {
    return this.renderTypedNewCoverage(IT_NEW_COVERAGE_METRICS);
  },

  renderOverallCoverage () {
    return this.renderCoverage(OVERALL_COVERAGE_METRICS);
  },

  renderOverallNewCoverage () {
    return this.renderNewCoverage(OVERALL_NEW_COVERAGE_METRICS);
  },

  renderListOfMeasures(list) {
    let metrics = list
        .map(key => _.findWhere(this.props.metrics, { key }))
        .map(metric => {
          return <DetailedMeasure key={metric.key} {...this.props} {...this.props} metric={metric.key}
                                  type={metric.type}/>;
        });
    return <div className="overview-detailed-measures-list">{metrics}</div>;
  },

  render () {
    return <div>
      {this.renderOverallCoverage()}
      {this.renderOverallNewCoverage()}

      {this.renderUTCoverage()}
      {this.renderUTNewCoverage()}

      {this.renderITCoverage()}
      {this.renderITNewCoverage()}

      {this.renderListOfMeasures(TEST_METRICS)}

      {this.renderOtherMeasures()}
    </div>;
  }
});
