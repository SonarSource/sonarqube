import _ from 'underscore';
import d3 from 'd3';
import React from 'react';

import { getMeasuresAndVariations } from '../../../api/measures';
import { DetailedMeasure } from '../common-components';
import { DomainTimeline } from '../timeline/domain-timeline';
import { DomainTreemap } from '../domain/treemap';
import { DomainBubbleChart } from '../domain/bubble-chart';
import { getPeriodLabel, getPeriodDate } from './../helpers/period-label';
import { TooltipsMixin } from '../../../components/mixins/tooltips-mixin';
import { filterMetrics, filterMetricsForDomains } from '../helpers/metrics';
import { Legend } from '../common-components';
import { CHART_COLORS_RANGE_PERCENT } from '../../../helpers/constants';
import { formatMeasure, formatMeasureVariation, localizeMetric } from '../../../helpers/measures';
import { DonutChart } from '../../../components/charts/donut-chart';
import DrilldownLink from '../helpers/drilldown-link';
import { CoverageMeasure } from './coverage-measure';


const UT_COVERAGE_METRICS = ['coverage', 'new_coverage', 'branch_coverage', 'line_coverage', 'uncovered_conditions',
  'uncovered_lines'];
const IT_COVERAGE_METRICS = ['it_coverage', 'new_it_coverage', 'it_branch_coverage', 'it_line_coverage',
  'it_uncovered_conditions', 'it_uncovered_lines'];
const OVERALL_COVERAGE_METRICS = ['overall_coverage', 'new_overall_coverage', 'overall_branch_coverage',
  'overall_line_coverage', 'overall_uncovered_conditions', 'overall_uncovered_lines'];
const TEST_METRICS = ['tests', 'test_execution_time', 'test_errors', 'test_failures', 'skipped_tests',
  'test_success_density'];
const KNOWN_METRICS = [].concat(TEST_METRICS, OVERALL_COVERAGE_METRICS, UT_COVERAGE_METRICS, IT_COVERAGE_METRICS);


