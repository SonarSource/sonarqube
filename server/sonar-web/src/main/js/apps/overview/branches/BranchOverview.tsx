/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { sortBy, uniq } from 'lodash';
import * as React from 'react';
import { parseDate, toNotSoISOString } from 'sonar-ui-common/helpers/dates';
import { isDefined } from 'sonar-ui-common/helpers/types';
import { getApplicationLeak } from '../../../api/application';
import { getMeasuresAndMeta } from '../../../api/measures';
import { getProjectActivity } from '../../../api/projectActivity';
import { getApplicationQualityGate, getQualityGateProjectStatus } from '../../../api/quality-gates';
import { getTimeMachineData } from '../../../api/time-machine';
import {
  getActivityGraph,
  getHistoryMetrics,
  saveActivityGraph
} from '../../../components/activity-graph/utils';
import {
  getBranchLikeDisplayName,
  getBranchLikeQuery,
  isSameBranchLike
} from '../../../helpers/branch-like';
import { enhanceConditionWithMeasure, enhanceMeasuresWithMetrics } from '../../../helpers/measures';
import { getLeakPeriod } from '../../../helpers/periods';
import {
  extractStatusConditionsFromApplicationStatusChildProject,
  extractStatusConditionsFromProjectStatus
} from '../../../helpers/qualityGates';
import { ApplicationPeriod } from '../../../types/application';
import { BranchLike } from '../../../types/branch-like';
import { ComponentQualifier } from '../../../types/component';
import { MetricKey } from '../../../types/metrics';
import { GraphType, MeasureHistory } from '../../../types/project-activity';
import { QualityGateStatus, QualityGateStatusCondition } from '../../../types/quality-gates';
import '../styles.css';
import { HISTORY_METRICS_LIST, METRICS } from '../utils';
import BranchOverviewRenderer from './BranchOverviewRenderer';

interface Props {
  branchLike?: BranchLike;
  component: T.Component;
}

interface State {
  analyses?: T.Analysis[];
  appLeak?: ApplicationPeriod;
  graph: GraphType;
  loadingHistory?: boolean;
  loadingStatus?: boolean;
  measures?: T.MeasureEnhanced[];
  measuresHistory?: MeasureHistory[];
  metrics?: T.Metric[];
  periods?: T.Period[];
  qgStatuses?: QualityGateStatus[];
}

export const BRANCH_OVERVIEW_ACTIVITY_GRAPH = 'sonar_branch_overview.graph';

// Get all history data over the past year.
const FROM_DATE = toNotSoISOString(new Date().setFullYear(new Date().getFullYear() - 1));

export default class BranchOverview extends React.PureComponent<Props, State> {
  mounted = false;
  state: State;

  constructor(props: Props) {
    super(props);

    const { graph } = getActivityGraph(BRANCH_OVERVIEW_ACTIVITY_GRAPH, props.component.key);
    this.state = { graph };
  }

  componentDidMount() {
    this.mounted = true;
    this.loadStatus();
    this.loadHistory();
  }

  componentDidUpdate(prevProps: Props) {
    if (
      this.props.component.key !== prevProps.component.key ||
      !isSameBranchLike(this.props.branchLike, prevProps.branchLike)
    ) {
      this.loadStatus();
      this.loadHistory();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadStatus = () => {
    if (this.props.component.qualifier === ComponentQualifier.Application) {
      this.loadApplicationStatus();
    } else {
      this.loadProjectStatus();
    }
  };

  loadApplicationStatus = async () => {
    const { branchLike, component } = this.props;
    this.setState({ loadingStatus: true });

    // Start by loading the application quality gate info, as well as the meta
    // data for the application as a whole.
    const appStatus = await getApplicationQualityGate({
      application: component.key,
      ...getBranchLikeQuery(branchLike)
    });
    const { measures: appMeasures, metrics, periods } = await this.loadMeasuresAndMeta(
      component.key
    );

    // We also need to load the application leak periods separately.
    getApplicationLeak(component.key, branchLike && getBranchLikeDisplayName(branchLike)).then(
      leaks => {
        if (this.mounted && leaks && leaks.length) {
          const sortedLeaks = sortBy(leaks, leak => {
            return new Date(leak.date);
          });
          this.setState({
            appLeak: sortedLeaks[0]
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ appLeak: undefined });
        }
      }
    );

    // We need to load the measures for each project in an application
    // individually, in order to display all QG conditions correctly. Loading
    // them at the parent application level will not get all the necessary
    // information, unfortunately, as they are aggregated.
    Promise.all(
      appStatus.projects.map(project => {
        return this.loadMeasuresAndMeta(
          project.key,
          // Only load metrics that apply to failing QG conditions; we don't
          // need the others anyway.
          project.conditions.filter(c => c.status !== 'OK').map(c => c.metric)
        ).then(({ measures }) => ({
          measures,
          project
        }));
      })
    ).then(
      results => {
        if (this.mounted) {
          const qgStatuses = results.map(({ measures = [], project }) => {
            const { key, name, status } = project;
            const conditions = extractStatusConditionsFromApplicationStatusChildProject(project);
            const failedConditions = this.getFailedConditions(conditions, measures);

            return {
              failedConditions,
              key,
              name,
              status
            };
          });

          this.setState({
            loadingStatus: false,
            measures: appMeasures,
            metrics,
            periods,
            qgStatuses
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loadingStatus: false, qgStatuses: undefined });
        }
      }
    );
  };

  loadProjectStatus = async () => {
    const {
      branchLike,
      component: { key, name }
    } = this.props;
    this.setState({ loadingStatus: true });

    const projectStatus = await getQualityGateProjectStatus({
      projectKey: key,
      ...getBranchLikeQuery(branchLike)
    });

    // Get failing condition metric keys. We need measures for them as well to
    // render them.
    const metricKeys =
      projectStatus.conditions !== undefined
        ? uniq([...METRICS, ...projectStatus.conditions.map(c => c.metricKey)])
        : METRICS;

    this.loadMeasuresAndMeta(key, metricKeys).then(
      ({ measures, metrics, periods }) => {
        if (this.mounted && measures) {
          const { ignoredConditions, status } = projectStatus;
          const conditions = extractStatusConditionsFromProjectStatus(projectStatus);
          const failedConditions = this.getFailedConditions(conditions, measures);

          const qgStatus = {
            ignoredConditions,
            failedConditions,
            key,
            name,
            status
          };

          this.setState({
            loadingStatus: false,
            measures,
            metrics,
            periods,
            qgStatuses: [qgStatus]
          });
        } else if (this.mounted) {
          this.setState({ loadingStatus: false, qgStatuses: undefined });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loadingStatus: false, qgStatuses: undefined });
        }
      }
    );
  };

