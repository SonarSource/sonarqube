/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import classNames from 'classnames';
import { Link } from 'react-router';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import Measure from '../../component-measures/components/Measure';
import { getPeriodValue, isDiffMetric, formatMeasure } from '../../../helpers/measures';
import { translate } from '../../../helpers/l10n';
import { getPeriod, getPeriodDate } from '../../../helpers/periods';
import { getComponentIssuesUrl } from '../../../helpers/urls';
import IssueTypeIcon from '../../../components/ui/IssueTypeIcon';

export default class QualityGateCondition extends React.Component {
  props: {
    component: { key: string },
    periods: Array<{
      index: number,
      date: string,
      mode: string,
      parameter?: string
    }>,
    condition: {
      level: string,
      measure: {
        metric: {
          key: string,
          name: string,
          type: string
        },
        value: string
      },
      op: string,
      period: number,
      error: string,
      warning: string
    }
  };

  getIssuesUrl(sinceLeakPeriod: boolean, customQuery: {}) {
    const query: Object = {
      resolved: 'false',
      ...customQuery
    };
    if (sinceLeakPeriod) {
      Object.assign(query, { sinceLeakPeriod: 'true' });
    }
    return getComponentIssuesUrl(this.props.component.key, query);
  }

  getUrlForCodeSmells(sinceLeakPeriod: boolean) {
    return this.getIssuesUrl(sinceLeakPeriod, { types: 'CODE_SMELL' });
  }

  getUrlForBugsOrVulnerabilities(type: string, sinceLeakPeriod: boolean) {
    const RATING_TO_SEVERITIES_MAPPING = [
      'BLOCKER,CRITICAL,MAJOR,MINOR',
      'BLOCKER,CRITICAL,MAJOR',
      'BLOCKER,CRITICAL',
      'BLOCKER'
    ];

    const { condition } = this.props;
    const threshold = condition.level === 'ERROR' ? condition.error : condition.warning;

    return this.getIssuesUrl(sinceLeakPeriod, {
      types: type,
      severities: RATING_TO_SEVERITIES_MAPPING[Number(threshold) - 1]
    });
  }

  getUrlForType(type: string, sinceLeakPeriod: boolean) {
    return type === 'CODE_SMELL'
      ? this.getUrlForCodeSmells(sinceLeakPeriod)
      : this.getUrlForBugsOrVulnerabilities(type, sinceLeakPeriod);
  }

  wrapWithLink(children: Object) {
    const { component, periods, condition } = this.props;

    const period = getPeriod(periods, condition.period);
    const periodDate = getPeriodDate(period);

    const className = classNames(
      'overview-quality-gate-condition',
      'overview-quality-gate-condition-' + condition.level.toLowerCase(),
      { 'overview-quality-gate-condition-leak': period != null }
    );

    const metricKey = condition.measure.metric.key;

    const RATING_METRICS_MAPPING = {
      reliability_rating: ['BUG', false],
      new_reliability_rating: ['BUG', true],
      security_rating: ['VULNERABILITY', false],
      new_security_rating: ['VULNERABILITY', true],
      sqale_rating: ['CODE_SMELL', false],
      new_sqale_rating: ['CODE_SMELL', true]
    };

    return RATING_METRICS_MAPPING[metricKey]
      ? <Link to={this.getUrlForType(...RATING_METRICS_MAPPING[metricKey])} className={className}>
          {children}
        </Link>
      : <DrilldownLink
          className={className}
          component={component.key}
          metric={condition.measure.metric.key}
          period={condition.period}
          periodDate={periodDate}>
          {children}
        </DrilldownLink>;
  }

  render() {
    const { periods, condition } = this.props;

    const { measure } = condition;
    const { metric } = measure;

    const isRating = metric.type === 'RATING';
    const isDiff = isDiffMetric(metric.key);

    const threshold = condition.level === 'ERROR' ? condition.error : condition.warning;

    const actual = condition.period ? getPeriodValue(measure, condition.period) : measure.value;
    const period = getPeriod(periods, condition.period);

    const operator = isRating
      ? translate('quality_gates.operator', condition.op, 'rating')
      : translate('quality_gates.operator', condition.op);

    return this.wrapWithLink(
      <div className="overview-quality-gate-condition-container">
        <div className="overview-quality-gate-condition-value">
          <Measure measure={{ value: actual, leak: actual }} metric={metric} />
        </div>

        <div>
          <div className="overview-quality-gate-condition-metric">
            <IssueTypeIcon query={metric.key} className="little-spacer-right" />
            {metric.name}
          </div>
          {!isDiff &&
            period != null &&
            <div className="overview-quality-gate-condition-period">
              {translate('quality_gates.conditions.leak')}
            </div>}
          <div className="overview-quality-gate-threshold">
            {operator} {formatMeasure(threshold, metric.type)}
          </div>
        </div>
      </div>
    );
  }
}
