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
import Link from '../../../components/common/Link';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import { getLeakValue } from '../../../components/measure/utils';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { findMeasure, formatMeasure, localizeMetric } from '../../../helpers/measures';
import { getComponentIssuesUrl, getComponentSecurityHotspotsUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { IssueType } from '../../../types/issues';
import { Component, MeasureEnhanced } from '../../../types/types';
import { getIssueIconClass, getIssueMetricKey } from '../utils';

export interface IssueLabelProps {
  branchLike?: BranchLike;
  component: Component;
  helpTooltip?: string;
  measures: MeasureEnhanced[];
  type: IssueType;
  useDiffMetric?: boolean;
}

export function IssueLabel(props: IssueLabelProps) {
  const { branchLike, component, helpTooltip, measures, type, useDiffMetric = false } = props;
  const metricKey = getIssueMetricKey(type, useDiffMetric);
  const measure = findMeasure(measures, metricKey);
  const iconClass = getIssueIconClass(type);

  let value;
  if (measure) {
    value = useDiffMetric ? getLeakValue(measure) : measure.value;
  }

  const params = {
    ...getBranchLikeQuery(branchLike),
    resolved: 'false',
    types: type,
    inNewCodePeriod: useDiffMetric ? 'true' : 'false',
  };

  const url =
    type === IssueType.SecurityHotspot
      ? getComponentSecurityHotspotsUrl(component.key, params)
      : getComponentIssuesUrl(component.key, params);

  return (
    <>
      {value === undefined ? (
        <span aria-label={translate('no_data')} className="overview-measures-empty-value" />
      ) : (
        <Link
          aria-label={translateWithParameters(
            'overview.see_list_of_x_y_issues',
            value,
            localizeMetric(metricKey)
          )}
          className="overview-measures-value text-light"
          to={url}
        >
          {formatMeasure(value, 'SHORT_INT')}
        </Link>
      )}
      {React.createElement(iconClass, { className: 'big-spacer-left little-spacer-right' })}
      {localizeMetric(metricKey)}
      {helpTooltip && <HelpTooltip className="little-spacer-left" overlay={helpTooltip} />}
    </>
  );
}

export default React.memo(IssueLabel);
