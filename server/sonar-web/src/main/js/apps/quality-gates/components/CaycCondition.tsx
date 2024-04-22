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
import styled from '@emotion/styled';
import { HelperHintIcon, Highlight } from 'design-system';
import React from 'react';
import { FormattedMessage } from 'react-intl';
import withMetricsContext from '../../../app/components/metrics/withMetricsContext';
import { translate } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
import DocHelpTooltip from '../../../sonar-aligned/components/controls/DocHelpTooltip';
import { MetricKey } from '../../../types/metrics';
import { Condition, Dict, Metric } from '../../../types/types';
import { getCaycConditionMetadata, getLocalizedMetricNameNoDiffMetric } from '../utils';

interface Props {
  condition: Condition;
  metric: Metric;
  metrics: Dict<Metric>;
}

function CaycCondition({ condition, metric, metrics }: Readonly<Props>) {
  const { shouldRenderOperator } = getCaycConditionMetadata(condition);

  const renderOperator = () => {
    const { op = 'GT' } = condition;
    return translate('quality_gates.operator.inverted', op);
  };

  return (
    <StyledItem
      data-guiding-id={
        condition.metric === MetricKey.new_violations ? 'caycConditionsSimplification' : undefined
      }
    >
      <span>
        <Highlight>{translate(`metric.${metric.key}.description.positive`)}</Highlight>
      </span>

      {shouldRenderOperator && (
        <StyledContentCell>
          <FormattedMessage
            id="quality_gates.conditions.cayc.metric"
            defaultMessage={translate('quality_gates.conditions.cayc.metric')}
            values={{
              metric: getLocalizedMetricNameNoDiffMetric(metric, metrics),
              operator: renderOperator(),
              value: <Highlight>&nbsp;{formatMeasure(condition.error, metric.type)}</Highlight>,
            }}
          />
          <DocHelpTooltip
            className="sw-ml-2 sw-align-text-top"
            content={translate('quality_gates.conditions.cayc.threshold.hint')}
          >
            <HelperHintIcon />
          </DocHelpTooltip>
        </StyledContentCell>
      )}
    </StyledItem>
  );
}

const StyledItem = styled.li`
  display: flex;
  justify-content: space-between;
  padding: 1rem;
  flex-wrap: wrap;

  &:not(:last-child) {
    padding-bottom: 0;
  }

  &:not(:last-child):after {
    content: '';
    border-bottom: 1px solid rgb(235, 235, 235);
    display: flex;
    height: 10px;
    width: 100%;
    padding-top: 1rem;
  }
`;

const StyledContentCell = styled.div`
  white-space: nowrap;

  & > div {
    justify-content: flex-end !important;
  }
`;

export default withMetricsContext(CaycCondition);
