/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as React from 'react';
import { uniq } from 'lodash';
import { connect } from 'react-redux';
import ApplicationQualityGate from '../qualityGate/ApplicationQualityGate';
import BugsAndVulnerabilities from '../main/BugsAndVulnerabilities';
import CodeSmells from '../main/CodeSmells';
import Coverage from '../main/Coverage';
import Duplications from '../main/Duplications';
import MetaContainer from '../meta/MetaContainer';
import QualityGate from '../qualityGate/QualityGate';
import A11ySkipTarget from '../../../app/components/a11y/A11ySkipTarget';
import { getMeasuresAndMeta } from '../../../api/measures';
import { getAllTimeMachineData } from '../../../api/time-machine';
import { parseDate } from '../../../helpers/dates';
import { enhanceMeasuresWithMetrics } from '../../../helpers/measures';
import { getLeakPeriod } from '../../../helpers/periods';
import { METRICS, HISTORY_METRICS_LIST } from '../utils';
import {
  DEFAULT_GRAPH,
  getDisplayedHistoryMetrics,
  getProjectActivityGraph
} from '../../projectActivity/utils';
import {
  isSameBranchLike,
  getBranchLikeQuery,
  isLongLivingBranch
} from '../../../helpers/branches';
import { fetchMetrics } from '../../../store/rootActions';
import { getMetrics, Store } from '../../../store/rootReducer';
import { translate } from '../../../helpers/l10n';
import '../styles.css';

interface Props {
  branchLike?: T.BranchLike;
  component: T.Component;
  fetchMetrics: () => void;
  onComponentChange: (changes: {}) => void;
  metrics: T.Dict<T.Metric>;
}

interface State {
  history?: {
    [metric: string]: Array<{ date: Date; value?: string }>;
  };
  historyStartDate?: Date;
  loading: boolean;
  measures: T.MeasureEnhanced[];
  periods?: T.Period[];
}

export class OverviewApp extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true, measures: [] };

  componentDidMount() {
    this.mounted = true;
    this.props.fetchMetrics();
    this.loadMeasures().then(this.loadHistory, () => {});
  }

  componentDidUpdate(prevProps: Props) {
    if (
      this.props.component.key !== prevProps.component.key ||
      !isSameBranchLike(this.props.branchLike, prevProps.branchLike)
    ) {
      this.loadMeasures().then(this.loadHistory, () => {});
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  getApplicationLeakPeriod = () => {
    return this.state.measures.find(measure => measure.metric.key === 'new_bugs')
      ? ({ index: 1 } as T.Period)
      : undefined;
  };

  isEmpty = () => {
    return (
      this.state.measures === undefined ||
      this.state.measures.find(measure => measure.metric.key === 'ncloc') === undefined
    );
  };

  loadHistory = () => {
    const { branchLike, component } = this.props;

    const { graph, customGraphs } = getProjectActivityGraph(component.key);
    let graphMetrics = getDisplayedHistoryMetrics(graph, customGraphs);
    if (!graphMetrics || graphMetrics.length <= 0) {
      graphMetrics = getDisplayedHistoryMetrics(DEFAULT_GRAPH, []);
    }

    const metrics = uniq(HISTORY_METRICS_LIST.concat(graphMetrics));
    return getAllTimeMachineData({
      ...getBranchLikeQuery(branchLike),
      component: component.key,
      metrics: metrics.join()
    }).then(r => {
      if (this.mounted) {
        const history: T.Dict<Array<{ date: Date; value?: string }>> = {};
        r.measures.forEach(measure => {
          const measureHistory = measure.history.map(analysis => ({
            date: parseDate(analysis.date),
            value: analysis.value
          }));
          history[measure.metric] = measureHistory;
        });
        const historyStartDate = history[HISTORY_METRICS_LIST[0]][0].date;
        this.setState({ history, historyStartDate });
      }
    });
  };

  loadMeasures = () => {
    const { branchLike, component } = this.props;
    this.setState({ loading: true });

    return getMeasuresAndMeta(component.key, METRICS, {
      additionalFields: 'metrics,periods',
      ...getBranchLikeQuery(branchLike)
    }).then(
      ({ component, metrics, periods }) => {
        if (this.mounted && metrics && component.measures) {
          this.setState({
            loading: false,
            measures: enhanceMeasuresWithMetrics(component.measures, metrics),
            periods
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  renderEmpty = () => {
    const { component } = this.props;
    const isApp = component.qualifier === 'APP';
    return (
      <div className="overview-main page-main">
        <h3>
          {!this.state.measures ||
          !this.state.measures.find(measure => measure.metric.key === 'projects')
            ? translate(isApp ? 'portfolio.app.empty' : 'overview.project.empty')
            : translate(
                isApp ? 'portfolio.app.no_lines_of_code' : 'overview.project.no_lines_of_code'
              )}
        </h3>
      </div>
    );
  };

  renderLoading = () => {
    return (
      <div className="text-center">
        <i className="spinner spinner-margin" />
      </div>
    );
  };

  renderMain = () => {
    const { branchLike, component } = this.props;
    const { periods, measures, history, historyStartDate } = this.state;
    const leakPeriod =
      component.qualifier === 'APP' ? this.getApplicationLeakPeriod() : getLeakPeriod(periods);
    const domainProps = {
      branchLike,
      component,
      measures,
      leakPeriod,
      history,
      historyStartDate
    };

    if (this.isEmpty()) {
      return this.renderEmpty();
    }

    return (
      <div className="overview-main page-main">
        {component.qualifier === 'APP' ? (
          <ApplicationQualityGate
            branch={isLongLivingBranch(branchLike) ? branchLike : undefined}
            component={component}
          />
        ) : (
          <QualityGate branchLike={branchLike} component={component} measures={measures} />
        )}

        <div className="overview-domains-list">
          <BugsAndVulnerabilities {...domainProps} />
          <CodeSmells {...domainProps} />
          <Coverage {...domainProps} />
          <Duplications {...domainProps} />
        </div>
      </div>
    );
  };

  render() {
    const { branchLike, component } = this.props;
    const { loading, measures, history } = this.state;

    if (loading) {
      return this.renderLoading();
    }

    return (
      <div className="page page-limited">
        <div className="overview page-with-sidebar">
          <A11ySkipTarget anchor="overview_main" />

          {this.renderMain()}

          <div className="overview-sidebar page-sidebar-fixed">
            <MetaContainer
              branchLike={branchLike}
              component={component}
              history={history}
              measures={measures}
              metrics={this.props.metrics}
              onComponentChange={this.props.onComponentChange}
            />
          </div>
        </div>
      </div>
    );
  }
}

const mapDispatchToProps = { fetchMetrics };

const mapStateToProps = (state: Store) => ({ metrics: getMetrics(state) });

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(OverviewApp);
