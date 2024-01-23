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
import { BasicSeparator, Card, Spinner } from 'design-system';
import * as React from 'react';
import { ComponentQualifier, isApplication } from '../../../types/component';
import { QualityGateStatus } from '../../../types/quality-gates';
import { CaycStatus, Component, QualityGate } from '../../../types/types';
import IgnoredConditionWarning from '../components/IgnoredConditionWarning';
import QualityGateStatusHeader from '../components/QualityGateStatusHeader';
import QualityGateStatusPassedView from '../components/QualityGateStatusPassedView';
import { QualityGateStatusTitle } from '../components/QualityGateStatusTitle';
import ApplicationNonCaycProjectWarning from './ApplicationNonCaycProjectWarning';
import CleanAsYouCodeWarning from './CleanAsYouCodeWarning';
import QualityGatePanelSection from './QualityGatePanelSection';

export interface QualityGatePanelProps {
  component: Pick<Component, 'key' | 'qualifier' | 'qualityGate'>;
  loading?: boolean;
  qgStatuses?: QualityGateStatus[];
  qualityGate?: QualityGate;
}

export function QualityGatePanel(props: QualityGatePanelProps) {
  const { component, loading, qgStatuses = [], qualityGate } = props;

  if (qgStatuses === undefined) {
    return null;
  }

  const overallLevel = qgStatuses.map((s) => s.status).includes('ERROR') ? 'ERROR' : 'OK';
  const success = overallLevel === 'OK';

  const failedQgStatuses = qgStatuses.filter((qgStatus) => qgStatus.failedConditions.length > 0);

  const overallFailedConditionsCount = qgStatuses.reduce(
    (acc, qgStatus) => acc + qgStatus.failedConditions.length,
    0,
  );

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
    <div data-test="overview__quality-gate-panel">
      <QualityGateStatusTitle />
      <div className="sw-pt-5">
        <Spinner loading={loading}>
          <QualityGateStatusHeader
            status={overallLevel}
            failedConditionCount={overallFailedConditionsCount}
          />
          {success && <QualityGateStatusPassedView />}

          {showIgnoredConditionWarning && <IgnoredConditionWarning />}

          {!success && <BasicSeparator />}

          {overallFailedConditionsCount > 0 && (
            <div data-test="overview__quality-gate-conditions">
              {failedQgStatuses.map((qgStatus, qgStatusIdx) => (
                <QualityGatePanelSection
                  isApplication={isApp}
                  isLastStatus={qgStatusIdx === failedQgStatuses.length - 1}
                  key={qgStatus.key}
                  qgStatus={qgStatus}
                  qualityGate={qualityGate}
                />
              ))}
            </div>
          )}
        </Spinner>
      </div>

      {nonCaycProjectsInApp.length > 0 && (
        <ApplicationNonCaycProjectWarning projects={nonCaycProjectsInApp} />
      )}

      {qgStatuses.length === 1 &&
        qgStatuses[0].caycStatus === CaycStatus.NonCompliant &&
        qualityGate?.actions?.manageConditions &&
        !isApp && (
          <Card className="sw-mt-4 sw-body-sm">
            <CleanAsYouCodeWarning component={component} />
          </Card>
        )}
    </div>
  );
}

export default React.memo(QualityGatePanel);
