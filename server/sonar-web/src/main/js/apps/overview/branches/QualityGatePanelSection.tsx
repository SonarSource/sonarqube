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

import { BasicSeparator, BorderlessAccordion, TextMuted } from 'design-system';
import * as React from 'react';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { isDiffMetric } from '../../../helpers/measures';
import { BranchLike } from '../../../types/branch-like';
import {
  QualityGateStatus,
  QualityGateStatusConditionEnhanced,
} from '../../../types/quality-gates';
import { QualityGate } from '../../../types/types';
import QualityGateConditions from '../components/QualityGateConditions';
import ZeroNewIssuesSimplificationGuide from '../components/ZeroNewIssuesSimplificationGuide';

export interface QualityGatePanelSectionProps {
  branchLike?: BranchLike;
  isApplication?: boolean;
  isLastStatus?: boolean;
  qgStatus: QualityGateStatus;
  qualityGate?: QualityGate;
}

function splitConditions(
  conditions: QualityGateStatusConditionEnhanced[],
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

export function QualityGatePanelSection(props: QualityGatePanelSectionProps) {
  const { isApplication, isLastStatus, qgStatus, qualityGate } = props;
  const [collapsed, setCollapsed] = React.useState(false);

  const toggle = React.useCallback(() => {
    setCollapsed(!collapsed);
  }, [collapsed]);

  const [newCodeFailedConditions, overallFailedConditions] = splitConditions(
    qgStatus.failedConditions,
  );

  const showSectionTitles =
    isApplication || (overallFailedConditions.length > 0 && newCodeFailedConditions.length > 0);

  const toggleLabel = collapsed
    ? translateWithParameters('overview.quality_gate.show_project_conditions_x', qgStatus.name)
    : translateWithParameters('overview.quality_gate.hide_project_conditions_x', qgStatus.name);

  const newCodeText =
    newCodeFailedConditions.length === 1
      ? translate('quality_gates.conditions.new_code_1')
      : translateWithParameters(
          'quality_gates.conditions.new_code_x',
          newCodeFailedConditions.length.toString(),
        );

  const overallText =
    overallFailedConditions.length === 1
      ? translate('quality_gates.conditions.overall_code_1')
      : translateWithParameters(
          'quality_gates.conditions.overall_code_x',
          overallFailedConditions.length.toString(),
        );

  const renderFailedConditions = () => {
    return (
      <>
        {newCodeFailedConditions.length > 0 && (
          <>
            {showSectionTitles && (
              <>
                <p className="sw-px-2 sw-py-3">{newCodeText}</p>

                <BasicSeparator />
              </>
            )}

            {qualityGate?.isBuiltIn && (
              <ZeroNewIssuesSimplificationGuide qualityGate={qualityGate} />
            )}
            <QualityGateConditions
              component={qgStatus}
              branchLike={qgStatus.branchLike}
              failedConditions={newCodeFailedConditions}
              isBuiltInQualityGate={qualityGate?.isBuiltIn}
            />
          </>
        )}

        {overallFailedConditions.length > 0 && (
          <>
            {showSectionTitles && (
              <>
                <p className="sw-px-2 sw-py-3">{overallText}</p>

                <BasicSeparator />
              </>
            )}

            <QualityGateConditions
              component={qgStatus}
              branchLike={qgStatus.branchLike}
              failedConditions={overallFailedConditions}
            />
          </>
        )}
      </>
    );
  };

  return (
    <>
      {isApplication ? (
        <>
          <BorderlessAccordion
            ariaLabel={toggleLabel}
            onClick={toggle}
            open={!collapsed}
            header={
              <div className="sw-flex sw-flex-col sw-text-sm">
                <span className="sw-body-sm-highlight">{qgStatus.name}</span>

                {collapsed && newCodeFailedConditions.length > 0 && (
                  <TextMuted text={newCodeText} />
                )}

                {collapsed && overallFailedConditions.length > 0 && (
                  <TextMuted text={overallText} />
                )}
              </div>
            }
          >
            <BasicSeparator />

            {renderFailedConditions()}
          </BorderlessAccordion>

          {(!isLastStatus || collapsed) && <BasicSeparator />}
        </>
      ) : (
        renderFailedConditions()
      )}
    </>
  );
}

export default React.memo(QualityGatePanelSection);
