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
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import BoxedTabs from 'sonar-ui-common/components/controls/BoxedTabs';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { isDiffMetric } from 'sonar-ui-common/helpers/measures';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import { rawSizes } from '../../../app/theme';
import { findMeasure } from '../../../helpers/measures';
import { ApplicationPeriod } from '../../../types/application';
import { BranchLike } from '../../../types/branch-like';
import { ComponentQualifier } from '../../../types/component';
import { MetricKey } from '../../../types/metrics';
import IssueLabel from '../components/IssueLabel';
import IssueRating from '../components/IssueRating';
import MeasurementLabel from '../components/MeasurementLabel';
import { IssueType, MeasurementType } from '../utils';
import DebtValue from './DebtValue';
import { DrilldownMeasureValue } from './DrilldownMeasureValue';
import { LeakPeriodInfo } from './LeakPeriodInfo';
import SecurityHotspotsReviewed from './SecurityHotspotsReviewed';

export interface MeasuresPanelProps {
  branchLike?: BranchLike;
  component: T.Component;
  leakPeriod?: T.Period | ApplicationPeriod;
  loading?: boolean;
  measures?: T.MeasureEnhanced[];
}

export enum MeasuresPanelTabs {
  New,
  Overall
}

export function MeasuresPanel(props: MeasuresPanelProps) {
  const { branchLike, component, loading, leakPeriod, measures = [] } = props;

  const hasDiffMeasures = measures.some(m => isDiffMetric(m.metric.key));
  const isApp = component.qualifier === ComponentQualifier.Application;

  const [tab, selectTab] = React.useState(MeasuresPanelTabs.New);

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
      )
    },
    {
      key: MeasuresPanelTabs.Overall,
      label: (
        <div className="text-left overview-measures-tab">
          <span className="text-bold" style={{ position: 'absolute', top: 2 * rawSizes.grid }}>
            {translate('overview.overall_code')}
          </span>
        </div>
      )
    }
  ];

  return (
    <div className="overview-panel" data-test="overview__measures-panel">
      <h2 className="overview-panel-title">{translate('overview.measures')}</h2>

      {loading ? (
        <div className="overview-panel-content overview-panel-big-padded">
          <DeferredSpinner loading={loading} />
        </div>
      ) : (
        <>
          <BoxedTabs onSelect={selectTab} selected={tab} tabs={tabs} />

          <div className="overview-panel-content flex-1 bordered">
            {!hasDiffMeasures && isNewCodeTab ? (
              <div
                className="display-flex-center display-flex-justify-center"
                style={{ height: 500 }}>
                <img
                  alt="" /* Make screen readers ignore this image; it's purely eye candy. */
                  className="spacer-right"
                  height={52}
                  src={`${getBaseUrl()}/images/source-code.svg`}
                />
                <div className="big-spacer-left text-muted" style={{ maxWidth: 500 }}>
                  <p className="spacer-bottom big-spacer-top big">
                    {translate('overview.measures.empty_explanation')}
                  </p>
                  <p>
                    <FormattedMessage
                      defaultMessage={translate('overview.measures.empty_link')}
                      id="overview.measures.empty_link"
                      values={{
                        learn_more_link: (
                          <Link to="/documentation/user-guide/clean-as-you-code/">
                            {translate('learn_more')}
                          </Link>
                        )
                      }}
                    />
                  </p>
                </div>
              </div>
            ) : (
              <>
                {[
                  IssueType.Bug,
                  IssueType.Vulnerability,
                  IssueType.SecurityHotspot,
                  IssueType.CodeSmell
                ].map((type: IssueType) => (
                  <div
                    className="display-flex-row overview-measures-row"
                    data-test={`overview__measures-${type.toString().toLowerCase()}`}
                    key={type}>
                    {type === IssueType.CodeSmell ? (
                      <>
                        <div className="overview-panel-big-padded flex-1 small display-flex-center big-spacer-left">
                          <DebtValue
                            branchLike={branchLike}
                            component={component}
                            measures={measures}
                            useDiffMetric={isNewCodeTab}
                          />
                        </div>
                        <div className="flex-1 small display-flex-center">
                          <IssueLabel
                            branchLike={branchLike}
                            component={component}
                            measures={measures}
                            type={type}
                            useDiffMetric={isNewCodeTab}
                          />
                        </div>
                      </>
                    ) : (
                      <div className="overview-panel-big-padded flex-1 small display-flex-center big-spacer-left">
                        <IssueLabel
                          branchLike={branchLike}
                          component={component}
                          docTooltip={
                            type === IssueType.SecurityHotspot
                              ? import(
                                  /* webpackMode: "eager" */ 'Docs/tooltips/metrics/security-hotspots.md'
                                )
                              : undefined
                          }
                          measures={measures}
                          type={type}
                          useDiffMetric={isNewCodeTab}
                        />
                      </div>
                    )}
                    {type === IssueType.SecurityHotspot && (
                      <div className="flex-1 small display-flex-center">
                        <SecurityHotspotsReviewed
                          measures={measures}
                          useDiffMetric={isNewCodeTab}
                        />
                      </div>
                    )}
                    {(!isApp || tab === MeasuresPanelTabs.Overall) && (
                      <div className="overview-panel-big-padded overview-measures-aside display-flex-center">
                        <IssueRating
                          branchLike={branchLike}
                          component={component}
                          measures={measures}
                          type={type}
                          useDiffMetric={isNewCodeTab}
                        />
                      </div>
                    )}
                  </div>
                ))}

                <div className="display-flex-row overview-measures-row">
                  {(findMeasure(measures, MetricKey.coverage) ||
                    findMeasure(measures, MetricKey.new_coverage)) && (
                    <div
                      className="overview-panel-huge-padded flex-1 bordered-right display-flex-center"
                      data-test="overview__measures-coverage">
                      <MeasurementLabel
                        branchLike={branchLike}
                        centered={isNewCodeTab}
                        component={component}
                        measures={measures}
                        type={MeasurementType.Coverage}
                        useDiffMetric={isNewCodeTab}
                      />

                      {tab === MeasuresPanelTabs.Overall && (
                        <div className="huge-spacer-left">
                          <DrilldownMeasureValue
                            branchLike={branchLike}
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
                      branchLike={branchLike}
                      centered={isNewCodeTab}
                      component={component}
                      measures={measures}
                      type={MeasurementType.Duplication}
                      useDiffMetric={isNewCodeTab}
                    />

                    {tab === MeasuresPanelTabs.Overall && (
                      <div className="huge-spacer-left">
                        <DrilldownMeasureValue
                          branchLike={branchLike}
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

export default React.memo(MeasuresPanel);
