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
import classNames from 'classnames';
import { themeColor } from 'design-system';
import * as React from 'react';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { Condition, Metric } from '../../../types/types';
import { getCorrectCaycCondition } from '../utils';
import ConditionValueDescription from './ConditionValueDescription';

interface Props {
  condition: Condition;
  isCaycCompliantAndOverCompliant?: boolean;
  isCaycModal?: boolean;
  metric: Metric;
}

function ConditionValue({
  condition,
  isCaycModal,
  metric,
  isCaycCompliantAndOverCompliant,
}: Props) {
  if (isCaycModal) {
    const isToBeModified = condition.error !== getCorrectCaycCondition(condition).error;

    return (
      <>
        {isToBeModified && (
          <RedColorText className="sw-line-through sw-mr-2">
            {formatMeasure(condition.error, metric.type)}
          </RedColorText>
        )}
        <GreenColorText isToBeModified={isToBeModified} className={classNames('sw-mr-2')}>
          {formatMeasure(getCorrectCaycCondition(condition).error, metric.type)}
        </GreenColorText>
        <ConditionValueDescription
          isToBeModified={isToBeModified}
          condition={getCorrectCaycCondition(condition)}
          metric={metric}
        />
      </>
    );
  }

  return (
    <>
      <span className="sw-mr-2">{formatMeasure(condition.error, metric.type)}</span>
      {isCaycCompliantAndOverCompliant && condition.isCaycCondition && (
        <ConditionValueDescription condition={condition} metric={metric} />
      )}
    </>
  );
}

export default ConditionValue;

const RedColorText = styled.span`
  color: ${themeColor('qgConditionNotCayc')};
`;

export const GreenColorText = styled.span<{ isToBeModified: boolean }>`
  color: ${(props) => (props.isToBeModified ? themeColor('qgConditionCayc') : 'inherit')};
`;
