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
import { getBranchLikeQuery } from '~sonar-aligned/helpers/branch-like';
import { getComponentIssuesUrl } from '~sonar-aligned/helpers/urls';
import { getComponentDrilldownUrl } from '../../helpers/urls';
import { BranchLike } from '../../types/branch-like';
import Link from '../common/Link';
import { isIssueMeasure, propsToIssueParams } from './utils';

interface Props {
  ariaLabel?: string;
  branchLike?: BranchLike;
  children?: React.ReactNode;
  className?: string;
  component: string;
  inNewCodePeriod?: boolean;
  metric: string;
}

export default class DrilldownLink extends React.PureComponent<Props> {
  renderIssuesLink = () => {
    const { ariaLabel, className, component, children, branchLike, metric, inNewCodePeriod } =
      this.props;

    const url = getComponentIssuesUrl(component, {
      ...propsToIssueParams(metric, inNewCodePeriod),
      ...getBranchLikeQuery(branchLike),
    });

    return (
      <Link aria-label={ariaLabel} className={className} to={url}>
        {children}
      </Link>
    );
  };

  render() {
    const { ariaLabel, className, metric, component, children, branchLike } = this.props;

    if (isIssueMeasure(metric)) {
      return this.renderIssuesLink();
    }

    const url = getComponentDrilldownUrl({
      componentKey: component,
      metric,
      branchLike,
      listView: true,
    });

    return (
      <Link aria-label={ariaLabel} className={className} to={url}>
        {children}
      </Link>
    );
  }
}
