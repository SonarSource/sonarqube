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
import { isBefore, sub } from 'date-fns';
import {
  ButtonLink,
  Card,
  CoverageIndicator,
  DuplicationsIndicator,
  FlagMessage,
  LightLabel,
  PageTitle,
  Spinner,
  ToggleButton,
} from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import DocLink from '../../../components/common/DocLink';
import ComponentReportActions from '../../../components/controls/ComponentReportActions';
import { duplicationRatingConverter } from '../../../components/measure/utils';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { findMeasure, isDiffMetric } from '../../../helpers/measures';
import { ApplicationPeriod } from '../../../types/application';
import { Branch } from '../../../types/branch-like';
import { ComponentQualifier } from '../../../types/component';
import { IssueType } from '../../../types/issues';
import { MetricKey } from '../../../types/metrics';
import { Analysis, ProjectAnalysisEventCategory } from '../../../types/project-activity';
import { QualityGateStatus } from '../../../types/quality-gates';
import { Component, MeasureEnhanced, Period } from '../../../types/types';
import { MeasurementType, MeasuresTabs } from '../utils';
import { MAX_ANALYSES_NB } from './ActivityPanel';
import { LeakPeriodInfo } from './LeakPeriodInfo';
import MeasuresPanelIssueMeasure from './MeasuresPanelIssueMeasure';
import MeasuresPanelPercentMeasure from './MeasuresPanelPercentMeasure';

export interface MeasuresPanelProps {
  analyses?: Analysis[];
  appLeak?: ApplicationPeriod;
  branch?: Branch;
  component: Component;
  loading?: boolean;
  measures: MeasureEnhanced[];
  period?: Period;
  qgStatuses?: QualityGateStatus[];
  isNewCode: boolean;
  onTabSelect: (tab: MeasuresTabs) => void;
}

const SQ_UPGRADE_NOTIFICATION_TIMEOUT = { weeks: 3 };

export function MeasuresPanel(props: MeasuresPanelProps) {
  const {
    analyses,
    appLeak,
    branch,
    component,
    loading,
    measures,
    period,
    qgStatuses = [],
    isNewCode,
  } = props;

  const isApp = component.qualifier === ComponentQualifier.Application;
  const leakPeriod = isApp ? appLeak : period;

  const { failingConditionsOnNewCode, failingConditionsOnOverallCode } =
    countFailingConditions(qgStatuses);
  const failingConditions = failingConditionsOnNewCode + failingConditionsOnOverallCode;

  const recentSqUpgradeEvent = React.useMemo(() => {
    if (!analyses || analyses.length === 0) {
      return undefined;
    }

    const notificationExpirationTime = sub(new Date(), SQ_UPGRADE_NOTIFICATION_TIMEOUT);

    for (const analysis of analyses.slice(0, MAX_ANALYSES_NB)) {
      if (isBefore(new Date(analysis.date), notificationExpirationTime)) {
        return undefined;
      }

      let sqUpgradeEvent = undefined;
      let hasQpUpdateEvent = false;
      for (const event of analysis.events) {
        sqUpgradeEvent =
          event.category === ProjectAnalysisEventCategory.SqUpgrade ? event : sqUpgradeEvent;
        hasQpUpdateEvent =
          hasQpUpdateEvent || event.category === ProjectAnalysisEventCategory.QualityProfile;

        if (sqUpgradeEvent !== undefined && hasQpUpdateEvent) {
          return sqUpgradeEvent;
        }
      }
    }

    return undefined;
  }, [analyses]);

  const scrollToLatestSqUpgradeEvent = () => {
    document.querySelector(`#${recentSqUpgradeEvent?.key}`)?.scrollIntoView({
      behavior: 'smooth',
      block: 'center',
      inline: 'center',
    });
  };

  const tabs = [
    {
      value: MeasuresTabs.New,
      label: translate('overview.new_code'),
      counter: failingConditionsOnNewCode,
    },
    {
      value: MeasuresTabs.Overall,
      label: translate('overview.overall_code'),
      counter: failingConditionsOnOverallCode,
    },
  ];

  return (
    <div data-test="overview__measures-panel">
      <div className="sw-float-right -sw-mt-6">
        <ComponentReportActions component={component} branch={branch} />
      </div>
      <div className="sw-flex sw-mb-4">
        <PageTitle as="h2" text={translate('overview.measures')} />
      </div>

      {loading ? (
        <div>
          <Spinner loading={loading} />
        </div>
      ) : (
        <>
          {recentSqUpgradeEvent && (
            <div>
              <FlagMessage className="sw-mb-4" variant="info">
                <FormattedMessage
                  id="overview.quality_profiles_update_after_sq_upgrade.message"
                  tagName="span"
                  values={{
                    link: (
                      <ButtonLink onClick={scrollToLatestSqUpgradeEvent}>
                        <FormattedMessage id="overview.quality_profiles_update_after_sq_upgrade.link" />
                      </ButtonLink>
                    ),
                    sqVersion: recentSqUpgradeEvent.name,
                  }}
                />
              </FlagMessage>
            </div>
          )}
          <div className="sw-flex sw-items-center">
            <ToggleButton
              onChange={props.onTabSelect}
              options={tabs}
              value={isNewCode ? MeasuresTabs.New : MeasuresTabs.Overall}
            />
            {failingConditions > 0 && (
              <LightLabel className="sw-body-sm-highlight sw-ml-8">
                {failingConditions === 1
                  ? translate('overview.1_condition_failed')
                  : translateWithParameters('overview.X_conditions_failed', failingConditions)}
              </LightLabel>
            )}
          </div>
          {isNewCode && leakPeriod ? (
            <LightLabel className="sw-body-sm sw-flex sw-items-center sw-mt-4">
              <span className="sw-mr-1">{translate('overview.new_code')}:</span>
              <LeakPeriodInfo leakPeriod={leakPeriod} />
            </LightLabel>
          ) : (
            <div className="sw-h-4 sw-pt-1 sw-mt-4" />
          )}

          {component.qualifier === ComponentQualifier.Application && component.needIssueSync && (
            <FlagMessage className="sw-mt-4" variant="info">
              <span>
                {`${translate('indexation.in_progress')} ${translate(
                  'indexation.details_unavailable',
                )}`}
                <DocLink
                  className="sw-ml-1 sw-whitespace-nowrap"
                  to="/instance-administration/reindexing/"
                >
                  {translate('learn_more')}
                </DocLink>
              </span>
            </FlagMessage>
          )}

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
                  isNewCodeTab={isNewCode}
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
                  useDiffMetric={isNewCode}
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
                useDiffMetric={isNewCode}
              />
            </Card>
          </div>
        </>
      )}
    </div>
  );
}

export default React.memo(MeasuresPanel);

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
