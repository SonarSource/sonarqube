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
import { LightLabel, TextError } from 'design-system';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { To } from 'react-router-dom';
import { formatMeasure } from '../../../helpers/measures';
import { MetricKey, MetricType } from '../../../types/metrics';
import { QualityGateStatusConditionEnhanced } from '../../../types/quality-gates';
import { Status, getConditionRequiredLabel } from '../utils';
import MeasuresCard, { MeasuresCardProps } from './MeasuresCard';

interface Props extends MeasuresCardProps {
  conditions: QualityGateStatusConditionEnhanced[];
  label: string;
  url: To;
  value?: string;
  conditionMetric: MetricKey;
  showRequired?: boolean;
}

export default function MeasuresCardNumber(
  props: React.PropsWithChildren<Props & React.HTMLAttributes<HTMLDivElement>>,
) {
  const { label, value, conditions, url, conditionMetric, showRequired = false, ...rest } = props;

  const intl = useIntl();

  const condition = conditions.find((condition) => condition.metric === conditionMetric);

  const conditionFailed = condition?.level === Status.ERROR;

  return (
    <MeasuresCard
      url={url}
      value={formatMeasure(value, MetricType.ShortInteger)}
      label={label}
      failed={conditionFailed}
      {...rest}
    >
      <span className="sw-body-xs sw-mt-3">
        {showRequired &&
          condition &&
          (conditionFailed ? (
            <TextError
              className="sw-font-regular sw-inline"
              text={getConditionRequiredLabel(condition, intl, true)}
            />
          ) : (
            <LightLabel>{getConditionRequiredLabel(condition, intl)}</LightLabel>
          ))}
      </span>
    </MeasuresCard>
  );
}
