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
import { getComponentDrilldownUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { isApplication } from '../../../types/component';
import { MetricKey } from '../../../types/metrics';
import { QualityGateStatusConditionEnhanced } from '../../../types/quality-gates';
import { Component, MeasureEnhanced } from '../../../types/types';
import MeasuresCardPercent from '../components/MeasuresCardPercent';
import { MeasurementType, getMeasurementMetricKey } from '../utils';

interface Props {
  useDiffMetric?: boolean;
  branch?: BranchLike;
  component: Component;
  measures: MeasureEnhanced[];
  conditions: QualityGateStatusConditionEnhanced[];
}

/**
 * Renders Coverage and Duplication cards for the Overview page.
 */
export default function MeasuresPanelPercentCards(props: Readonly<Props>) {
  const { useDiffMetric, branch, component, measures, conditions } = props;

  const isApp = isApplication(component.qualifier);

  return (
    <>
      <MeasuresCardPercent
        branchLike={branch}
        componentKey={component.key}
        conditions={conditions}
        measures={measures}
        measurementType={MeasurementType.Coverage}
        label="overview.quality_gate.coverage"
        url={getComponentDrilldownUrl({
          componentKey: component.key,
          metric: getMeasurementMetricKey(MeasurementType.Coverage, Boolean(useDiffMetric)),
          branchLike: branch,
          listView: true,
        })}
        conditionMetric={useDiffMetric ? MetricKey.new_coverage : MetricKey.coverage}
        linesMetric={useDiffMetric ? MetricKey.new_lines_to_cover : MetricKey.lines_to_cover}
        useDiffMetric={useDiffMetric}
        showRequired={!isApp}
      />

      <MeasuresCardPercent
        branchLike={branch}
        componentKey={component.key}
        conditions={conditions}
        measures={measures}
        measurementType={MeasurementType.Duplication}
        label="overview.quality_gate.duplications"
        url={getComponentDrilldownUrl({
          componentKey: component.key,
          metric: getMeasurementMetricKey(MeasurementType.Duplication, Boolean(useDiffMetric)),
          branchLike: branch,
          listView: true,
        })}
        conditionMetric={
          useDiffMetric
            ? MetricKey.new_duplicated_lines_density
            : MetricKey.duplicated_lines_density
        }
        linesMetric={useDiffMetric ? MetricKey.new_lines : MetricKey.lines}
        useDiffMetric={useDiffMetric}
        showRequired={!isApp}
      />
    </>
  );
}
