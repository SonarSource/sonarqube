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
import {
  ContentLink,
  CoverageIndicator,
  DuplicationsIndicator,
  LightLabel,
  TextError,
} from 'design-system';
import * as React from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { To } from 'react-router-dom';
import { duplicationRatingConverter, getLeakValue } from '../../../components/measure/utils';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { findMeasure, formatMeasure, localizeMetric } from '../../../helpers/measures';
import { getComponentDrilldownUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { MetricKey, MetricType } from '../../../types/metrics';
import { QualityGateStatusConditionEnhanced } from '../../../types/quality-gates';
import { MeasureEnhanced } from '../../../types/types';
import {
  MeasurementType,
  Status,
  getConditionRequiredLabel,
  getMeasurementMetricKey,
} from '../utils';
import MeasuresCard from './MeasuresCard';

interface Props {
  componentKey: string;
  branchLike?: BranchLike;
  measurementType: MeasurementType;
  label: string;
  url: To;
  measures: MeasureEnhanced[];
  conditions: QualityGateStatusConditionEnhanced[];
  conditionMetric: MetricKey;
  linesMetric: MetricKey;
  useDiffMetric?: boolean;
  showRequired?: boolean;
}

export default function MeasuresCardPercent(
  props: React.PropsWithChildren<Props & React.HTMLAttributes<HTMLDivElement>>,
) {
  const {
    componentKey,
    branchLike,
    measurementType,
    label,
    url,
    measures,
    conditions,
    conditionMetric,
    linesMetric,
    useDiffMetric = false,
    showRequired = false,
  } = props;

  const intl = useIntl();

  const metricKey = getMeasurementMetricKey(measurementType, useDiffMetric);
  const value = useDiffMetric
    ? getLeakValue(findMeasure(measures, metricKey))
    : findMeasure(measures, metricKey)?.value;
  const linesValue = useDiffMetric
    ? getLeakValue(findMeasure(measures, linesMetric))
    : findMeasure(measures, linesMetric)?.value;
  const linesLabel = `overview.${metricKey}.on_x_new_lines`;
  const linesUrl = getComponentDrilldownUrl({
    componentKey,
    metric: linesMetric,
    branchLike,
    listView: true,
  });

  const condition = conditions.find((c) => c.metric === conditionMetric);
  const conditionFailed = condition?.level === Status.ERROR;

  const shouldRenderRequiredLabel = showRequired && condition;

  return (
    <MeasuresCard
      value={formatMeasure(value, MetricType.Percent)}
      metric={metricKey}
      url={url}
      label={label}
      failed={conditionFailed}
      icon={renderIcon(measurementType, value)}
    >
      {shouldRenderRequiredLabel && (
        <span className="sw-body-xs sw-mt-3">
          {conditionFailed ? (
            <TextError
              className="sw-font-regular sw-inline"
              text={getConditionRequiredLabel(condition, intl, true)}
            />
          ) : (
            <LightLabel>{getConditionRequiredLabel(condition, intl)}</LightLabel>
          )}
        </span>
      )}
      <div
        className={classNames('sw-flex sw-body-xs sw-justify-between sw-items-center', {
          'sw-mt-1': shouldRenderRequiredLabel,
          'sw-mt-3': !shouldRenderRequiredLabel,
        })}
      >
        <LightLabel className="sw-flex sw-gap-1">
          <FormattedMessage
            defaultMessage={translate(linesLabel)}
            id={linesLabel}
            values={{
              link: (
                <ContentLink
                  aria-label={translateWithParameters(
                    'overview.see_more_details_on_x_y',
                    linesValue ?? '0',
                    localizeMetric(linesMetric),
                  )}
                  className="sw-body-sm-highlight sw--mt-[3px]"
                  to={linesUrl}
                >
                  {formatMeasure(linesValue ?? '0', MetricType.ShortInteger)}
                </ContentLink>
              ),
            }}
          />
        </LightLabel>
      </div>
    </MeasuresCard>
  );
}

function renderIcon(type: MeasurementType, value?: string) {
  if (type === MeasurementType.Coverage) {
    return <CoverageIndicator value={value} size="md" />;
  }

  const rating = duplicationRatingConverter(Number(value));
  return <DuplicationsIndicator rating={rating} size="md" />;
}
