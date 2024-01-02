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
import { ButtonPlain } from '../../../components/controls/buttons';
import ChevronDownIcon from '../../../components/icons/ChevronDownIcon';
import ChevronRightIcon from '../../../components/icons/ChevronRightIcon';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { isDiffMetric } from '../../../helpers/measures';
import { BranchLike } from '../../../types/branch-like';
import { isApplication } from '../../../types/component';
import {
  QualityGateStatus,
  QualityGateStatusConditionEnhanced,
} from '../../../types/quality-gates';
import { CaycStatus, Component } from '../../../types/types';
import QualityGateConditions from '../components/QualityGateConditions';
import CleanAsYouCodeWarning from './CleanAsYouCodeWarning';
import CleanAsYouCodeWarningOverCompliant from './CleanAsYouCodeWarningOverCompliant';

export interface QualityGatePanelSectionProps {
  branchLike?: BranchLike;
  component: Pick<Component, 'key' | 'qualifier' | 'qualityGate'>;
  qgStatus: QualityGateStatus;
}

function splitConditions(
  conditions: QualityGateStatusConditionEnhanced[]
): [QualityGateStatusConditionEnhanced[], QualityGateStatusConditionEnhanced[]] {
  const newCodeFailedConditions = [];
  const overallFailedConditions = [];

  for (const condition of conditions) {
    if (isDiffMetric(condition.metric)) {
      newCodeFailedConditions.push(condition);
    } else {
      overallFailedConditions.push(condition);
    }
  }

  return [newCodeFailedConditions, overallFailedConditions];
}

function displayConditions(conditions: number) {
  if (conditions === 0) {
    return null;
  }

  const text =
    conditions === 1
      ? translate('overview.1_condition_failed')
      : translateWithParameters('overview.X_conditions_failed', conditions);

  return <span className="text-muted big-spacer-left">{text}</span>;
}

export function QualityGatePanelSection(props: QualityGatePanelSectionProps) {
  const { component, qgStatus } = props;
  const [collapsed, setCollapsed] = React.useState(false);

  const toggle = React.useCallback(() => {
    setCollapsed(!collapsed);
  }, [collapsed]);

  /*
   * Show if project has failed conditions or that
   * it is a single non-cayc project
   * In the context of an App, only show projects with failed conditions
   */
  if (
    !(
      qgStatus.failedConditions.length > 0 ||
      (qgStatus.caycStatus !== CaycStatus.Compliant && !isApplication(component.qualifier))
    )
  ) {
    return null;
  }

  const [newCodeFailedConditions, overallFailedConditions] = splitConditions(
    qgStatus.failedConditions
  );

  const showName = isApplication(component.qualifier);

  const showSectionTitles =
    isApplication(component.qualifier) ||
    qgStatus.caycStatus !== CaycStatus.Compliant ||
    (overallFailedConditions.length > 0 && newCodeFailedConditions.length > 0);

  const toggleLabel = collapsed
    ? translateWithParameters('overview.quality_gate.show_project_conditions_x', qgStatus.name)
    : translateWithParameters('overview.quality_gate.hide_project_conditions_x', qgStatus.name);

  return (
    <div className="overview-quality-gate-conditions">
      {showName && (
        <ButtonPlain
          aria-label={toggleLabel}
          aria-expanded={!collapsed}
          className="width-100 text-left"
          onClick={toggle}
        >
          <div className="display-flex-center">
            <h3
              className="overview-quality-gate-conditions-project-name text-ellipsis"
              title={qgStatus.name}
            >
              {collapsed ? <ChevronRightIcon /> : <ChevronDownIcon />}
              <span className="spacer-left">{qgStatus.name}</span>
            </h3>
            {collapsed && displayConditions(qgStatus.failedConditions.length)}
          </div>
        </ButtonPlain>
      )}

      {!collapsed && (
        <>
          {qgStatus.caycStatus === CaycStatus.NonCompliant &&
            !isApplication(component.qualifier) && (
              <div className="big-padded bordered-bottom overview-quality-gate-conditions-list">
                <CleanAsYouCodeWarning component={component} />
              </div>
            )}

          {qgStatus.caycStatus === CaycStatus.OverCompliant &&
            !isApplication(component.qualifier) && (
              <div className="big-padded bordered-bottom overview-quality-gate-conditions-list">
                <CleanAsYouCodeWarningOverCompliant component={component} />
              </div>
            )}

          {newCodeFailedConditions.length > 0 && (
            <>
              {showSectionTitles && (
                <h4 className="big-padded overview-quality-gate-conditions-section-title">
                  {translateWithParameters(
                    'quality_gates.conditions.new_code_x',
                    newCodeFailedConditions.length.toString()
                  )}
                </h4>
              )}
              <QualityGateConditions
                component={qgStatus}
                branchLike={qgStatus.branchLike}
                failedConditions={newCodeFailedConditions}
              />
            </>
          )}

          {overallFailedConditions.length > 0 && (
            <>
              {showSectionTitles && (
                <h4 className="big-padded overview-quality-gate-conditions-section-title">
                  {translateWithParameters(
                    'quality_gates.conditions.overall_code_x',
                    overallFailedConditions.length.toString()
                  )}
                </h4>
              )}
              <QualityGateConditions
                component={qgStatus}
                branchLike={qgStatus.branchLike}
                failedConditions={overallFailedConditions}
              />
            </>
          )}
        </>
      )}
    </div>
  );
}

export default React.memo(QualityGatePanelSection);