export const CoverageMain = React.createClass({
  mixins: [TooltipsMixin],

  getInitialState() {
    return {
      ready: false,
      leakPeriodLabel: getPeriodLabel(this.props.component.periods, this.props.leakPeriodIndex),
      leakPeriodDate: getPeriodDate(this.props.component.periods, this.props.leakPeriodIndex)
    };
  },

  componentDidMount() {
    this.requestMeasures().then(r => {
      let measures = this.getMeasuresValues(r, 'value');
      let leak = this.getMeasuresValues(r, 'var' + this.props.leakPeriodIndex);
      this.setState({ ready: true, measures, leak });
    });
  },

  getMeasuresValues (measures, fieldKey) {
    let values = {};
    Object.keys(measures).forEach(measureKey => {
      values[measureKey] = measures[measureKey][fieldKey];
    });
    return values;
  },

  getMetricsForDomain() {
    return this.props.metrics
        .filter(metric => ['Tests', 'Tests (Integration)', 'Tests (Overall)'].indexOf(metric.domain) !== -1)
        .map(metric => metric.key);
  },

  getMetricsForTimeline() {
    return filterMetricsForDomains(this.props.metrics, ['Tests', 'Tests (Integration)', 'Tests (Overall)']);
  },

  getAllMetricsForTimeline() {
    return filterMetrics(this.props.metrics);
  },

  requestMeasures () {
    return getMeasuresAndVariations(this.props.component.key, this.getMetricsForDomain());
  },

  renderLoading () {
    return <div className="text-center">
      <i className="spinner spinner-margin"/>
    </div>;
  },

  renderLegend () {
    return <Legend leakPeriodDate={this.state.leakPeriodDate} leakPeriodLabel={this.state.leakPeriodLabel}/>;
  },

  renderOtherMeasures() {
    let metrics = filterMetricsForDomains(this.props.metrics, ['Tests', 'Tests (Integration)', 'Tests (Overall)'])
        .filter(metric => KNOWN_METRICS.indexOf(metric.key) === -1)
        .map(metric => metric.key);
    return this.renderListOfMeasures(metrics);
  },

  renderUTCoverage () {
    let hasBothTypes = this.state.measures['coverage'] != null && this.state.measures['it_coverage'] != null;
    if (!hasBothTypes) {
      return null;
    }
    return <div className="overview-detailed-measures-list">
      <CoverageMeasure {...this.props} {...this.state} metric="coverage" leakMetric="new_coverage" type="PERCENT"/>
      <CoverageMeasure {...this.props} {...this.state} metric="line_coverage" leakMetric="new_line_coverage" type="PERCENT"/>
      <CoverageMeasure {...this.props} {...this.state} metric="branch_coverage" leakMetric="new_branch_coverage" type="PERCENT"/>

      <CoverageMeasure {...this.props} {...this.state} metric="uncovered_lines" type="INT"/>
      <CoverageMeasure {...this.props} {...this.state} metric="uncovered_conditions" type="INT"/>
    </div>;
  },

  renderITCoverage () {
    let hasBothTypes = this.state.measures['coverage'] != null && this.state.measures['it_coverage'] != null;
    if (!hasBothTypes) {
      return null;
    }
    return <div className="overview-detailed-measures-list">
      <CoverageMeasure {...this.props} {...this.state} metric="it_coverage" leakMetric="new_it_coverage" type="PERCENT"/>
      <CoverageMeasure {...this.props} {...this.state} metric="it_line_coverage" leakMetric="new_it_line_coverage" type="PERCENT"/>
      <CoverageMeasure {...this.props} {...this.state} metric="it_branch_coverage" leakMetric="new_it_branch_coverage" type="PERCENT"/>

      <CoverageMeasure {...this.props} {...this.state} metric="it_uncovered_lines" type="INT"/>
      <CoverageMeasure {...this.props} {...this.state} metric="it_uncovered_conditions" type="INT"/>
    </div>;
  },

  renderOverallCoverage () {
    return <div className="overview-detailed-measures-list">
      <CoverageMeasure {...this.props} {...this.state} metric="overall_coverage" leakMetric="new_overall_coverage" type="PERCENT"/>
      <CoverageMeasure {...this.props} {...this.state} metric="overall_line_coverage" leakMetric="new_overall_line_coverage" type="PERCENT"/>
      <CoverageMeasure {...this.props} {...this.state} metric="overall_branch_coverage" leakMetric="new_overall_branch_coverage" type="PERCENT"/>

      <CoverageMeasure {...this.props} {...this.state} metric="overall_uncovered_lines" type="INT"/>
      <CoverageMeasure {...this.props} {...this.state} metric="overall_uncovered_conditions" type="INT"/>
    </div>;
  },

  renderListOfMeasures(list) {
    let metrics = list
        .map(key => _.findWhere(this.props.metrics, { key }))
        .map(metric => {
          return <DetailedMeasure key={metric.key} {...this.props} {...this.state} metric={metric.key}
                                  type={metric.type}/>;
        });
    return <div className="overview-detailed-measures-list">{metrics}</div>;
  },

  render () {
    if (!this.state.ready) {
      return this.renderLoading();
    }
    let treemapScale = d3.scale.linear()
        .domain([0, 100])
        .range(CHART_COLORS_RANGE_PERCENT);
    return <div className="overview-detailed-page">
      <div className="overview-domain-charts">
        <div className="overview-domain">
          <div className="overview-domain-header">
            <div className="overview-title">Tests Overview</div>
            {this.renderLegend()}
          </div>
          {this.renderOverallCoverage()}
          {this.renderUTCoverage()}
          {this.renderITCoverage()}
          {this.renderListOfMeasures(TEST_METRICS)}
          {this.renderOtherMeasures()}
        </div>
        <DomainBubbleChart {...this.props}
            xMetric="complexity"
            yMetric="overall_coverage"
            sizeMetrics={['overall_uncovered_lines']}/>
      </div>

      <div className="overview-domain-charts">
        <DomainTimeline {...this.props} {...this.state}
            initialMetric="overall_coverage"
            metrics={this.getMetricsForTimeline()}
            allMetrics={this.getAllMetricsForTimeline()}/>
        <DomainTreemap {...this.props}
            sizeMetric="ncloc"
            colorMetric="overall_coverage"
            scale={treemapScale}/>
      </div>
    </div>;

  }
});
