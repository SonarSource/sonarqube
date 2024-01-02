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
import { DrilldownLink, GreySeparator, LightLabel, LightPrimary } from 'design-system';
import * as React from 'react';
import { getLeakValue } from '../../../components/measure/utils';
import { isPullRequest } from '../../../helpers/branch-like';
import { getLocalizedMetricName, translate, translateWithParameters } from '../../../helpers/l10n';
import { findMeasure, formatMeasure, localizeMetric } from '../../../helpers/measures';
import { getComponentDrilldownUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { MetricKey, MetricType } from '../../../types/metrics';
import { Component, MeasureEnhanced } from '../../../types/types';
import AfterMergeEstimate from '../pullRequests/AfterMergeEstimate';
import { MeasurementType, getMeasurementMetricKey } from '../utils';
import DrilldownMeasureValue from './DrilldownMeasureValue';
import MeasuresPanelCard from './MeasuresPanelCard';
import MeasuresPanelPercentMeasureLabel from './MeasuresPanelPercentMeasureLabel';

interface Props {
  branchLike?: BranchLike;
  component: Component;
  useDiffMetric: boolean;
  measures: MeasureEnhanced[];
  ratingIcon: (value: string | undefined) => React.ReactElement;
  secondaryMetricKey?: MetricKey;
  type: MeasurementType;
}

export default function MeasuresPanelPercentMeasure(props: Props) {
  const {
    branchLike,
    component,
    measures,
    ratingIcon,
    secondaryMetricKey,
    type,
    useDiffMetric = false,
  } = props;
  const metricKey = getMeasurementMetricKey(type, useDiffMetric);
  const measure = findMeasure(measures, metricKey);

  let value;
  if (measure) {
    value = useDiffMetric ? getLeakValue(measure) : measure.value;
  }

  const url = getComponentDrilldownUrl({
    componentKey: component.key,
    metric: metricKey,
    branchLike,
    listView: true,
  });

  const formattedValue = formatMeasure(value, MetricType.Percent, {
    decimals: 2,
    omitExtraDecimalZeros: true,
  });

  return (
    <MeasuresPanelCard
      data-test={`overview__measures-${type.toString().toLowerCase()}`}
      category={<LightPrimary>{translate('overview.measurement_type', type)}</LightPrimary>}
      rating={ratingIcon(value)}
    >
      <>
        <div className="sw-body-md sw-flex sw-items-center sw-mb-3">
          {value === undefined ? (
            <LightLabel aria-label={translate('no_data')}> — </LightLabel>
          ) : (
            <DrilldownLink
              aria-label={translateWithParameters(
                'overview.see_more_details_on_x_of_y',
                value,
                localizeMetric(metricKey),
              )}
              to={url}
            >
              {formattedValue}
            </DrilldownLink>
          )}

          <LightLabel className="sw-ml-2">
            {translate('overview.measurement_type', type)}
          </LightLabel>
        </div>
        <MeasuresPanelPercentMeasureLabel
          component={component}
          measures={measures}
          type={type}
          useDiffMetric={useDiffMetric}
          branchLike={branchLike}
        />

        {!useDiffMetric && secondaryMetricKey && (
          <>
            <GreySeparator className="sw-mt-4" />
            <div className="sw-body-md sw-flex sw-items-center sw-mt-4">
              <DrilldownMeasureValue
                branchLike={branchLike}
                component={component}
                measures={measures}
                metric={secondaryMetricKey}
              />
              <LightLabel className="sw-ml-2">
                {getLocalizedMetricName({ key: secondaryMetricKey })}
              </LightLabel>
            </div>
          </>
        )}

        {isPullRequest(branchLike) && (
          <div className="sw-body-sm sw-flex sw-items-center sw-mt-4">
            <AfterMergeEstimate measures={measures} type={type} />

            <LightLabel className="sw-ml-2">
              {translate('component_measures.facet_category.overall_category.estimated')}
            </LightLabel>
          </div>
        )}
      </>
    </MeasuresPanelCard>
  );
}
