/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as React from 'react';
import * as classNames from 'classnames';
import { Link } from 'react-router';
import DrilldownLink from '../../../components/shared/DrilldownLink';
import Measure from '../../../components/measure/Measure';
import IssueTypeIcon from '../../../components/ui/IssueTypeIcon';
import { getPeriodValue, isDiffMetric, formatMeasure } from '../../../helpers/measures';
import { translate } from '../../../helpers/l10n';
import { getComponentIssuesUrl } from '../../../helpers/urls';
import { getBranchLikeQuery, isPullRequest, isShortLivingBranch } from '../../../helpers/branches';

interface Props {
  branchLike?: T.BranchLike;
  component: Pick<T.Component, 'key'>;
  condition: T.QualityGateStatusConditionEnhanced;
}

export default class QualityGateCondition extends React.PureComponent<Props> {
  getDecimalsNumber(threshold: number, value: number) {
    const delta = Math.abs(threshold - value);
    if (delta < 0.1 && delta > 0) {
      const match = delta.toFixed(20).match('[^0.]');
      return match && match.index ? match.index - 1 : undefined;
    } else {
      return undefined;
    }
  }

  getIssuesUrl = (sinceLeakPeriod: boolean, customQuery: T.Dict<string>) => {
    const query: T.Dict<string | undefined> = {
      resolved: 'false',
      ...getBranchLikeQuery(this.props.branchLike),
      ...customQuery
    };
    if (sinceLeakPeriod) {
      Object.assign(query, { sinceLeakPeriod: 'true' });
    }
    return getComponentIssuesUrl(this.props.component.key, query);
  };

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

  wrapWithLink(children: React.ReactNode) {
    const { branchLike, component, condition } = this.props;

    const className = classNames(
      'overview-quality-gate-condition',
      'overview-quality-gate-condition-' + condition.level.toLowerCase(),
      {
        'overview-quality-gate-condition-leak':
          condition.period != null && !isPullRequest(branchLike) && !isShortLivingBranch(branchLike)
      }
    );

    const metricKey = condition.measure.metric.key;

    const RATING_METRICS_MAPPING: T.Dict<[string, boolean]> = {
      reliability_rating: ['BUG', false],
      new_reliability_rating: ['BUG', true],
      security_rating: ['VULNERABILITY', false],
      new_security_rating: ['VULNERABILITY', true],
      sqale_rating: ['CODE_SMELL', false],
      new_maintainability_rating: ['CODE_SMELL', true]
    };

    return RATING_METRICS_MAPPING[metricKey] ? (
      <Link className={className} to={this.getUrlForType(...RATING_METRICS_MAPPING[metricKey])}>
        {children}
      </Link>
    ) : (
      <DrilldownLink
        branchLike={branchLike}
        className={className}
        component={component.key}
        metric={condition.measure.metric.key}
        sinceLeakPeriod={condition.period != null}>
        {children}
      </DrilldownLink>
    );
  }

  render() {
    const { condition } = this.props;
    const { measure } = condition;
    const { metric } = measure;

    const isDiff = isDiffMetric(metric.key);

    const threshold = (condition.level === 'ERROR' ? condition.error : condition.warning) as string;
    const actual = (condition.period
      ? getPeriodValue(measure, condition.period)
      : measure.value) as string;

    let operator = translate('quality_gates.operator', condition.op);
    let decimals: number | undefined = undefined;

    if (metric.type === 'RATING') {
      operator = translate('quality_gates.operator', condition.op, 'rating');
    } else if (metric.type === 'PERCENT') {
      decimals = this.getDecimalsNumber(parseFloat(threshold), parseFloat(actual));
    }

    return this.wrapWithLink(
      <div className="overview-quality-gate-condition-container">
        <div className="overview-quality-gate-condition-value">
          <Measure
            decimals={decimals}
            metricKey={measure.metric.key}
            metricType={measure.metric.type}
            value={actual}
          />
        </div>

        <div>
          <div className="overview-quality-gate-condition-metric">
            <IssueTypeIcon className="little-spacer-right" query={metric.key} />
            {metric.name}
          </div>
          {!isDiff && condition.period != null && (
            <div className="overview-quality-gate-condition-period">
              {translate('quality_gates.conditions.new_code')}
            </div>
          )}
          <div className="overview-quality-gate-threshold">
            {operator} {formatMeasure(threshold, metric.type)}
          </div>
        </div>
      </div>
    );
  }
}
