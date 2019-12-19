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
import * as classNames from 'classnames';
import * as React from 'react';
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import DocTooltip from '../../../components/docs/DocTooltip';
import { BranchLike } from '../../../types/branch-like';
import { QualityGateStatus } from '../../../types/quality-gates';
import QualityGatePanelSection from './QualityGatePanelSection';

export interface QualityGatePanelProps {
  branchLike?: BranchLike;
  component: Pick<T.Component, 'key' | 'qualifier'>;
  loading?: boolean;
  qgStatuses?: QualityGateStatus[];
}

export function QualityGatePanel(props: QualityGatePanelProps) {
  const { branchLike, component, loading, qgStatuses = [] } = props;

  if (qgStatuses === undefined) {
    return null;
  }

  const overallLevel = qgStatuses.map(s => s.status).includes('ERROR') ? 'ERROR' : 'OK';
  const success = overallLevel === 'OK';

  const overallFailedConditionsCount = qgStatuses.reduce(
    (acc, qgStatus) => acc + qgStatus.failedConditions.length,
    0
  );

  const showIgnoredConditionWarning =
    component.qualifier === 'TRK' &&
    qgStatuses !== undefined &&
    qgStatuses.some(p => Boolean(p.ignoredConditions));

  return (
    <div className="overview-panel" data-test="overview__quality-gate-panel">
      <h2 className="overview-panel-title display-inline-flex-center">
        {translate('overview.quality_gate')}{' '}
        <DocTooltip
          className="little-spacer-left"
          doc={import(
            /* webpackMode: "eager" */ 'Docs/tooltips/quality-gates/project-homepage-quality-gate.md'
          )}
        />
      </h2>

      {showIgnoredConditionWarning && (
        <Alert className="big-spacer-bottom" display="inline" variant="info">
          <span className="text-middle">
            {translate('overview.quality_gate.ignored_conditions')}
          </span>
          <HelpTooltip
            className="spacer-left"
            overlay={translate('overview.quality_gate.ignored_conditions.tooltip')}
          />
        </Alert>
      )}

      <div className="overview-panel-content">
        {loading ? (
          <div className="overview-panel-big-padded">
            <DeferredSpinner loading={loading} />
          </div>
        ) : (
          <>
            <div
              className={classNames('overview-quality-gate-badge-large', {
                failed: !success,
                success
              })}>
              <h3 className="big-spacer-bottom huge">{translate('metric.level', overallLevel)}</h3>

              <span className="small">
                {overallFailedConditionsCount > 0
                  ? translateWithParameters(
                      'overview.X_conditions_failed',
                      overallFailedConditionsCount
                    )
                  : translate('overview.quality_gate_all_conditions_passed')}
              </span>
            </div>

            {overallFailedConditionsCount > 0 && (
              <div data-test="overview__quality-gate-conditions">
                {qgStatuses.map(qgStatus => (
                  <QualityGatePanelSection
                    branchLike={branchLike}
                    component={component}
                    key={qgStatus.key}
                    qgStatus={qgStatus}
                  />
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

export default React.memo(QualityGatePanel);
