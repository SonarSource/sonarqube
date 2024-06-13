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
import { Card, CardSeparator, Spinner, TextError } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { translate } from '../../../helpers/l10n';
import { isDiffMetric } from '../../../helpers/measures';
import { isApplication } from '../../../types/component';
import { QualityGateStatus } from '../../../types/quality-gates';
import { CaycStatus, Component, QualityGate } from '../../../types/types';
import IgnoredConditionWarning from '../components/IgnoredConditionWarning';
import ApplicationNonCaycProjectWarning from './ApplicationNonCaycProjectWarning';
import CleanAsYouCodeWarning from './CleanAsYouCodeWarning';
import QualityGatePanelSection from './QualityGatePanelSection';
import SonarLintPromotion from './SonarLintPromotion';

export interface QualityGatePanelProps {
  component: Pick<Component, 'key' | 'qualifier' | 'qualityGate'>;
  isNewCode?: boolean;
  loading?: boolean;
  qgStatuses?: QualityGateStatus[];
  qualityGate?: QualityGate;
  showCaycWarningInApp: boolean;
  showCaycWarningInProject: boolean;
  totalFailedConditionLength: number;
}

export function QualityGatePanel(props: QualityGatePanelProps) {
  const {
    component,
    loading,
    qgStatuses = [],
    qualityGate,
    isNewCode = false,
    totalFailedConditionLength,
    showCaycWarningInProject,
    showCaycWarningInApp,
  } = props;

  if (qgStatuses === undefined) {
    return null;
  }

  const failedQgStatuses = qgStatuses.filter((qgStatus) => qgStatus.failedConditions.length > 0);

  const totalFailedCondition = qgStatuses?.flatMap((qg) => qg.failedConditions) ?? [];

  const isApp = isApplication(component.qualifier);

  const nonCaycProjectsInApp = isApp
    ? qgStatuses
        .filter(({ caycStatus }) => caycStatus === CaycStatus.NonCompliant)
        .sort(({ name: a }, { name: b }) => a.localeCompare(b, undefined, { sensitivity: 'base' }))
    : [];

  const showIgnoredConditionWarning =
    component.qualifier === ComponentQualifier.Project &&
    qgStatuses.some((p) => Boolean(p.ignoredConditions));

  return (
    <div data-testid="overview__quality-gate-panel-conditions">
      <div>
        <Spinner loading={loading}>
          {showIgnoredConditionWarning && isNewCode && <IgnoredConditionWarning />}

          {isApp && (
            <>
              <TextError
                className="sw-mb-3"
                text={
                  <FormattedMessage
                    defaultMessage={translate('quality_gates.conditions.x_conditions_failed')}
                    id="quality_gates.conditions.x_conditions_failed"
                    values={{
                      conditions: totalFailedConditionLength,
                    }}
                  />
                }
              />
              <CardSeparator />
            </>
          )}

          {totalFailedCondition.length > 0 && (
            <div data-test="overview__quality-gate-conditions">
              {failedQgStatuses.map((qgStatus, qgStatusIdx) => {
                const failedConditionLength = qgStatus.failedConditions.filter((con) =>
                  isNewCode ? isDiffMetric(con.metric) : !isDiffMetric(con.metric),
                ).length;
                if (failedConditionLength > 0) {
                  return (
                    <QualityGatePanelSection
                      isApplication={isApp}
                      isLastStatus={qgStatusIdx === failedQgStatuses.length - 1}
                      key={qgStatus.key}
                      qgStatus={qgStatus}
                      qualityGate={qualityGate}
                      isNewCode={isNewCode}
                    />
                  );
                }
              })}
            </div>
          )}
        </Spinner>
      </div>

      {showCaycWarningInApp && <ApplicationNonCaycProjectWarning projects={nonCaycProjectsInApp} />}

      {showCaycWarningInProject && (
        <Card className="sw-mt-4 sw-body-sm">
          <CleanAsYouCodeWarning component={component} />
        </Card>
      )}

      <SonarLintPromotion qgConditions={qgStatuses?.flatMap((qg) => qg.failedConditions)} />
    </div>
  );
}

export default React.memo(QualityGatePanel);
