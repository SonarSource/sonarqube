/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React from 'react';
import classNames from 'classnames';
import { ComponentType, PeriodsListType, EnhancedConditionType } from '../propTypes';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import Measure from '../../component-measures/components/Measure';
import { getPeriodValue, isDiffMetric, formatMeasure } from '../../../helpers/measures';
import { translate } from '../../../helpers/l10n';
import { getPeriod, getPeriodDate } from '../../../helpers/periods';

const QualityGateCondition = ({ component, periods, condition }) => {
  const { measure } = condition;
  const { metric } = measure;

  const isRating = metric.type === 'RATING';
  const isDiff = isDiffMetric(metric.key);

  const threshold = condition.level === 'ERROR' ?
      condition.error :
      condition.warning;

  const actual = condition.period ?
      getPeriodValue(measure, condition.period) :
      measure.value;
  const period = getPeriod(periods, condition.period);

  const periodDate = getPeriodDate(period);
  const operator = isRating ?
      translate('quality_gates.operator', condition.op, 'rating') :
      translate('quality_gates.operator', condition.op);

  const className = classNames(
      'overview-quality-gate-condition',
      'overview-quality-gate-condition-' + condition.level.toLowerCase(),
      { 'overview-quality-gate-condition-leak': period != null }
  );

  return (
      <li className={className}>
        <div className="overview-quality-gate-condition-container">
          <div className="overview-quality-gate-condition-value">
            <DrilldownLink
                className={isRating ? 'link-no-underline' : null}
                component={component.key}
                metric={metric.key}
                period={condition.period}
                periodDate={periodDate}>
              <Measure measure={{ value: actual, leak: actual }} metric={metric}/>
            </DrilldownLink>
          </div>

          <div>
            <div className="overview-quality-gate-condition-metric">
              {metric.name}
            </div>
            {!isDiff && period != null && (
                <div className="overview-quality-gate-condition-period">
                  {translate('quality_gates.conditions.leak')}
                </div>
            )}
            <div className="overview-quality-gate-threshold">
              {operator} {formatMeasure(threshold, metric.type)}
            </div>
          </div>
        </div>
      </li>
  );
};

QualityGateCondition.propTypes = {
  component: ComponentType.isRequired,
  periods: PeriodsListType.isRequired,
  condition: EnhancedConditionType.isRequired
};

export default QualityGateCondition;
