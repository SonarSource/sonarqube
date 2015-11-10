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
import { IssueMeasure, AddedRemovedMeasure, OnNewCodeMeasure, SeverityMeasure } from './issue-measure';
import { formatMeasure, formatMeasureVariation, localizeMetric } from '../../../helpers/measures';
import IssuesLink from '../helpers/issues-link';
import { Measure } from '../main/components';
import { getMetricName } from '../helpers/metrics';
import Tags from './tags';
import Assignees from './assignees';
import { getFacets, extractAssignees } from '../../../api/issues';
import StatusHelper from '../../../components/shared/status-helper';
import Rating from '../helpers/rating';
import DrilldownLink from '../helpers/drilldown-link';


const KNOWN_METRICS = ['violations', 'sqale_index', 'sqale_rating', 'sqale_debt_ratio', 'blocker_violations',
  'critical_violations', 'major_violations', 'minor_violations', 'info_violations', 'confirmed_issues'];


export const IssuesMain = React.createClass({
  mixins: [TooltipsMixin],

  getInitialState() {
    return {
      ready: false,
      leakPeriodLabel: getPeriodLabel(this.props.component.periods, this.props.leakPeriodIndex),
      leakPeriodDate: getPeriodDate(this.props.component.periods, this.props.leakPeriodIndex)
    };
  },

  componentDidMount() {
    Promise.all([
      this.requestMeasures(),
      this.requestIssues()
    ]).then(responses => {
      let measures = this.getMeasuresValues(responses[0], 'value');
      let leak = this.getMeasuresValues(responses[0], 'var' + this.props.leakPeriodIndex);
      let tags = this.getFacet(responses[1].facets, 'tags');
      let assignees = extractAssignees(this.getFacet(responses[1].facets, 'assignees'), responses[1].response);
      this.setState({ ready: true, measures, leak, tags, assignees });
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
        .filter(metric => ['Issues', 'Technical Debt'].indexOf(metric.domain) !== -1)
        .map(metric => metric.key);
  },

  getMetricsForTimeline() {
    return filterMetricsForDomains(this.props.metrics, ['Issues', 'Technical Debt']);
  },

  getAllMetricsForTimeline() {
    return filterMetrics(this.props.metrics);
  },

  requestMeasures () {
    return getMeasuresAndVariations(this.props.component.key, this.getMetricsForDomain());
  },

  getFacet (facets, facetKey) {
    return _.findWhere(facets, { property: facetKey }).values;
  },

  requestIssues () {
    return getFacets({
      componentUuids: this.props.component.id,
      resolved: 'false'
    }, ['tags', 'assignees']);
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
    let metrics = filterMetricsForDomains(this.props.metrics, ['Issues', 'Technical Debt'])
        .filter(metric => KNOWN_METRICS.indexOf(metric.key) === -1)
        .map(metric => {
          return <DetailedMeasure key={metric.key} {...this.props} {...this.state} metric={metric.key}
                                  type={metric.type}/>;
        });
    return <div>{metrics}</div>;
  },

  render () {
    if (!this.state.ready) {
      return this.renderLoading();
    }

    let treemapScale = d3.scale.ordinal()
        .domain([1, 2, 3, 4, 5])
        .range(CHART_COLORS_RANGE_PERCENT);

    let rating = formatMeasure(this.state.measures['sqale_rating'], 'RATING');

    return <div className="overview-detailed-page">
      <div className="overview-domain-charts">
        <div className="overview-domain overview-domain-fixed-width">
          <div className="overview-domain-header">
            <div className="overview-title">Technical Debt Overview</div>
            {this.renderLegend()}
          </div>

          <div className="overview-detailed-measures-list overview-detailed-measures-list-inline">
            <div className="overview-detailed-measure">
              <div className="overview-detailed-measure-nutshell">
                <span className="overview-detailed-measure-name">SQALE Rating</span>
                <span className="overview-detailed-measure-value">
                  <DrilldownLink component={this.props.component.key} metric="sqale_rating">
                    <Rating value={this.state.measures['sqale_rating']}/>
                  </DrilldownLink>
                </span>
              </div>
            </div>
            <AddedRemovedMeasure {...this.props} {...this.state}
                metric="violations" leakMetric="new_violations" type="INT"/>
            <AddedRemovedMeasure {...this.props} {...this.state}
                metric="sqale_index" leakMetric="new_technical_debt" type="WORK_DUR"/>
            <OnNewCodeMeasure {...this.props} {...this.state}
                metric="sqale_debt_ratio" leakMetric="new_sqale_debt_ratio" type="PERCENT"/>
          </div>

          <div className="overview-detailed-measures-list overview-detailed-measures-list-inline">
            <SeverityMeasure {...this.props} {...this.state} severity="BLOCKER"/>
            <SeverityMeasure {...this.props} {...this.state} severity="CRITICAL"/>
            <SeverityMeasure {...this.props} {...this.state} severity="MAJOR"/>
            <SeverityMeasure {...this.props} {...this.state} severity="MINOR"/>
            <SeverityMeasure {...this.props} {...this.state} severity="INFO"/>
          </div>

          <div className="overview-detailed-measures-list overview-detailed-measures-list-inline">
            <div className="overview-detailed-measure">
              <div className="overview-detailed-measure-nutshell">
                <Tags {...this.props} tags={this.state.tags}/>
              </div>
            </div>
            <div className="overview-detailed-measure">
              <div className="overview-detailed-measure-nutshell">
                <div className="overview-detailed-measure-name">
                  <StatusHelper status="OPEN"/> & <StatusHelper status="REOPENED"/> Issues
                </div>
                <div className="spacer-top">
                  <Assignees {...this.props} assignees={this.state.assignees}/>
                </div>
              </div>
            </div>
          </div>

          <div className="overview-detailed-measures-list">
            {this.renderOtherMeasures()}
          </div>
        </div>
        <DomainBubbleChart {...this.props}
            xMetric="violations"
            yMetric="sqale_index"
            sizeMetrics={['blocker_violations', 'critical_violations']}/>
      </div>

      <div className="overview-domain-charts">
        <DomainTimeline {...this.props} {...this.state}
            initialMetric="sqale_index"
            metrics={this.getMetricsForTimeline()}
            allMetrics={this.getAllMetricsForTimeline()}/>
        <DomainTreemap {...this.props}
            sizeMetric="ncloc"
            colorMetric="sqale_rating"
            scale={treemapScale}/>
      </div>
    </div>;

  }
});
