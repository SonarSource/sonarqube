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
import { Link } from 'react-router';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { formatMeasure, localizeMetric } from 'sonar-ui-common/helpers/measures';
import DocTooltip from '../../../components/docs/DocTooltip';
import { getLeakValue } from '../../../components/measure/utils';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { findMeasure } from '../../../helpers/measures';
import { getComponentIssuesUrl, getComponentSecurityHotspotsUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { getIssueIconClass, getIssueMetricKey, IssueType } from '../utils';

export interface IssueLabelProps {
  branchLike?: BranchLike;
  component: T.Component;
  docTooltip?: Promise<{ default: string }>;
  measures: T.MeasureEnhanced[];
  type: IssueType;
  useDiffMetric?: boolean;
}

export function IssueLabel(props: IssueLabelProps) {
  const { branchLike, component, docTooltip, measures, type, useDiffMetric = false } = props;
  const metric = getIssueMetricKey(type, useDiffMetric);
  const measure = findMeasure(measures, metric);
  const iconClass = getIssueIconClass(type);

  let value;
  if (measure) {
    value = useDiffMetric ? getLeakValue(measure) : measure.value;
  }

  const params = {
    ...getBranchLikeQuery(branchLike),
    resolved: 'false',
    types: type,
    sinceLeakPeriod: useDiffMetric ? 'true' : 'false'
  };

  return (
    <>
      {value === undefined ? (
        <span aria-label={translate('no_data')} className="overview-measures-empty-value" />
      ) : (
        <Link
          className="overview-measures-value text-light"
          to={
            type === IssueType.SecurityHotspot
              ? getComponentSecurityHotspotsUrl(component.key, params)
              : getComponentIssuesUrl(component.key, params)
          }>
          {formatMeasure(value, 'SHORT_INT')}
        </Link>
      )}
      {React.createElement(iconClass, { className: 'big-spacer-left little-spacer-right' })}
      {localizeMetric(metric)}
      {docTooltip && <DocTooltip className="little-spacer-left" doc={docTooltip} />}
    </>
  );
}

export default React.memo(IssueLabel);
