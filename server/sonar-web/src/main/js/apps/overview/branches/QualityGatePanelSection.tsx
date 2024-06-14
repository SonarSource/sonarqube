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
import { BorderlessAccordion, CardSeparator } from 'design-system';
import * as React from 'react';
import { translateWithParameters } from '../../../helpers/l10n';
import { QualityGateStatus } from '../../../types/quality-gates';
import { QualityGate } from '../../../types/types';
import FailedConditions from './FailedConditions';

export interface QualityGatePanelSectionProps {
  isApplication?: boolean;
  isLastStatus?: boolean;
  isNewCode: boolean;
  qgStatus: QualityGateStatus;
  qualityGate?: QualityGate;
}

export function QualityGatePanelSection(props: QualityGatePanelSectionProps) {
  const { isApplication, isLastStatus, qgStatus, qualityGate, isNewCode } = props;
  const [collapsed, setCollapsed] = React.useState(false);

  const toggle = React.useCallback(() => {
    setCollapsed(!collapsed);
  }, [collapsed]);

  const toggleLabel = collapsed
    ? translateWithParameters('overview.quality_gate.show_project_conditions_x', qgStatus.name)
    : translateWithParameters('overview.quality_gate.hide_project_conditions_x', qgStatus.name);

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
              </div>
            }
          >
            <CardSeparator />

            <FailedConditions
              isNewCode={isNewCode}
              isApplication={isApplication}
              qualityGate={qualityGate}
              failedConditions={qgStatus.failedConditions}
              branchLike={qgStatus.branchLike}
              component={qgStatus}
            />
          </BorderlessAccordion>

          {(!isLastStatus || collapsed) && <CardSeparator />}
        </>
      ) : (
        <FailedConditions
          isNewCode={isNewCode}
          isApplication={isApplication}
          qualityGate={qualityGate}
          failedConditions={qgStatus.failedConditions}
          branchLike={qgStatus.branchLike}
          component={qgStatus}
        />
      )}
    </>
  );
}

export default React.memo(QualityGatePanelSection);
