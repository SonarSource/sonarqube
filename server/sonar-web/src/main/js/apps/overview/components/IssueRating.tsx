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
/* eslint-disable react/no-unused-prop-types */

import { DiscreetLinkBox, MetricsRatingBadge } from 'design-system';
import * as React from 'react';
import Tooltip from '../../../components/controls/Tooltip';
import RatingTooltipContent from '../../../components/measure/RatingTooltipContent';
import { getLeakValue } from '../../../components/measure/utils';
import { translateWithParameters } from '../../../helpers/l10n';
import { findMeasure, formatRating } from '../../../helpers/measures';
import { getComponentDrilldownUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { IssueType } from '../../../types/issues';
import { Component, MeasureEnhanced } from '../../../types/types';
import { getIssueRatingMetricKey } from '../utils';

export interface IssueRatingProps {
  branchLike?: BranchLike;
  component: Component;
  measures: MeasureEnhanced[];
  type: IssueType;
  useDiffMetric?: boolean;
}

export function IssueRating(props: IssueRatingProps) {
  const { branchLike, component, useDiffMetric = false, measures, type } = props;
  const ratingKey = getIssueRatingMetricKey(type, useDiffMetric);
  const measure = findMeasure(measures, ratingKey);
  const rawValue = measure && (useDiffMetric ? getLeakValue(measure) : measure.value);
  const value = formatRating(rawValue);

  if (!ratingKey || !measure) {
    return <NoRating />;
  }

  return (
    <Tooltip overlay={rawValue && <RatingTooltipContent metricKey={ratingKey} value={rawValue} />}>
      <span>
        {value ? (
          <DiscreetLinkBox
            to={getComponentDrilldownUrl({
              branchLike,
              componentKey: component.key,
              metric: ratingKey,
              listView: true,
            })}
          >
            <MetricsRatingBadge
              label={translateWithParameters('metric.has_rating_X', value)}
              rating={value}
              size="md"
            />
          </DiscreetLinkBox>
        ) : (
          <NoRating />
        )}
      </span>
    </Tooltip>
  );
}

export default IssueRating;

function NoRating() {
  return <div className="sw-w-8 sw-h-8 sw-flex sw-justify-center sw-items-center">–</div>;
}
