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
import { DrilldownLink, LightLabel } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { getLeakValue } from '../../../components/measure/utils';
import { translate } from '../../../helpers/l10n';
import { findMeasure, formatMeasure } from '../../../helpers/measures';
import { getComponentDrilldownUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { MetricType } from '../../../types/metrics';
import { Component, MeasureEnhanced } from '../../../types/types';
import { MeasurementType, getMeasurementLabelKeys, getMeasurementLinesMetricKey } from '../utils';

interface Props {
  branchLike?: BranchLike;
  component: Component;
  useDiffMetric: boolean;
  measures: MeasureEnhanced[];
  type: MeasurementType;
}

export default function MeasuresPanelPercentMeasureLabel(props: Props) {
  const { branchLike, component, measures, type, useDiffMetric = false } = props;
  const { expandedLabelKey, labelKey } = getMeasurementLabelKeys(type, useDiffMetric);
  const linesMetric = getMeasurementLinesMetricKey(type, useDiffMetric);
  const measure = findMeasure(measures, linesMetric);

  if (!measure) {
    return <LightLabel>{translate(labelKey)}</LightLabel>;
  }

  const value = useDiffMetric ? getLeakValue(measure) : measure.value;

  const url = getComponentDrilldownUrl({
    componentKey: component.key,
    metric: linesMetric,
    branchLike,
    listView: true,
  });

  return (
    <LightLabel>
      <FormattedMessage
        defaultMessage={translate(expandedLabelKey)}
        id={expandedLabelKey}
        values={{
          count: (
            <DrilldownLink className="sw-body-md-highlight" to={url}>
              {formatMeasure(value, MetricType.ShortInteger)}
            </DrilldownLink>
          ),
        }}
      />
    </LightLabel>
  );
}
