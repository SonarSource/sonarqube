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
import { Legend } from '../components/legend';
import { CHART_COLORS_RANGE_PERCENT } from '../../../helpers/constants';


export const DuplicationsMain = React.createClass({
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
        .filter(metric => ['Duplication'].indexOf(metric.domain) !== -1)
        .map(metric => metric.key);
  },

  getMetricsForTimeline() {
    return filterMetricsForDomains(this.props.metrics, ['Duplication']);
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

  renderMeasures() {
    let metrics = filterMetricsForDomains(this.props.metrics, ['Duplication'])
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
    let treemapScale = d3.scale.linear()
        .domain([0, 100])
        .range(CHART_COLORS_RANGE_PERCENT);
    return <div className="overview-detailed-page">
      <div className="overview-cards-list">
        <div className="overview-card overview-card-fixed-width">
          <div className="overview-card-header">
            <div className="overview-title">Duplications Overview</div>
            {this.renderLegend()}
          </div>
          <div className="overview-detailed-measures-list">
            {this.renderMeasures()}
          </div>
        </div>
        <div className="overview-card">
          <DomainBubbleChart {...this.props}
              xMetric="ncloc"
              yMetric="duplicated_lines"
              sizeMetrics={['duplicated_blocks']}/>
        </div>
      </div>

      <div className="overview-cards-list">
        <div className="overview-card">
          <DomainTimeline {...this.props} {...this.state}
              initialMetric="duplicated_lines_density"
              metrics={this.getMetricsForTimeline()}
              allMetrics={this.getAllMetricsForTimeline()}/>
        </div>
        <div className="overview-card">
          <DomainTreemap {...this.props}
              sizeMetric="ncloc"
              colorMetric="duplicated_lines_density"
              scale={treemapScale}/>
        </div>
      </div>
    </div>;

  }
});