  loadMeasuresAndMeta = (componentKey: string, metricKeys: string[] = []) => {
    const { branchLike } = this.props;

    return getMeasuresAndMeta(componentKey, metricKeys.length > 0 ? metricKeys : METRICS, {
      additionalFields: 'metrics,periods',
      ...getBranchLikeQuery(branchLike)
    }).then(({ component: { measures }, metrics, periods }) => {
      return {
        measures: enhanceMeasuresWithMetrics(measures || [], metrics || []),
        metrics,
        periods
      };
    });
  };

  loadHistory = () => {
    this.setState({ loadingHistory: true });

    return Promise.all([this.loadHistoryMeasures(), this.loadAnalyses()]).then(
      this.doneLoadingHistory,
      this.doneLoadingHistory
    );
  };

  loadHistoryMeasures = () => {
    const { branchLike, component } = this.props;
    const { graph } = this.state;

    const graphMetrics = getHistoryMetrics(graph, []);
    const metrics = uniq([...HISTORY_METRICS_LIST, ...graphMetrics]);

    return getTimeMachineData({
      ...getBranchLikeQuery(branchLike),
      from: FROM_DATE,
      component: component.key,
      metrics: metrics.join()
    }).then(
      ({ measures }) => {
        if (this.mounted) {
          this.setState({
            measuresHistory: measures.map(measure => ({
              metric: measure.metric,
              history: measure.history.map(analysis => ({
                date: parseDate(analysis.date),
                value: analysis.value
              }))
            }))
          });
        }
      },
      () => {}
    );
  };

  loadAnalyses = () => {
    const { branchLike } = this.props;

    return getProjectActivity({
      ...getBranchLikeQuery(branchLike),
      project: this.getTopLevelComponent(),
      from: FROM_DATE
    }).then(
      ({ analyses }) => {
        if (this.mounted) {
          this.setState({
            analyses
          });
        }
      },
      () => {}
    );
  };

  getFailedConditions = (
    conditions: QualityGateStatusCondition[],
    measures: T.MeasureEnhanced[]
  ) => {
    return (
      conditions
        .filter(c => c.level !== 'OK')
        // Enhance them with Metric information, which will be needed
        // to render the conditions properly.
        .map(c => enhanceConditionWithMeasure(c, measures))
        // The enhancement will return undefined if it cannot find the
        // appropriate measure. Make sure we filter them out.
        .filter(isDefined)
    );
  };

  getTopLevelComponent = () => {
    const { component } = this.props;
    let current = component.breadcrumbs.length - 1;
    while (
      current > 0 &&
      !([
        ComponentQualifier.Project,
        ComponentQualifier.Portfolio,
        ComponentQualifier.Application
      ] as string[]).includes(component.breadcrumbs[current].qualifier)
    ) {
      current--;
    }
    return component.breadcrumbs[current].key;
  };

  doneLoadingHistory = () => {
    if (this.mounted) {
      this.setState({
        loadingHistory: false
      });
    }
  };

  handleGraphChange = (graph: GraphType) => {
    const { component } = this.props;
    saveActivityGraph(BRANCH_OVERVIEW_ACTIVITY_GRAPH, component.key, graph);
    this.setState({ graph, loadingHistory: true }, () => {
      this.loadHistoryMeasures().then(this.doneLoadingHistory, this.doneLoadingHistory);
    });
  };

  render() {
    const { branchLike, component } = this.props;
    const {
      analyses,
      appLeak,
      graph,
      loadingStatus,
      loadingHistory,
      measures,
      measuresHistory,
      metrics,
      periods,
      qgStatuses
    } = this.state;

    const leakPeriod =
      component.qualifier === ComponentQualifier.Application ? appLeak : getLeakPeriod(periods);

    const projectIsEmpty =
      loadingStatus === false &&
      (measures === undefined ||
        measures.find(measure =>
          ([MetricKey.lines, MetricKey.new_lines] as string[]).includes(measure.metric.key)
        ) === undefined);

    return (
      <BranchOverviewRenderer
        analyses={analyses}
        branchLike={branchLike}
        component={component}
        graph={graph}
        leakPeriod={leakPeriod}
        loadingHistory={loadingHistory}
        loadingStatus={loadingStatus}
        measures={measures}
        measuresHistory={measuresHistory}
        metrics={metrics}
        onGraphChange={this.handleGraphChange}
        projectIsEmpty={projectIsEmpty}
        qgStatuses={qgStatuses}
      />
    );
  }
}
