/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
import React from 'react';
import moment from 'moment';
import QualityGate from '../qualityGate/QualityGate';
import BugsAndVulnerabilities from '../main/BugsAndVulnerabilities';
import CodeSmells from '../main/CodeSmells';
import Coverage from '../main/Coverage';
import Duplications from '../main/Duplications';
import Meta from './../meta/Meta';
import { getMeasuresAndMeta } from '../../../api/measures';
import { getTimeMachineData } from '../../../api/time-machine';
import { enhanceMeasuresWithMetrics } from '../../../helpers/measures';
import { getLeakPeriod } from '../../../helpers/periods';
import { ComponentType } from '../propTypes';
import { TooltipsContainer } from '../../../components/mixins/tooltips-mixin';
import '../styles.css';

const METRICS = [
  // quality gate
  'alert_status',
  'quality_gate_details',

  // bugs
  'bugs',
  'new_bugs',
  'reliability_rating',
  'new_reliability_rating',

  // vulnerabilities
  'vulnerabilities',
  'new_vulnerabilities',
  'security_rating',
  'new_security_rating',

  // code smells
  'code_smells',
  'new_code_smells',
  'sqale_rating',
  'new_maintainability_rating',
  'sqale_index',
  'new_technical_debt',

  // coverage
  'coverage',
  'new_coverage',
  'new_lines_to_cover',
  'tests',

  // duplications
  'duplicated_lines_density',
  'new_duplicated_lines_density',
  'duplicated_blocks',

  // size
  'ncloc',
  'ncloc_language_distribution',
  'new_lines'
];

const HISTORY_METRICS_LIST = ['sqale_index', 'duplicated_lines_density', 'ncloc', 'coverage'];

export default class OverviewApp extends React.PureComponent {
  static propTypes = {
    component: ComponentType.isRequired
  };

  state = {
    loading: true
  };

  componentDidMount() {
    this.mounted = true;
    document.querySelector('html').classList.add('dashboard-page');
    this.loadMeasures(this.props.component.key).then(() => this.loadHistory(this.props.component));
  }

  componentDidUpdate(prevProps) {
    if (this.props.component.key !== prevProps.component.key) {
      this.loadMeasures(this.props.component.key).then(() =>
        this.loadHistory(this.props.component)
      );
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    document.querySelector('html').classList.remove('dashboard-page');
  }

  loadMeasures(componentKey) {
    this.setState({ loading: true });

    return getMeasuresAndMeta(componentKey, METRICS, {
      additionalFields: 'metrics,periods'
    }).then(r => {
      if (this.mounted) {
        this.setState({
          loading: false,
          measures: enhanceMeasuresWithMetrics(r.component.measures, r.metrics),
          periods: r.periods
        });
      }
    });
  }

  loadHistory(component) {
    return getTimeMachineData(component.key, HISTORY_METRICS_LIST).then(r => {
      if (this.mounted) {
        const history = {};
        r.measures.forEach(measure => {
          const measureHistory = measure.history.map(analysis => ({
            date: moment(analysis.date).toDate(),
            value: analysis.value
          }));
          history[measure.metric] = measureHistory;
        });
        const historyStartDate = history[HISTORY_METRICS_LIST[0]][0].date;
        this.setState({ history, historyStartDate });
      }
    });
  }

  renderLoading() {
    return (
      <div className="text-center">
        <i className="spinner spinner-margin" />
      </div>
    );
  }

  render() {
    const { component } = this.props;
    const { loading, measures, periods, history, historyStartDate } = this.state;

    if (loading) {
      return this.renderLoading();
    }

    const leakPeriod = getLeakPeriod(periods);
    const domainProps = { component, measures, leakPeriod, history, historyStartDate };

    return (
      <div className="page page-limited">
        <div className="overview page-with-sidebar">
          <div className="overview-main page-main">
            <QualityGate component={component} measures={measures} />

            <TooltipsContainer>
              <div className="overview-domains-list">
                <BugsAndVulnerabilities {...domainProps} />
                <CodeSmells {...domainProps} />
                <Coverage {...domainProps} />
                <Duplications {...domainProps} />
              </div>
            </TooltipsContainer>
          </div>

          <div className="page-sidebar-fixed">
            <Meta component={component} measures={measures} />
          </div>
        </div>
      </div>
    );
  }
}
