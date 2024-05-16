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
import classNames from 'classnames';
import * as React from 'react';
import { getComponentSecurityHotspotsUrl } from '~sonar-aligned/helpers/urls';
import { MetricKey } from '~sonar-aligned/types/metrics';
import { getLeakValue } from '../../../components/measure/utils';
import { findMeasure } from '../../../helpers/measures';
import { getComponentDrilldownUrl } from '../../../helpers/urls';
import { PullRequest } from '../../../types/branch-like';
import { QualityGateStatusConditionEnhanced } from '../../../types/quality-gates';
import { Component, MeasureEnhanced } from '../../../types/types';
import MeasuresCardNumber from '../components/MeasuresCardNumber';
import MeasuresCardPercent from '../components/MeasuresCardPercent';
import { MeasurementType, getMeasurementMetricKey } from '../utils';
import IssueMeasuresCard from './IssueMeasuresCard';

interface Props {
  className?: string;
  pullRequest: PullRequest;
  component: Component;
  measures: MeasureEnhanced[];
  conditions: QualityGateStatusConditionEnhanced[];
}

export default function MeasuresCardPanel(props: React.PropsWithChildren<Props>) {
  const { pullRequest, component, measures, conditions, className } = props;

  const newSecurityHotspots = getLeakValue(
    findMeasure(measures, MetricKey.new_security_hotspots),
  ) as string;

  return (
    <>
      <IssueMeasuresCard
        conditions={conditions}
        measures={measures}
        component={component}
        pullRequest={pullRequest}
      />

      <div className={classNames('sw-w-full sw-flex sw-flex-row sw-gap-4 sw-mt-4', className)}>
        <div className="sw-flex-1 sw-flex sw-flex-col sw-gap-4">
          <MeasuresCardPercent
            componentKey={component.key}
            branchLike={pullRequest}
            measurementType={MeasurementType.Coverage}
            label="overview.quality_gate.coverage"
            url={getComponentDrilldownUrl({
              componentKey: component.key,
              metric: getMeasurementMetricKey(MeasurementType.Coverage, true),
              branchLike: pullRequest,
              listView: true,
            })}
            conditions={conditions}
            conditionMetric={MetricKey.new_coverage}
            overallConditionMetric={MetricKey.coverage}
            linesMetric={MetricKey.new_lines_to_cover}
            measures={measures}
            showRequired
            useDiffMetric
          />

          <MeasuresCardNumber
            label={
              newSecurityHotspots === '1'
                ? 'issue.type.SECURITY_HOTSPOT'
                : 'issue.type.SECURITY_HOTSPOT.plural'
            }
            url={getComponentSecurityHotspotsUrl(component.key, pullRequest)}
            value={newSecurityHotspots}
            metric={MetricKey.new_security_hotspots}
            conditions={conditions}
            conditionMetric={MetricKey.new_security_hotspots_reviewed}
            showRequired
          />
        </div>

        <div className="sw-flex-1 sw-flex sw-flex-col sw-gap-4">
          <MeasuresCardPercent
            componentKey={component.key}
            branchLike={pullRequest}
            measurementType={MeasurementType.Duplication}
            label="overview.quality_gate.duplications"
            url={getComponentDrilldownUrl({
              componentKey: component.key,
              metric: getMeasurementMetricKey(MeasurementType.Duplication, true),
              branchLike: pullRequest,
              listView: true,
            })}
            conditions={conditions}
            conditionMetric={MetricKey.new_duplicated_lines_density}
            overallConditionMetric={MetricKey.duplicated_lines_density}
            linesMetric={MetricKey.new_lines}
            measures={measures}
            useDiffMetric
            showRequired
          />
        </div>
      </div>
    </>
  );
}
