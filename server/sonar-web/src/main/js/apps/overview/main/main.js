import _ from 'underscore';
import moment from 'moment';
import React from 'react';

import { GeneralIssues } from './issues';
import { GeneralCoverage } from './coverage';
import { GeneralDuplications } from './duplications';
import { GeneralSize } from './size';
import { getPeriodLabel, getPeriodDate } from './../helpers/periods';
import { getMeasuresAndVariations } from '../../../api/measures';
import { getFacet, getIssuesCount } from '../../../api/issues';
import { getTimeMachineData } from '../../../api/time-machine';
import { SEVERITIES } from '../../../helpers/constants';


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
  'overall_coverage',
  'duplicated_lines_density',
  'ncloc'
];


function getFacetValue (facet, key) {
  return _.findWhere(facet, { val: key }).count;
}


export default React.createClass({
  propTypes: {
    leakPeriodIndex: React.PropTypes.string.isRequired
  },

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
      this.requestLeakIssuesAndDebt(),
      this.requestIssuesLeakSeverities()
    ]).then(responses => {
      let measures = this.getMeasuresValues(responses[0], 'value');
      measures.issues = responses[1].issues;
      measures.debt = responses[1].debt;

      let leak;
      if (this.state.leakPeriodLabel) {
        leak = this.getMeasuresValues(responses[0], 'var' + this.props.leakPeriodIndex);
        leak.issues = responses[2].issues;
        leak.debt = responses[2].debt;
        leak.issuesSeverities = SEVERITIES.map(s => getFacetValue(responses[3].facet, s));
      }

      this.setState({
        ready: true,
        measures: measures,
        leak: leak
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
    if (!this.state.leakPeriodLabel) {
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

  requestIssuesLeakSeverities() {
    if (!this.state.leakPeriodLabel) {
      return Promise.resolve();
    }

    let createdAfter = moment(this.state.leakPeriodDate).format('YYYY-MM-DDTHH:mm:ssZZ');

    return getFacet({
      componentUuids: this.props.component.id,
      createdAfter: createdAfter,
      resolved: 'false'
    }, 'severities');
  },

  requestHistory () {
    let metrics = HISTORY_METRICS_LIST.join(',');
    return getTimeMachineData(this.props.component.key, metrics).then(r => {
      let history = {};
      r[0].cols.forEach((col, index) => {
        history[col.metric] = r[0].cells.map(cell => {
          let date = moment(cell.d).toDate();
          let value = cell.v[index] || 0;
          return { date, value };
        });
      });
      this.setState({ history });
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

    let props = _.extend({}, this.props, this.state);

    return <div className="overview-domains-list">
      <GeneralIssues {...props} history={this.state.history['sqale_index']}/>
      <GeneralCoverage {...props} history={this.state.history['overall_coverage']}/>
      <GeneralDuplications {...props} history={this.state.history['duplicated_lines_density']}/>
      <GeneralSize {...props} history={this.state.history['ncloc']}/>
    </div>;
  }
});
