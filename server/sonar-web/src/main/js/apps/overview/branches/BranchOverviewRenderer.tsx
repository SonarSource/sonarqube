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
import * as React from 'react';
import { parseDate } from 'sonar-ui-common/helpers/dates';
import A11ySkipTarget from '../../../app/components/a11y/A11ySkipTarget';
import { ApplicationPeriod } from '../../../types/application';
import { BranchLike } from '../../../types/branch-like';
import { GraphType, MeasureHistory } from '../../../types/project-activity';
import { QualityGateStatus } from '../../../types/quality-gates';
import ActivityPanel from './ActivityPanel';
import { MeasuresPanel } from './MeasuresPanel';
import NoCodeWarning from './NoCodeWarning';
import QualityGatePanel from './QualityGatePanel';

export interface BranchOverviewRendererProps {
  analyses?: T.Analysis[];
  branchLike?: BranchLike;
  component: T.Component;
  graph?: GraphType;
  leakPeriod?: T.Period | ApplicationPeriod;
  loadingHistory?: boolean;
  loadingStatus?: boolean;
  measures?: T.MeasureEnhanced[];
  measuresHistory?: MeasureHistory[];
  metrics?: T.Metric[];
  onGraphChange: (graph: GraphType) => void;
  projectIsEmpty?: boolean;
  qgStatuses?: QualityGateStatus[];
}

export function BranchOverviewRenderer(props: BranchOverviewRendererProps) {
  const {
    analyses,
    branchLike,
    component,
    graph,
    leakPeriod,
    loadingHistory,
    loadingStatus,
    measures,
    measuresHistory = [],
    metrics = [],
    onGraphChange,
    projectIsEmpty,
    qgStatuses
  } = props;

  return (
    <div className="page page-limited">
      <div className="overview">
        <A11ySkipTarget anchor="overview_main" />

        {projectIsEmpty ? (
          <NoCodeWarning branchLike={branchLike} component={component} measures={measures} />
        ) : (
          <>
            <div className="display-flex-row">
              <div className="width-25 big-spacer-right">
                <QualityGatePanel
                  branchLike={branchLike}
                  component={component}
                  loading={loadingStatus}
                  qgStatuses={qgStatuses}
                />
              </div>

              <div className="flex-1">
                <div className="display-flex-column">
                  <MeasuresPanel
                    branchLike={branchLike}
                    component={component}
                    leakPeriod={leakPeriod}
                    loading={loadingStatus}
                    measures={measures}
                  />

                  <ActivityPanel
                    analyses={analyses}
                    branchLike={branchLike}
                    component={component}
                    graph={graph}
                    leakPeriodDate={leakPeriod && parseDate(leakPeriod.date)}
                    loading={loadingHistory}
                    measuresHistory={measuresHistory}
                    metrics={metrics}
                    onGraphChange={onGraphChange}
                  />
                </div>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

export default React.memo(BranchOverviewRenderer);
