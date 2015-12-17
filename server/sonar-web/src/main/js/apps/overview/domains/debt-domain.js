import _ from 'underscore';
import d3 from 'd3';
import React from 'react';

import { getMeasuresAndVariations } from '../../../api/measures';
import { DetailedMeasure } from '../components/detailed-measure';
import { DomainTimeline } from '../components/domain-timeline';
import { DomainTreemap } from '../components/domain-treemap';
import { DomainBubbleChart } from '../components/domain-bubble-chart';
import { getPeriodLabel, getPeriodDate } from './../helpers/periods';
import { TooltipsMixin } from '../../../components/mixins/tooltips-mixin';
import { filterMetrics, filterMetricsForDomains } from '../helpers/metrics';
import { CHART_COLORS_RANGE_PERCENT } from '../../../helpers/constants';
import { AddedRemovedMeasure,
         AddedRemovedDebt,
         OnNewCodeMeasure,
         SeverityMeasure } from './../components/issue-measure';
import { IssuesTags } from './../components/issues-tags';
import Assignees from './../components/issues-assignees';
import { getFacet, extractAssignees } from '../../../api/issues';
import { Rating } from '../../../components/shared/rating';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import { DomainLeakTitle } from '../main/components';


const KNOWN_METRICS = ['violations', 'sqale_index', 'sqale_rating', 'sqale_debt_ratio', 'blocker_violations',
  'critical_violations', 'major_violations', 'minor_violations', 'info_violations', 'confirmed_issues'];


export const DebtMain = React.createClass({
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
      this.requestIssues(),
      this.requestAssignees()
    ]).then(responses => {
      let measures = this.getMeasuresValues(responses[0], 'value');
      let leak = this.getMeasuresValues(responses[0], 'var' + this.props.leakPeriodIndex);
      let tags = responses[1].facet;
      let assignees = extractAssignees(responses[2].facet, responses[2].response);
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
    return getFacet({
      componentUuids: this.props.component.id,
      resolved: 'false'
    }, 'tags');
  },

  requestAssignees () {
    return getFacet({
      componentUuids: this.props.component.id,
      resolved: 'false',
      statuses: 'OPEN,REOPENED'
    }, 'assignees');
  },

  renderLoading () {
    return <div className="flex-1 text-center">
      <i className="spinner spinner-margin"/>
    </div>;
  },

  renderLegend () {
    return <DomainLeakTitle inline={true} label={this.state.leakPeriodLabel} date={this.state.leakPeriodDate}/>;
  },

  renderOtherMeasures() {
    let metrics = filterMetricsForDomains(this.props.metrics, ['Issues', 'Technical Debt'])
        .filter(metric => KNOWN_METRICS.indexOf(metric.key) === -1)
        .filter(metric => this.state.measures[metric.key] != null)
        .map(metric => {
          return <DetailedMeasure key={metric.key}
                                  {...this.props}
                                  {...this.state}
                                  metric={metric.key}
                                  type={metric.type}/>;
        });
    if (!metrics.length) {
      return null;
    }
    return <div className="overview-detailed-measures-list">{metrics}</div>;
  },

  renderRating () {
    if (this.state.measures['sqale_rating'] == null) {
      return null;
    }
    return <div className="overview-detailed-measure overview-detailed-measure-rating">
      <div className="overview-detailed-measure-nutshell">
        <span className="overview-detailed-measure-value">
          <DrilldownLink component={this.props.component.key} metric="sqale_rating">
            <Rating value={this.state.measures['sqale_rating']}/>
          </DrilldownLink>
        </span>
      </div>
    </div>;
  },

  renderTags () {
    if (!this.state.tags.length) {
      return null;
    }
    return <div className="overview-detailed-measure">
      <div className="overview-detailed-measure-nutshell">
        <IssuesTags {...this.props} tags={this.state.tags}/>
      </div>
    </div>;
  },

  render () {
    if (!this.state.ready) {
      return this.renderLoading();
    }

    let treemapScale = d3.scale.ordinal()
        .domain([1, 2, 3, 4, 5])
        .range(CHART_COLORS_RANGE_PERCENT);

    return <div className="overview-detailed-page">
      <div className="overview-cards-list">
        <div className="overview-card overview-card-fixed-width">
          <div className="overview-card-header">
            <div className="overview-title">{window.t('overview.domain.debt')}</div>
            {this.renderLegend()}
          </div>

          <div className="overview-detailed-measures-list overview-detailed-measures-list-inline">
            {this.renderRating()}
            <AddedRemovedDebt {...this.props} {...this.state}
                metric="sqale_index" leakMetric="new_technical_debt" type="WORK_DUR"/>
            <OnNewCodeMeasure {...this.props} {...this.state}
                metric="sqale_debt_ratio" leakMetric="new_sqale_debt_ratio" type="PERCENT"/>
            <AddedRemovedMeasure {...this.props} {...this.state}
                metric="violations" leakMetric="new_violations" type="INT"/>
          </div>

          <div className="overview-detailed-measures-list overview-detailed-measures-list-inline">
            <SeverityMeasure {...this.props} {...this.state} severity="BLOCKER"/>
            <SeverityMeasure {...this.props} {...this.state} severity="CRITICAL"/>
            <SeverityMeasure {...this.props} {...this.state} severity="MAJOR"/>
            <SeverityMeasure {...this.props} {...this.state} severity="MINOR"/>
            <SeverityMeasure {...this.props} {...this.state} severity="INFO"/>
          </div>

          <div className="overview-detailed-measures-list overview-detailed-measures-list-inline">
            {this.renderTags()}
            <div className="overview-detailed-measure">
              <div className="overview-detailed-measure-nutshell">
                <div className="overview-detailed-measure-name">
                  {window.t('overview.unmanaged_issues')}
                </div>
                <div className="spacer-top">
                  <Assignees {...this.props} assignees={this.state.assignees}/>
                </div>
              </div>
            </div>
          </div>

          {this.renderOtherMeasures()}
        </div>

        <div className="overview-card">
          <DomainBubbleChart {...this.props}
              xMetric="ncloc"
              yMetric="violations"
              sizeMetrics={['sqale_index']}/>
        </div>
      </div>

      <div className="overview-cards-list">
        <div className="overview-card">
          <DomainTimeline {...this.props} {...this.state}
              initialMetric="sqale_index"
              metrics={this.getMetricsForTimeline()}
              allMetrics={this.getAllMetricsForTimeline()}/>
        </div>
        <div className="overview-card">
          <DomainTreemap {...this.props}
              sizeMetric="ncloc"
              colorMetric="sqale_rating"
              scale={treemapScale}/>
        </div>
      </div>
    </div>;

  }
});
