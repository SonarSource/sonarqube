/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { rawSizes } from '../../../app/theme';
import BoxedTabs, { getTabId, getTabPanelId } from '../../../components/controls/BoxedTabs';
import ComponentReportActions from '../../../components/controls/ComponentReportActions';
import { Location, withRouter } from '../../../components/hoc/withRouter';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import { findMeasure, isDiffMetric } from '../../../helpers/measures';
import { CodeScope } from '../../../helpers/urls';
import { ApplicationPeriod } from '../../../types/application';
import { Branch } from '../../../types/branch-like';
import { ComponentQualifier } from '../../../types/component';
import { IssueType } from '../../../types/issues';
import { MetricKey } from '../../../types/metrics';
import { Component, MeasureEnhanced, Period } from '../../../types/types';
import MeasurementLabel from '../components/MeasurementLabel';
import { MeasurementType, parseQuery } from '../utils';
import { DrilldownMeasureValue } from './DrilldownMeasureValue';
import { LeakPeriodInfo } from './LeakPeriodInfo';
import MeasuresPanelIssueMeasureRow from './MeasuresPanelIssueMeasureRow';
import MeasuresPanelNoNewCode from './MeasuresPanelNoNewCode';

export interface MeasuresPanelProps {
  appLeak?: ApplicationPeriod;
  branch?: Branch;
  component: Component;
  loading?: boolean;
  measures?: MeasureEnhanced[];
  period?: Period;
  location: Location;
}

export enum MeasuresPanelTabs {
  New = 'new',
  Overall = 'overall',
}

export function MeasuresPanel(props: MeasuresPanelProps) {
  const { appLeak, branch, component, loading, measures = [], period, location } = props;

  const hasDiffMeasures = measures.some((m) => isDiffMetric(m.metric.key));
  const isApp = component.qualifier === ComponentQualifier.Application;
  const leakPeriod = isApp ? appLeak : period;
  const query = parseQuery(location.query);

  const [tab, selectTab] = React.useState(() => {
    return query.codeScope === CodeScope.Overall
      ? MeasuresPanelTabs.Overall
      : MeasuresPanelTabs.New;
  });

  const isNewCodeTab = tab === MeasuresPanelTabs.New;

  React.useEffect(() => {
    // Open Overall tab by default if there are no new measures.
    if (loading === false && !hasDiffMeasures && isNewCodeTab) {
      selectTab(MeasuresPanelTabs.Overall);
    }
    // In this case, we explicitly do NOT want to mark tab as a dependency, as
    // it would prevent the user from selecting it, even if it's empty.
    /* eslint-disable-next-line react-hooks/exhaustive-deps */
  }, [loading, hasDiffMeasures]);

  const tabs = [
    {
      key: MeasuresPanelTabs.New,
      label: (
        <div className="text-left overview-measures-tab">
          <span className="text-bold">{translate('overview.new_code')}</span>
          {leakPeriod && <LeakPeriodInfo leakPeriod={leakPeriod} />}
        </div>
      ),
    },
    {
      key: MeasuresPanelTabs.Overall,
      label: (
        <div className="text-left overview-measures-tab">
          <span className="text-bold" style={{ position: 'absolute', top: 2 * rawSizes.grid }}>
            {translate('overview.overall_code')}
          </span>
        </div>
      ),
    },
  ];

  return (
    <div className="overview-panel" data-test="overview__measures-panel">
      <div className="display-flex-space-between display-flex-start">
        <h2 className="overview-panel-title">{translate('overview.measures')}</h2>
        <ComponentReportActions component={component} branch={branch} />
      </div>

      {loading ? (
        <div className="overview-panel-content overview-panel-big-padded">
          <DeferredSpinner loading={loading} />
        </div>
      ) : (
        <>
          <BoxedTabs onSelect={(key) => selectTab(key)} selected={tab} tabs={tabs} />

          <div
            className="overview-panel-content flex-1 bordered"
            role="tabpanel"
            id={getTabPanelId(tab)}
            aria-labelledby={getTabId(tab)}
          >
            {!hasDiffMeasures && isNewCodeTab ? (
              <MeasuresPanelNoNewCode branch={branch} component={component} period={period} />
            ) : (
              <>
                {[
                  IssueType.Bug,
                  IssueType.Vulnerability,
                  IssueType.SecurityHotspot,
                  IssueType.CodeSmell,
                ].map((type: IssueType) => (
                  <MeasuresPanelIssueMeasureRow
                    branchLike={branch}
                    component={component}
                    isNewCodeTab={isNewCodeTab}
                    key={type}
                    measures={measures}
                    type={type}
                  />
                ))}

                <div className="display-flex-row overview-measures-row">
                  {(findMeasure(measures, MetricKey.coverage) ||
                    findMeasure(measures, MetricKey.new_coverage)) && (
                    <div
                      className="overview-panel-huge-padded flex-1 bordered-right display-flex-center"
                      data-test="overview__measures-coverage"
                    >
                      <MeasurementLabel
                        branchLike={branch}
                        centered={isNewCodeTab}
                        component={component}
                        measures={measures}
                        type={MeasurementType.Coverage}
                        useDiffMetric={isNewCodeTab}
                      />

                      {tab === MeasuresPanelTabs.Overall && (
                        <div className="huge-spacer-left">
                          <DrilldownMeasureValue
                            branchLike={branch}
                            component={component}
                            measures={measures}
                            metric={MetricKey.tests}
                          />
                        </div>
                      )}
                    </div>
                  )}
                  <div className="overview-panel-huge-padded flex-1 display-flex-center">
                    <MeasurementLabel
                      branchLike={branch}
                      centered={isNewCodeTab}
                      component={component}
                      measures={measures}
                      type={MeasurementType.Duplication}
                      useDiffMetric={isNewCodeTab}
                    />

                    {tab === MeasuresPanelTabs.Overall && (
                      <div className="huge-spacer-left">
                        <DrilldownMeasureValue
                          branchLike={branch}
                          component={component}
                          measures={measures}
                          metric={MetricKey.duplicated_blocks}
                        />
                      </div>
                    )}
                  </div>
                </div>
              </>
            )}
          </div>
        </>
      )}
    </div>
  );
}

export default withRouter(React.memo(MeasuresPanel));
