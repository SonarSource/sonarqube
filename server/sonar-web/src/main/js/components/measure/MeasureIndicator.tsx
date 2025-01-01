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

import { CoverageIndicator, DuplicationsIndicator } from '~design-system';
import Measure from '~sonar-aligned/components/measure/Measure';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import { BranchLike } from '../../types/branch-like';
import { duplicationRatingConverter } from './utils';

interface Props {
  branchLike?: BranchLike;
  className?: string;
  componentKey: string;
  decimals?: number;
  forceRatingMetric?: boolean;
  metricKey: string;
  metricType: string;
  small?: boolean;
  value: string | undefined;
}

export default function MeasureIndicator(props: Props) {
  const { className, metricKey, metricType, value } = props;

  if (
    metricType === MetricType.Percent &&
    (metricKey === MetricKey.duplicated_lines_density ||
      metricKey === MetricKey.new_duplicated_lines_density)
  ) {
    return (
      <div className={className}>
        <DuplicationsIndicator rating={duplicationRatingConverter(Number(value))} />
      </div>
    );
  }

  if (metricType === MetricType.Percent) {
    return (
      <div className={className}>
        <CoverageIndicator value={value} />
      </div>
    );
  }

  return <Measure {...props} badgeSize="sm" />;
}
