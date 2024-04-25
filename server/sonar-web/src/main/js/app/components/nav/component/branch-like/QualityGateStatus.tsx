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
import classNames from 'classnames';
import { QualityGateIndicator } from 'design-system';
import React from 'react';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { MetricType } from '~sonar-aligned/types/metrics';
import { translateWithParameters } from '../../../../../helpers/l10n';
import { BranchLike } from '../../../../../types/branch-like';

interface Props {
  branchLike: BranchLike;
  className: string;
  showStatusText?: boolean;
}

export default function QualityGateStatus({ className, showStatusText, branchLike }: Props) {
  // eslint-disable-next-line @typescript-eslint/prefer-optional-chain, @typescript-eslint/no-unnecessary-condition
  if (!branchLike.status?.qualityGateStatus) {
    return null;
  }

  const formatted = formatMeasure(branchLike.status?.qualityGateStatus, MetricType.Level);
  const ariaLabel = translateWithParameters('overview.quality_gate_x', formatted);
  return (
    <div className={classNames(`it__level-${branchLike.status.qualityGateStatus}`, className)}>
      <QualityGateIndicator
        status={branchLike.status?.qualityGateStatus}
        className="sw-mr-2"
        ariaLabel={ariaLabel}
        size="sm"
      />
      {showStatusText && <span>{formatted}</span>}
    </div>
  );
}
