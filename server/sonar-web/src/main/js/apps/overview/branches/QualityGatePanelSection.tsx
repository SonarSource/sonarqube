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
import { translate } from 'sonar-ui-common/helpers/l10n';
import { isDiffMetric } from '../../../helpers/measures';
import { BranchLike } from '../../../types/branch-like';
import { ComponentQualifier } from '../../../types/component';
import { QualityGateStatus } from '../../../types/quality-gates';
import { QualityGateConditions } from '../components/QualityGateConditions';

export interface QualityGatePanelSectionProps {
  branchLike?: BranchLike;
  component: Pick<T.Component, 'key' | 'qualifier'>;
  qgStatus: QualityGateStatus;
}

export function QualityGatePanelSection(props: QualityGatePanelSectionProps) {
  const { branchLike, component, qgStatus } = props;
  const newCodeFailedConditions = qgStatus.failedConditions.filter(c => isDiffMetric(c.metric));
  const overallFailedConditions = qgStatus.failedConditions.filter(c => !isDiffMetric(c.metric));

  if (newCodeFailedConditions.length === 0 && overallFailedConditions.length === 0) {
    return null;
  }

  const showName = component.qualifier === ComponentQualifier.Application;

  return (
    <div className="overview-quality-gate-conditions">
      {showName && (
        <h3 className="overview-quality-gate-conditions-project-name">{qgStatus.name}</h3>
      )}

      {newCodeFailedConditions.length > 0 && (
        <>
          <h4 className="overview-quality-gate-conditions-section-title">
            {translate('quality_gates.conditions.new_code')}
          </h4>
          <QualityGateConditions
            branchLike={branchLike}
            component={qgStatus}
            failedConditions={newCodeFailedConditions}
          />
        </>
      )}

      {overallFailedConditions.length > 0 && (
        <>
          <h4 className="overview-quality-gate-conditions-section-title">
            {translate('quality_gates.conditions.overall_code')}
          </h4>
          <QualityGateConditions
            branchLike={branchLike}
            component={qgStatus}
            failedConditions={overallFailedConditions}
          />
        </>
      )}
    </div>
  );
}

export default React.memo(QualityGatePanelSection);
