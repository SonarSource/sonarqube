/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { DrilldownLink, HelperHintIcon, LightLabel } from 'design-system';
import * as React from 'react';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import { getLeakValue } from '../../../components/measure/utils';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { findMeasure, formatMeasure, localizeMetric } from '../../../helpers/measures';
import { getComponentIssuesUrl, getComponentSecurityHotspotsUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { IssueType } from '../../../types/issues';
import { MetricType } from '../../../types/metrics';
import { Component, MeasureEnhanced } from '../../../types/types';
import { getIssueMetricKey } from '../utils';

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
    <div className="sw-body-md sw-flex sw-items-center">
      {value === undefined ? (
        <LightLabel aria-label={translate('no_data')}> — </LightLabel>
      ) : (
        <DrilldownLink
          aria-label={translateWithParameters(
            'overview.see_list_of_x_y_issues',
            value,
            localizeMetric(metricKey)
          )}
          className="it__overview-measures-value"
          to={url}
        >
          {formatMeasure(value, MetricType.ShortInteger)}
        </DrilldownLink>
      )}
      <LightLabel className="sw-mx-2">{localizeMetric(metricKey)}</LightLabel>
      {helpTooltip && (
        <HelpTooltip overlay={helpTooltip}>
          <HelperHintIcon aria-label={helpTooltip} />
        </HelpTooltip>
      )}
    </div>
  );
}

export default React.memo(IssueLabel);
