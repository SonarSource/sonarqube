/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import _ from 'underscore';
import moment from 'moment';
import React from 'react';

import { Risk } from './risk';
import { CodeSmells } from './code-smells';
import { GeneralCoverage } from './coverage';
import { GeneralDuplications } from './duplications';
import { GeneralStructure } from './structure';
import { CoverageSelectionMixin } from '../components/coverage-selection-mixin';
import { getPeriodLabel, getPeriodDate } from './../helpers/periods';
import { getMeasures } from '../../../api/measures';
import { getTimeMachineData } from '../../../api/time-machine';


const METRICS_LIST = [
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
  'ncloc_language_distribution',

  'sqale_index',
  'new_technical_debt',
  'code_smells',
  'new_code_smells',
  'sqale_rating',
  'reliability_rating',
  'bugs',
  'new_bugs',
  'security_rating',
  'vulnerabilities',
  'new_vulnerabilities'
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
    this.requestMeasures().then(r => {
      const measures = this.getMeasuresValues(r);

      let leak;
      if (this.state.leakPeriodDate) {
        leak = this.getMeasuresValues(r, Number(this.props.leakPeriodIndex));
      }

      this.setState({
        ready: true,
        measures,
        leak,
        coverageMetricPrefix: this.getCoverageMetricPrefix(measures)
      }, this.requestHistory);
    });
  },

  requestMeasures () {
    return getMeasures(this.props.component.key, METRICS_LIST);
  },

  getMeasuresValues (measures, period) {
    const values = {};
    measures.forEach(measure => {
      const container = period ? _.findWhere(measure.periods, { index: period }) : measure;
      if (container) {
        values[measure.metric] = container.value;
      }
    });
    return values;
  },

  requestHistory () {
    const coverageMetric = this.state.coverageMetricPrefix + 'coverage';
    const metrics = [].concat(HISTORY_METRICS_LIST, coverageMetric).join(',');
    return getTimeMachineData(this.props.component.key, metrics).then(r => {
      const history = {};
      r[0].cols.forEach((col, index) => {
        history[col.metric] = r[0].cells.map(cell => {
          const date = moment(cell.d).toDate();
          const value = cell.v[index] || 0;
          return { date, value };
        });
      });
      const historyStartDate = history[HISTORY_METRICS_LIST[0]][0].date;
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

    const coverageMetric = this.state.coverageMetricPrefix + 'coverage';
    const props = _.extend({}, this.props, this.state);

    return <div className="overview-domains-list">
      <Risk {...props}/>
      <CodeSmells {...props} history={this.state.history['sqale_index']}/>
      <GeneralCoverage {...props} coverageMetricPrefix={this.state.coverageMetricPrefix}
                                  history={this.state.history[coverageMetric]}/>
      <GeneralDuplications {...props} history={this.state.history['duplicated_lines_density']}/>
      <GeneralStructure {...props} history={this.state.history['ncloc']}/>
    </div>;
  }
});
