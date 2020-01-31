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
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import Rating from 'sonar-ui-common/components/ui/Rating';
import { getLeakValue, getRatingTooltip } from '../../../components/measure/utils';
import DrilldownLink from '../../../components/shared/DrilldownLink';
import { findMeasure } from '../../../helpers/measures';
import { BranchLike } from '../../../types/branch-like';
import { getIssueRatingMetricKey, getIssueRatingName, IssueType } from '../utils';

export interface IssueRatingProps {
  branchLike?: BranchLike;
  component: T.Component;
  measures: T.MeasureEnhanced[];
  type: IssueType;
  useDiffMetric?: boolean;
}

function renderRatingLink(props: IssueRatingProps) {
  const { branchLike, component, useDiffMetric = false, measures, type } = props;
  const rating = getIssueRatingMetricKey(type, useDiffMetric);
  const measure = findMeasure(measures, rating);

  if (!rating || !measure) {
    return (
      <div className="padded">
        <Rating value={undefined} />
      </div>
    );
  }

  const value = measure && (useDiffMetric ? getLeakValue(measure) : measure.value);
  const tooltip = value && getRatingTooltip(rating, Number(value));

  return (
    <Tooltip overlay={tooltip}>
      <span>
        <DrilldownLink
          branchLike={branchLike}
          className="link-no-underline"
          component={component.key}
          metric={rating}>
          <Rating value={value} />
        </DrilldownLink>
      </span>
    </Tooltip>
  );
}

export function IssueRating(props: IssueRatingProps) {
  const { type } = props;

  return (
    <>
      <span className="flex-1 big-spacer-right text-right">{getIssueRatingName(type)}</span>
      {renderRatingLink(props)}
    </>
  );
}

export default React.memo(IssueRating);
