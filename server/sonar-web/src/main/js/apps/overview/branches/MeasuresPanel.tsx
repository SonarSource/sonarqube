/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import {
  Card,
  CoverageIndicator,
  DeferredSpinner,
  DuplicationsIndicator,
  LightLabel,
  PageTitle,
  ToggleButton,
} from 'design-system';
import * as React from 'react';
import ComponentReportActions from '../../../components/controls/ComponentReportActions';
import { Location, withRouter } from '../../../components/hoc/withRouter';
import { duplicationRatingConverter } from '../../../components/measure/utils';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { findMeasure, isDiffMetric } from '../../../helpers/measures';
import { CodeScope } from '../../../helpers/urls';
import { ApplicationPeriod } from '../../../types/application';
import { Branch } from '../../../types/branch-like';
import { ComponentQualifier } from '../../../types/component';
import { IssueType } from '../../../types/issues';
import { MetricKey } from '../../../types/metrics';
import { QualityGateStatus } from '../../../types/quality-gates';
import { Component, MeasureEnhanced, Period } from '../../../types/types';
import { MeasurementType, parseQuery } from '../utils';
import { LeakPeriodInfo } from './LeakPeriodInfo';
import MeasuresPanelIssueMeasure from './MeasuresPanelIssueMeasure';
import MeasuresPanelNoNewCode from './MeasuresPanelNoNewCode';
import MeasuresPanelPercentMeasure from './MeasuresPanelPercentMeasure';

export interface MeasuresPanelProps {
  appLeak?: ApplicationPeriod;
  branch?: Branch;
  component: Component;
  loading?: boolean;
  measures?: MeasureEnhanced[];
  period?: Period;
  location: Location;
  qgStatuses?: QualityGateStatus[];
}

export enum MeasuresPanelTabs {
  New = 'new',
  Overall = 'overall',
}

export function MeasuresPanel(props: MeasuresPanelProps) {
  const {
    appLeak,
    branch,
    component,
    loading,
    measures = [],
    period,
    qgStatuses = [],
    location,
  } = props;

  const hasDiffMeasures = measures.some((m) => isDiffMetric(m.metric.key));
  const isApp = component.qualifier === ComponentQualifier.Application;
  const leakPeriod = isApp ? appLeak : period;
  const query = parseQuery(location.query);

  const { failingConditionsOnNewCode, failingConditionsOnOverallCode } =
    countFailingConditions(qgStatuses);
  const failingConditions = failingConditionsOnNewCode + failingConditionsOnOverallCode;

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
      value: MeasuresPanelTabs.New,
      label: translate('overview.new_code'),
      counter: failingConditionsOnNewCode,
    },
    {
      value: MeasuresPanelTabs.Overall,
      label: translate('overview.overall_code'),
      counter: failingConditionsOnOverallCode,
    },
  ];

  return (
    <div data-test="overview__measures-panel">
      <div className="sw-float-right -sw-mt-6">
        <ComponentReportActions component={component} branch={branch} />
      </div>
      <h2 className="sw-flex sw-mb-4">
        <PageTitle text={translate('overview.measures')} />
      </h2>

      {loading ? (
        <div>
          <DeferredSpinner loading={loading} />
        </div>
      ) : (
        <>
          <div className="sw-flex sw-items-center">
            <ToggleButton onChange={(key) => selectTab(key)} options={tabs} value={tab} />
            {failingConditions > 0 && (
              <LightLabel className="sw-body-sm-highlight sw-ml-8">
                {translateWithParameters('overview.X_conditions_failed', failingConditions)}
              </LightLabel>
            )}
          </div>

          {tab === MeasuresPanelTabs.New && leakPeriod ? (
            <LightLabel className="sw-body-sm sw-flex sw-items-center sw-mt-4">
              <span className="sw-mr-1">{translate('overview.new_code')}:</span>
              <LeakPeriodInfo leakPeriod={leakPeriod} />
            </LightLabel>
          ) : (
            <div className="sw-h-4 sw-pt-1 sw-mt-4" />
          )}

          {!hasDiffMeasures && isNewCodeTab ? (
            <MeasuresPanelNoNewCode branch={branch} component={component} period={period} />
          ) : (
            <div className="sw-grid sw-grid-cols-2 sw-gap-4 sw-mt-4">
              {[
                IssueType.Bug,
                IssueType.CodeSmell,
                IssueType.Vulnerability,
                IssueType.SecurityHotspot,
              ].map((type: IssueType) => (
                <Card key={type} className="sw-p-8">
                  <MeasuresPanelIssueMeasure
                    branchLike={branch}
                    component={component}
                    isNewCodeTab={isNewCodeTab}
                    measures={measures}
                    type={type}
                  />
                </Card>
              ))}

              {(findMeasure(measures, MetricKey.coverage) ||
                findMeasure(measures, MetricKey.new_coverage)) && (
                <Card className="sw-p-8" data-test="overview__measures-coverage">
                  <MeasuresPanelPercentMeasure
                    branchLike={branch}
                    component={component}
                    measures={measures}
                    ratingIcon={renderCoverageIcon}
                    secondaryMetricKey={MetricKey.tests}
                    type={MeasurementType.Coverage}
                    useDiffMetric={isNewCodeTab}
                  />
                </Card>
              )}

              <Card className="sw-p-8">
                <MeasuresPanelPercentMeasure
                  branchLike={branch}
                  component={component}
                  measures={measures}
                  ratingIcon={renderDuplicationIcon}
                  secondaryMetricKey={MetricKey.duplicated_blocks}
                  type={MeasurementType.Duplication}
                  useDiffMetric={isNewCodeTab}
                />
              </Card>
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default withRouter(React.memo(MeasuresPanel));

function renderCoverageIcon(value?: string) {
  return <CoverageIndicator value={value} size="md" />;
}

function renderDuplicationIcon(value?: string) {
  const rating = value !== undefined ? duplicationRatingConverter(Number(value)) : undefined;

  return <DuplicationsIndicator rating={rating} size="md" />;
}

function countFailingConditions(qgStatuses: QualityGateStatus[]) {
  let failingConditionsOnNewCode = 0;
  let failingConditionsOnOverallCode = 0;

  qgStatuses.forEach(({ failedConditions }) => {
    failedConditions.forEach((condition) => {
      if (isDiffMetric(condition.metric)) {
        failingConditionsOnNewCode += 1;
      } else {
        failingConditionsOnOverallCode += 1;
      }
    });
  });

  return { failingConditionsOnNewCode, failingConditionsOnOverallCode };
}
