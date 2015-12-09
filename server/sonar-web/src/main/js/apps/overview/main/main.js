import _ from 'underscore';
import moment from 'moment';
import React from 'react';

import { GeneralDebt } from './debt';
import { GeneralCoverage } from './coverage';
import { GeneralDuplications } from './duplications';
import { GeneralStructure } from './structure';
import { CoverageSelectionMixin } from '../components/coverage-selection-mixin';
import { getPeriodLabel, getPeriodDate } from './../helpers/periods';
import { getMeasuresAndVariations } from '../../../api/measures';
import { getIssuesCount } from '../../../api/issues';
import { getTimeMachineData } from '../../../api/time-machine';


const METRICS_LIST = [
  'sqale_rating',
  'overall_coverage',
  'new_overall_coverage',
  'coverage',
  'new_coverage',
  'it_coverage',
  'new_it_coverage',
  'tests',
  'duplicated_lines_density',
  'duplicated_blocks',
  'ncloc',
  'ncloc_language_distribution'
];

const HISTORY_METRICS_LIST = [
  'sqale_index',
  'duplicated_lines_density',
  'ncloc'
];


export default React.createClass({
  propTypes: {
    leakPeriodIndex: React.PropTypes.string.isRequired
  },

  mixins: [CoverageSelectionMixin],

  getInitialState() {
    return {
      ready: false,
      history: {},
      leakPeriodLabel: getPeriodLabel(this.props.component.periods, this.props.leakPeriodIndex),
      leakPeriodDate: getPeriodDate(this.props.component.periods, this.props.leakPeriodIndex)
    };
  },

  componentDidMount() {
    Promise.all([
      this.requestMeasures(),
      this.requestIssuesAndDebt(),
      this.requestLeakIssuesAndDebt()
    ]).then(responses => {
      let measures = this.getMeasuresValues(responses[0], 'value');
      measures.issues = responses[1].issues;
      measures.debt = responses[1].debt;

      let leak;
      if (this.state.leakPeriodDate) {
        leak = this.getMeasuresValues(responses[0], 'var' + this.props.leakPeriodIndex);
        leak.issues = responses[2].issues;
        leak.debt = responses[2].debt;
      }

      this.setState({
        ready: true,
        measures: measures,
        leak: leak,
        coverageMetricPrefix: this.getCoverageMetricPrefix(measures)
      }, this.requestHistory);
    });
  },

  requestMeasures () {
    return getMeasuresAndVariations(this.props.component.key, METRICS_LIST);
  },

  getMeasuresValues (measures, fieldKey) {
    let values = {};
    Object.keys(measures).forEach(measureKey => {
      values[measureKey] = measures[measureKey][fieldKey];
    });
    return values;
  },

  requestIssuesAndDebt() {
    // FIXME requesting severities facet only to get debtTotal
    return getIssuesCount({
      componentUuids: this.props.component.id,
      resolved: 'false',
      facets: 'severities'
    });
  },

  requestLeakIssuesAndDebt() {
    if (!this.state.leakPeriodDate) {
      return Promise.resolve();
    }

    let createdAfter = moment(this.state.leakPeriodDate).format('YYYY-MM-DDTHH:mm:ssZZ');

    // FIXME requesting severities facet only to get debtTotal
    return getIssuesCount({
      componentUuids: this.props.component.id,
      createdAfter: createdAfter,
      resolved: 'false',
      facets: 'severities'
    });
  },

  requestHistory () {
    let coverageMetric = this.state.coverageMetricPrefix + 'coverage';
    let metrics = [].concat(HISTORY_METRICS_LIST, coverageMetric).join(',');
    return getTimeMachineData(this.props.component.key, metrics).then(r => {
      let history = {};
      r[0].cols.forEach((col, index) => {
        history[col.metric] = r[0].cells.map(cell => {
          let date = moment(cell.d).toDate();
          let value = cell.v[index] || 0;
          return { date, value };
        });
      });
      let historyStartDate = history[HISTORY_METRICS_LIST[0]][0].date;
      this.setState({ history, historyStartDate });
    });
  },

  renderLoading () {
    return <div className="text-center">
      <i className="spinner spinner-margin"/>
    </div>;
  },

  render() {
    if (!this.state.ready) {
      return this.renderLoading();
    }

    let coverageMetric = this.state.coverageMetricPrefix + 'coverage';
    let props = _.extend({}, this.props, this.state);

    return <div className="overview-domains-list">
      <GeneralDebt {...props} history={this.state.history['sqale_index']}/>
      <GeneralCoverage {...props} coverageMetricPrefix={this.state.coverageMetricPrefix}
                                  history={this.state.history[coverageMetric]}/>
      <GeneralDuplications {...props} history={this.state.history['duplicated_lines_density']}/>
      <GeneralStructure {...props} history={this.state.history['ncloc']}/>
    </div>;
  }
});
