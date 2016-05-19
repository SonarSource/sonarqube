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

import { ComponentType, PeriodsListType, EnhancedConditionType } from '../propTypes';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import { formatMeasure, getPeriodValue } from '../../../helpers/measures';
import { translate } from '../../../helpers/l10n';
import { getPeriod, getPeriodLabel, getPeriodDate } from '../../../helpers/periods';

const QualityGateCondition = ({ component, periods, condition }) => {
  const { measure } = condition;
  const { metric } = measure;

  const threshold = condition.level === 'ERROR' ?
      condition.error :
      condition.warning;

  const actual = condition.period ?
      getPeriodValue(measure, condition.period) :
      measure.value;

  const period = getPeriod(periods, condition.period);
  const periodLabel = getPeriodLabel(period);
  const periodDate = getPeriodDate(period);

  return (
      <li className="overview-quality-gate-condition">
        <div className="overview-quality-gate-condition-period">
          {periodLabel}
        </div>

        <div className="overview-quality-gate-condition-container">
          <div className="overview-quality-gate-condition-value">
            <DrilldownLink
                component={component.key}
                metric={metric.key}
                period={condition.period}
                periodDate={periodDate}>
              <span className={'alert_' + condition.level.toUpperCase()}>
                {formatMeasure(actual, metric.type)}
              </span>
            </DrilldownLink>
          </div>

          <div>
            <div className="overview-quality-gate-condition-metric">
              {metric.name}
            </div>
            <div className="overview-quality-gate-condition-threshold">
              {translate('quality_gates.operator', condition.op, 'short')}
              {' '}
              {formatMeasure(threshold, metric.type)}
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
