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
import { Spinner } from '@sonarsource/echoes-react';
import { isBefore, sub } from 'date-fns';
import { ButtonLink, FlagMessage, LightLabel, Tabs } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { translate } from '../../../helpers/l10n';
import { isDiffMetric } from '../../../helpers/measures';
import { CodeScope } from '../../../helpers/urls';
import { ApplicationPeriod } from '../../../types/application';
import { ComponentQualifier } from '../../../types/component';
import { Analysis, ProjectAnalysisEventCategory } from '../../../types/project-activity';
import { QualityGateStatus } from '../../../types/quality-gates';
import { Component, Period } from '../../../types/types';
import { MAX_ANALYSES_NB } from './ActivityPanel';
import { LeakPeriodInfo } from './LeakPeriodInfo';

export interface MeasuresPanelProps {
  analyses?: Analysis[];
  appLeak?: ApplicationPeriod;
  component: Component;
  loading?: boolean;
  period?: Period;
  qgStatuses?: QualityGateStatus[];
  isNewCode: boolean;
  onTabSelect: (tab: CodeScope) => void;
}

const SQ_UPGRADE_NOTIFICATION_TIMEOUT = { weeks: 3 };

export function TabsPanel(props: React.PropsWithChildren<MeasuresPanelProps>) {
  const {
    analyses,
    appLeak,
    component,
    loading,
    period,
    qgStatuses = [],
    isNewCode,
    children,
  } = props;
  const isApp = component.qualifier === ComponentQualifier.Application;
  const leakPeriod = isApp ? appLeak : period;

  const { failingConditionsOnNewCode, failingConditionsOnOverallCode } =
    countFailingConditions(qgStatuses);

  const recentSqUpgrade = React.useMemo(() => {
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
          return { analysis, event: sqUpgradeEvent };
        }
      }
    }

    return undefined;
  }, [analyses]);

  const scrollToLatestSqUpgradeEvent = () => {
    if (recentSqUpgrade) {
      document
        .querySelector(`[data-analysis-key="${recentSqUpgrade.analysis.key}"]`)
        ?.scrollIntoView({
          behavior: 'smooth',
          block: 'center',
          inline: 'center',
        });
    }
  };

  const tabs = [
    {
      value: CodeScope.New,
      label: translate('overview.new_code'),
      counter: failingConditionsOnNewCode,
    },
    {
      value: CodeScope.Overall,
      label: translate('overview.overall_code'),
      counter: failingConditionsOnOverallCode,
    },
  ];

  return (
    <div className="sw-mt-3" data-testid="overview__measures-panel">
      {loading ? (
        <div>
          <Spinner isLoading={loading} />
        </div>
      ) : (
        <>
          {recentSqUpgrade && (
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
                    sqVersion: recentSqUpgrade.event.name,
                  }}
                />
              </FlagMessage>
            </div>
          )}
          <div className="sw-flex sw-items-center sw--mx-6">
            <Tabs
              onChange={props.onTabSelect}
              options={tabs}
              value={isNewCode ? CodeScope.New : CodeScope.Overall}
            >
              {isNewCode && leakPeriod && (
                <LightLabel className="sw-body-sm sw-flex sw-items-center sw-mr-6">
                  <span className="sw-mr-1">{translate('overview.new_code')}:</span>
                  <LeakPeriodInfo leakPeriod={leakPeriod} />
                </LightLabel>
              )}
            </Tabs>
          </div>

          {component.qualifier === ComponentQualifier.Application && component.needIssueSync && (
            <FlagMessage className="sw-mt-4" variant="info">
              <span>
                {`${translate('indexation.in_progress')} ${translate(
                  'indexation.details_unavailable',
                )}`}
                <DocumentationLink
                  className="sw-ml-1 sw-whitespace-nowrap"
                  to="/instance-administration/reindexing/"
                >
                  {translate('learn_more')}
                </DocumentationLink>
              </span>
            </FlagMessage>
          )}

          {children}
        </>
      )}
    </div>
  );
}

export default React.memo(TabsPanel);

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
