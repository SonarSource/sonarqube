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

import { LinkHighlight, LinkStandalone } from '@sonarsource/echoes-react';
import classNames from 'classnames';
import { CoverageIndicator, DuplicationsIndicator, LightLabel, TextError } from 'design-system';
import * as React from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { To } from 'react-router-dom';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import { duplicationRatingConverter, getLeakValue } from '../../../components/measure/utils';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { findMeasure, localizeMetric } from '../../../helpers/measures';
import { isDefined } from '../../../helpers/types';
import { getComponentDrilldownUrl } from '../../../helpers/urls';
import { isPullRequest } from '../../../sonar-aligned/helpers/branch-like';
import { BranchLike } from '../../../types/branch-like';
import { QualityGateStatusConditionEnhanced } from '../../../types/quality-gates';
import { MeasureEnhanced } from '../../../types/types';
import {
  MeasurementType,
  Status,
  getConditionRequiredLabel,
  getMeasurementMetricKey,
} from '../utils';
import AfterMergeNote from './AfterMergeNote';
import MeasuresCard from './MeasuresCard';

interface Props {
  branchLike?: BranchLike;
  componentKey: string;
  conditionMetric: MetricKey;
  conditions: QualityGateStatusConditionEnhanced[];
  label: string;
  linesMetric: MetricKey;
  measurementType: MeasurementType;
  measures: MeasureEnhanced[];
  overallConditionMetric?: MetricKey;
  showRequired?: boolean;
  url: To;
  useDiffMetric?: boolean;
}

export default function MeasuresCardPercent(
  props: Readonly<React.PropsWithChildren<Props & React.HTMLAttributes<HTMLDivElement>>>,
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
    overallConditionMetric,
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

  const formattedMeasure = formatMeasure(linesValue ?? '0', MetricType.ShortInteger);

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
                <LinkStandalone
                  highlight={LinkHighlight.Default}
                  aria-label={translateWithParameters(
                    'overview.see_more_details_on_x_y',
                    isDefined(linesValue) ? `${formattedMeasure} (${linesValue})` : '0',
                    localizeMetric(linesMetric),
                  )}
                  className="sw-body-sm-highlight sw--mt-[3px]"
                  to={linesUrl}
                >
                  {formattedMeasure}
                </LinkStandalone>
              ),
            }}
          />
        </LightLabel>
      </div>
      {overallConditionMetric && isPullRequest(branchLike) && (
        <AfterMergeNote measures={measures} overallMetric={overallConditionMetric} />
      )}
    </MeasuresCard>
  );
}

function renderIcon(type: MeasurementType, value?: string) {
  if (type === MeasurementType.Coverage) {
    return <CoverageIndicator aria-hidden="true" value={value} size="md" />;
  }

  const rating = duplicationRatingConverter(Number(value));
  return <DuplicationsIndicator aria-hidden="true" rating={rating} size="md" />;
}
