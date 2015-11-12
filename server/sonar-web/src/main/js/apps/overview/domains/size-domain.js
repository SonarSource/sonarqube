import React from 'react';

import { LanguageDistribution } from './../components/language-distribution';
import { ComplexityDistribution } from './../components/complexity-distribution';
import { getMeasuresAndVariations } from '../../../api/measures';
import { DetailedMeasure } from '../components/detailed-measure';
import { DomainTimeline } from '../components/domain-timeline';
import { DomainTreemap } from '../components/domain-treemap';
import { getPeriodLabel, getPeriodDate } from './../helpers/periods';
import { TooltipsMixin } from '../../../components/mixins/tooltips-mixin';
import { filterMetrics, filterMetricsForDomains } from '../helpers/metrics';
import { Legend } from '../components/legend';


export const SizeMain = React.createClass({
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
        .filter(metric => ['Size', 'Complexity', 'Documentation'].indexOf(metric.domain) !== -1)
        .map(metric => metric.key);
  },

  getMetricsForTimeline() {
    return filterMetricsForDomains(this.props.metrics, ['Size', 'Complexity', 'Documentation']);
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

  renderOtherMeasures(domain, hiddenMetrics) {
    let metrics = filterMetricsForDomains(this.props.metrics, [domain])
        .filter(metric => hiddenMetrics.indexOf(metric.key) === -1)
        .map(metric => {
          return <DetailedMeasure key={metric.key} {...this.props} {...this.state} metric={metric.key}
                                  type={metric.type}/>;
        });
    return <div>{metrics}</div>;
  },

  renderOtherSizeMeasures() {
    return this.renderOtherMeasures('Size', ['ncloc']);
  },

  renderOtherComplexityMeasures() {
    return this.renderOtherMeasures('Complexity',
        ['complexity', 'function_complexity', 'file_complexity', 'class_complexity']);
  },

  renderOtherDocumentationMeasures() {
    return this.renderOtherMeasures('Documentation', []);
  },

  render () {
    if (!this.state.ready) {
      return this.renderLoading();
    }
    return <div className="overview-detailed-page">
      <div className="overview-card">
        <div className="overview-card-header">
          <div className="overview-title">Size Overview</div>
          {this.renderLegend()}
        </div>

        <div className="overview-detailed-layout-size">
          <div className="overview-detailed-layout-column">
            <div className="overview-detailed-measures-list">
              <DetailedMeasure {...this.props} {...this.state} metric="ncloc" type="INT">
                <LanguageDistribution lines={this.state.measures['ncloc']}
                                      distribution={this.state.measures['ncloc_language_distribution']}/>
              </DetailedMeasure>
              {this.renderOtherSizeMeasures()}
            </div>
          </div>

          <div className="overview-detailed-layout-column">
            <div className="overview-detailed-measures-list">
              <DetailedMeasure {...this.props} {...this.state} metric="complexity" type="INT"/>
              <DetailedMeasure {...this.props} {...this.state} metric="function_complexity" type="FLOAT">
                <ComplexityDistribution distribution={this.state.measures['function_complexity_distribution']}/>
              </DetailedMeasure>
              <DetailedMeasure {...this.props} {...this.state} metric="file_complexity" type="FLOAT">
                <ComplexityDistribution distribution={this.state.measures['file_complexity_distribution']}/>
              </DetailedMeasure>
              <DetailedMeasure {...this.props} {...this.state} metric="class_complexity" type="FLOAT"/>
              {this.renderOtherComplexityMeasures()}
            </div>
          </div>

          <div className="overview-detailed-layout-column">
            <div className="overview-detailed-measures-list">
              {this.renderOtherDocumentationMeasures()}
            </div>
          </div>
        </div>
      </div>

      <div className="overview-cards-list">
        <div className="overview-card">
          <DomainTimeline {...this.props} {...this.state}
              initialMetric="ncloc"
              metrics={this.getMetricsForTimeline()}
              allMetrics={this.getAllMetricsForTimeline()}/>
        </div>
        <div className="overview-card">
          <DomainTreemap {...this.props} sizeMetric="ncloc"/>
        </div>
      </div>
    </div>;

  }
});
