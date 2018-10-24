/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { Dispatch } from 'redux';
import { connect } from 'react-redux';
import { withRouter, WithRouterProps } from 'react-router';
import App from './App';
import throwGlobalError from '../../../app/utils/throwGlobalError';
import { getCurrentUser, getMetrics, getMetricsKey } from '../../../store/rootReducer';
import { fetchMetrics } from '../../../store/rootActions';
import { getMeasuresAndMeta } from '../../../api/measures';
import { getLeakPeriod } from '../../../helpers/periods';
import { enhanceMeasure } from '../../../components/measure/utils';
import { getBranchLikeQuery } from '../../../helpers/branches';
import {
  BranchLike,
  ComponentMeasure,
  CurrentUser,
  Measure,
  MeasureEnhanced,
  Metric,
  Period
} from '../../../app/types';

interface StateToProps {
  currentUser: CurrentUser;
  metrics: { [metric: string]: Metric };
  metricsKey: string[];
}

interface DispatchToProps {
  fetchMeasures: (
    component: string,
    metricsKey: string[],
    branchLike?: BranchLike
  ) => Promise<{ component: ComponentMeasure; measures: MeasureEnhanced[]; leakPeriod?: Period }>;
  fetchMetrics: () => void;
}

interface OwnProps {
  branchLike?: BranchLike;
  component: ComponentMeasure;
}

const mapStateToProps = (state: any): StateToProps => ({
  currentUser: getCurrentUser(state),
  metrics: getMetrics(state),
  metricsKey: getMetricsKey(state)
});

function banQualityGate({ measures = [], qualifier }: ComponentMeasure): Measure[] {
  const bannedMetrics: string[] = [];
  if (!['VW', 'SVW'].includes(qualifier)) {
    bannedMetrics.push('alert_status');
  }
  if (qualifier === 'APP') {
    bannedMetrics.push('releasability_rating', 'releasability_effort');
  }
  return measures.filter(measure => !bannedMetrics.includes(measure.metric));
}

const fetchMeasures = (component: string, metricsKey: string[], branchLike?: BranchLike) => (
  _dispatch: Dispatch,
  getState: () => any
) => {
  if (metricsKey.length <= 0) {
    return Promise.resolve({ component: {}, measures: [], leakPeriod: null });
  }

  return getMeasuresAndMeta(component, metricsKey, {
    additionalFields: 'periods',
    ...getBranchLikeQuery(branchLike)
  }).then(({ component, periods }) => {
    const measures = banQualityGate(component).map(measure =>
      enhanceMeasure(measure, getMetrics(getState()))
    );

    const newBugs = measures.find(measure => measure.metric.key === 'new_bugs');
    const applicationPeriods = newBugs ? [{ index: 1 } as Period] : [];
    const leakPeriod = getLeakPeriod(component.qualifier === 'APP' ? applicationPeriods : periods);
    return { component, measures, leakPeriod };
  }, throwGlobalError);
};

const mapDispatchToProps: DispatchToProps = { fetchMeasures: fetchMeasures as any, fetchMetrics };

export default withRouter<OwnProps>(
  connect<StateToProps, DispatchToProps, OwnProps & WithRouterProps>(
    mapStateToProps,
    mapDispatchToProps
  )(App)
);
