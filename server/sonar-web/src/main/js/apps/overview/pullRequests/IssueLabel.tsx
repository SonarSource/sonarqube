/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { Link } from 'react-router';
import * as classNames from 'classnames';
import { getMetricName, ISSUETYPE_MAP, IssueType } from '../utils';
import { getLeakValue } from '../../../components/measure/utils';
import { getBranchLikeQuery } from '../../../helpers/branches';
import { getComponentIssuesUrl } from '../../../helpers/urls';
import { formatMeasure, findMeasure } from '../../../helpers/measures';

interface Props {
  branchLike?: T.ShortLivingBranch | T.PullRequest;
  className?: string;
  component: T.Component;
  measures: T.Measure[];
  type: IssueType;
}

export default function IssueLabel({ branchLike, className, component, measures, type }: Props) {
  const { metric, iconClass } = ISSUETYPE_MAP[type];

  const measure = findMeasure(measures, metric);

  let value;
  if (measure) {
    value = getLeakValue(measure);
  }

  const params = {
    ...getBranchLikeQuery(branchLike),
    resolved: 'false',
    types: type
  };

  return (
    <>
      {value === undefined ? (
        <span className={classNames(className, 'measure-empty')}>—</span>
      ) : (
        <Link className={className} to={getComponentIssuesUrl(component.key, params)}>
          {formatMeasure(value, 'SHORT_INT')}
        </Link>
      )}
      {React.createElement(iconClass, { className: 'big-spacer-left little-spacer-right' })}
      {getMetricName(metric)}
    </>
  );
}
