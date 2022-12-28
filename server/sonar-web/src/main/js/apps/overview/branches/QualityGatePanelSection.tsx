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
import * as React from 'react';
import { ButtonPlain } from '../../../components/controls/buttons';
import ChevronDownIcon from '../../../components/icons/ChevronDownIcon';
import ChevronRightIcon from '../../../components/icons/ChevronRightIcon';
import { Alert } from '../../../components/ui/Alert';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { isDiffMetric } from '../../../helpers/measures';
import { BranchLike } from '../../../types/branch-like';
import { ComponentQualifier } from '../../../types/component';
import {
  QualityGateStatus,
  QualityGateStatusConditionEnhanced,
} from '../../../types/quality-gates';
import { Component } from '../../../types/types';
import QualityGateConditions from '../components/QualityGateConditions';
import { CAYC_METRICS } from '../utils';
import CleanAsYouCodeWarning from './CleanAsYouCodeWarning';

export interface QualityGatePanelSectionProps {
  branchLike?: BranchLike;
  component: Pick<Component, 'key' | 'qualifier'>;
  qgStatus: QualityGateStatus;
}

function splitConditions(conditions: QualityGateStatusConditionEnhanced[]) {
  const caycConditions = [];
  const newCodeFailedConditions = [];
  const overallFailedConditions = [];

  for (const condition of conditions) {
    if (CAYC_METRICS.includes(condition.metric)) {
      caycConditions.push(condition);
    } else if (isDiffMetric(condition.metric)) {
      newCodeFailedConditions.push(condition);
    } else {
      overallFailedConditions.push(condition);
    }
  }

  return [caycConditions, newCodeFailedConditions, overallFailedConditions];
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

  if (qgStatus.failedConditions.length === 0 && qgStatus.isCaycCompliant) {
    return null;
  }

  const [caycConditions, newCodeFailedConditions, overallFailedConditions] = splitConditions(
    qgStatus.failedConditions
  );

  /*
   * Show Clean as You Code if:
   * - The QG is not CAYC-compliant
   * - There are *any* failing conditions, we either show:
   *   - the cayc-specific failures
   *   - that cayc is passing and only other conditions are failing
   */
  const showCayc = !qgStatus.isCaycCompliant || qgStatus.failedConditions.length > 0;

  const showSuccessfulCayc = caycConditions.length === 0 && qgStatus.isCaycCompliant;

  const hasOtherConditions = newCodeFailedConditions.length + overallFailedConditions.length > 0;

  const showName = component.qualifier === ComponentQualifier.Application;

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
          {showCayc && (
            <>
              <div className="display-flex-center overview-quality-gate-conditions-section-title">
                <h4 className="padded">{translate('quality_gates.conditions.cayc')}</h4>
                {displayConditions(caycConditions.length)}
              </div>

              {!qgStatus.isCaycCompliant && (
                <div className="big-padded bordered-bottom overview-quality-gate-conditions-list">
                  <CleanAsYouCodeWarning />
                </div>
              )}

              {showSuccessfulCayc && (
                <div className="big-padded bordered-bottom overview-quality-gate-conditions-list">
                  <Alert variant="success" className="no-margin-bottom">
                    {translate('overview.quality_gate.conditions.cayc.passed')}
                  </Alert>
                </div>
              )}

              {caycConditions.length > 0 && (
                <QualityGateConditions
                  component={qgStatus}
                  branchLike={qgStatus.branchLike}
                  failedConditions={caycConditions}
                />
              )}
            </>
          )}

          {hasOtherConditions && (
            <>
              <div className="display-flex-center overview-quality-gate-conditions-section-title">
                <h4 className="padded">{translate('quality_gates.conditions.other_conditions')}</h4>
                {displayConditions(newCodeFailedConditions.length + overallFailedConditions.length)}
              </div>

              {newCodeFailedConditions.length > 0 && (
                <>
                  <h5 className="big-padded overview-quality-gate-conditions-subsection-title">
                    {translate('quality_gates.conditions.new_code')}
                  </h5>
                  <QualityGateConditions
                    component={qgStatus}
                    branchLike={qgStatus.branchLike}
                    failedConditions={newCodeFailedConditions}
                  />
                </>
              )}

              {overallFailedConditions.length > 0 && (
                <>
                  <h5 className="big-padded overview-quality-gate-conditions-subsection-title">
                    {translate('quality_gates.conditions.overall_code')}
                  </h5>
                  <QualityGateConditions
                    component={qgStatus}
                    branchLike={qgStatus.branchLike}
                    failedConditions={overallFailedConditions}
                  />
                </>
              )}
            </>
          )}
        </>
      )}
    </div>
  );
}

export default React.memo(QualityGatePanelSection);
