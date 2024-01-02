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
import { DrilldownLink, HelperHintIcon, LightLabel } from 'design-system';
import * as React from 'react';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import Tooltip from '../../../components/controls/Tooltip';
import { getLeakValue } from '../../../components/measure/utils';
import { DEFAULT_ISSUES_QUERY } from '../../../components/shared/utils';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { findMeasure, formatMeasure, localizeMetric } from '../../../helpers/measures';
import { getComponentIssuesUrl, getComponentSecurityHotspotsUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { ComponentQualifier } from '../../../types/component';
import { IssueType } from '../../../types/issues';
import { MetricType } from '../../../types/metrics';
import { Component, MeasureEnhanced } from '../../../types/types';
import { getIssueMetricKey } from '../utils';
import { OverviewDisabledLinkTooltip } from './OverviewDisabledLinkTooltip';

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
    inNewCodePeriod: useDiffMetric ? 'true' : 'false',
    ...DEFAULT_ISSUES_QUERY,
    types: type,
  };

  const url =
    type === IssueType.SecurityHotspot
      ? getComponentSecurityHotspotsUrl(component.key, params)
      : getComponentIssuesUrl(component.key, params);

  const disabled =
    component.qualifier === ComponentQualifier.Application && component.needIssueSync;

  const drilldownLinkProps = disabled
    ? { disabled, to: '' }
    : {
        'aria-label': translateWithParameters(
          'overview.see_list_of_x_y_issues',
          value as string,
          localizeMetric(metricKey),
        ),
        to: url,
      };

  return (
    <div className="sw-body-md sw-flex sw-items-center">
      {value === undefined ? (
        <LightLabel aria-label={translate('no_data')}> — </LightLabel>
      ) : (
        <Tooltip
          classNameSpace={disabled ? 'tooltip' : 'sw-hidden'}
          overlay={<OverviewDisabledLinkTooltip />}
        >
          <DrilldownLink className="it__overview-measures-value" {...drilldownLinkProps}>
            {formatMeasure(value, MetricType.ShortInteger)}
          </DrilldownLink>
        </Tooltip>
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
