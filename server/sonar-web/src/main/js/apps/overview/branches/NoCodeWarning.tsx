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
import { FlagMessage } from 'design-system';
import * as React from 'react';
import { isMainBranch } from '~sonar-aligned/helpers/branch-like';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { MetricKey } from '~sonar-aligned/types/metrics';
import { getBranchLikeDisplayName } from '../../../helpers/branch-like';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { Component, MeasureEnhanced } from '../../../types/types';

interface Props {
  branchLike?: BranchLike;
  component: Component;
  measures?: MeasureEnhanced[];
}

export function NoCodeWarning({ branchLike, component, measures }: Props) {
  const isApp = component.qualifier === ComponentQualifier.Application;

  /* eslint-disable no-lonely-if */
  // - Is App
  //     - No measures, OR measures, but no projects => empty
  //     - Else => no lines of code
  // - Else
  //   - No measures => empty
  //       - Main branch?
  //       - LLB?
  //       - No branch info?
  //   - Measures, but no ncloc (checked in isEmpty()) => no lines of code
  //       - Main branch?
  //       - LLB?
  //       - No branch info?
  let title = translate('overview.project.no_lines_of_code');
  if (isApp) {
    if (
      measures === undefined ||
      measures.find((measure) => measure.metric.key === MetricKey.projects) === undefined
    ) {
      title = translate('portfolio.app.empty');
    } else {
      title = translate('portfolio.app.no_lines_of_code');
    }
  } else {
    if (measures === undefined || measures.length === 0) {
      if (isMainBranch(branchLike)) {
        title = translate('overview.project.main_branch_empty');
      } else if (branchLike !== undefined) {
        title = translateWithParameters(
          'overview.project.branch_X_empty',
          getBranchLikeDisplayName(branchLike),
        );
      } else {
        title = translate('overview.project.empty');
      }
    } else {
      if (isMainBranch(branchLike)) {
        title = translate('overview.project.main_branch_no_lines_of_code');
      } else if (branchLike !== undefined) {
        title = translateWithParameters(
          'overview.project.branch_X_no_lines_of_code',
          getBranchLikeDisplayName(branchLike),
        );
      }
    }
  }
  /* eslint-enable no-lonely-if */

  return <FlagMessage variant="warning">{title}</FlagMessage>;
}

export default React.memo(NoCodeWarning);
