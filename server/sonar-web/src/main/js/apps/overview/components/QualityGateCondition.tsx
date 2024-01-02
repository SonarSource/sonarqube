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
import * as React from 'react';
import { Path } from 'react-router-dom';
import Link from '../../../components/common/Link';
import IssueTypeIcon from '../../../components/icons/IssueTypeIcon';
import Measure from '../../../components/measure/Measure';
import DrilldownLink from '../../../components/shared/DrilldownLink';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { formatMeasure, isDiffMetric, localizeMetric } from '../../../helpers/measures';
import { getComponentIssuesUrl, getComponentSecurityHotspotsUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { IssueType } from '../../../types/issues';
import { MetricKey } from '../../../types/metrics';
import { QualityGateStatusConditionEnhanced } from '../../../types/quality-gates';
import { Component, Dict } from '../../../types/types';

interface Props {
  branchLike?: BranchLike;
  component: Pick<Component, 'key'>;
  condition: QualityGateStatusConditionEnhanced;
}

export default class QualityGateCondition extends React.PureComponent<Props> {
  getIssuesUrl = (inNewCodePeriod: boolean, customQuery: Dict<string>) => {
    const query: Dict<string | undefined> = {
      resolved: 'false',
      ...getBranchLikeQuery(this.props.branchLike),
      ...customQuery,
    };
    if (inNewCodePeriod) {
      Object.assign(query, { inNewCodePeriod: 'true' });
    }
    return getComponentIssuesUrl(this.props.component.key, query);
  };

  getUrlForSecurityHotspot(inNewCodePeriod: boolean) {
    const query: Dict<string | undefined> = {
      ...getBranchLikeQuery(this.props.branchLike),
    };
    if (inNewCodePeriod) {
      Object.assign(query, { inNewCodePeriod: 'true' });
    }
    return getComponentSecurityHotspotsUrl(this.props.component.key, query);
  }

  getUrlForCodeSmells(inNewCodePeriod: boolean) {
    return this.getIssuesUrl(inNewCodePeriod, { types: 'CODE_SMELL' });
  }

  getUrlForBugsOrVulnerabilities(type: string, inNewCodePeriod: boolean) {
    const RATING_TO_SEVERITIES_MAPPING = [
      'BLOCKER,CRITICAL,MAJOR,MINOR',
      'BLOCKER,CRITICAL,MAJOR',
      'BLOCKER,CRITICAL',
      'BLOCKER',
    ];

    const { condition } = this.props;
    const threshold = condition.level === 'ERROR' ? condition.error : condition.warning;

    return this.getIssuesUrl(inNewCodePeriod, {
      types: type,
      severities: RATING_TO_SEVERITIES_MAPPING[Number(threshold) - 1],
    });
  }

  wrapWithLink(children: React.ReactNode) {
    const { branchLike, component, condition } = this.props;

    const className = classNames(
      'overview-quality-gate-condition',
      `overview-quality-gate-condition-${condition.level.toLowerCase()}`
    );

    const metricKey = condition.measure.metric.key;

    const METRICS_TO_URL_MAPPING: Dict<() => Path> = {
      [MetricKey.reliability_rating]: () =>
        this.getUrlForBugsOrVulnerabilities(IssueType.Bug, false),
      [MetricKey.new_reliability_rating]: () =>
        this.getUrlForBugsOrVulnerabilities(IssueType.Bug, true),
      [MetricKey.security_rating]: () =>
        this.getUrlForBugsOrVulnerabilities(IssueType.Vulnerability, false),
      [MetricKey.new_security_rating]: () =>
        this.getUrlForBugsOrVulnerabilities(IssueType.Vulnerability, true),
      [MetricKey.sqale_rating]: () => this.getUrlForCodeSmells(false),
      [MetricKey.new_maintainability_rating]: () => this.getUrlForCodeSmells(true),
      [MetricKey.security_hotspots_reviewed]: () => this.getUrlForSecurityHotspot(false),
      [MetricKey.new_security_hotspots_reviewed]: () => this.getUrlForSecurityHotspot(true),
    };

    return (
      <li>
        {METRICS_TO_URL_MAPPING[metricKey] ? (
          <Link className={className} to={METRICS_TO_URL_MAPPING[metricKey]()}>
            {children}
          </Link>
        ) : (
          <DrilldownLink
            branchLike={branchLike}
            className={className}
            component={component.key}
            metric={condition.measure.metric.key}
            inNewCodePeriod={condition.period != null}
          >
            {children}
          </DrilldownLink>
        )}
      </li>
    );
  }

  render() {
    const { condition } = this.props;
    const { measure } = condition;
    const { metric } = measure;

    const isDiff = isDiffMetric(metric.key);

    const threshold = (condition.level === 'ERROR' ? condition.error : condition.warning) as string;
    const actual = (condition.period ? measure.period?.value : measure.value) as string;

    let operator = translate('quality_gates.operator', condition.op);

    if (metric.type === 'RATING') {
      operator = translate('quality_gates.operator', condition.op, 'rating');
    }

    return this.wrapWithLink(
      <div className="overview-quality-gate-condition-container display-flex-center">
        <div className="overview-quality-gate-condition-value text-center spacer-right">
          <Measure
            decimals={2}
            metricKey={measure.metric.key}
            metricType={measure.metric.type}
            value={actual}
          />
        </div>

        <div>
          <span className="overview-quality-gate-condition-metric little-spacer-right">
            <IssueTypeIcon className="little-spacer-right" query={metric.key} />
            {localizeMetric(metric.key)}
          </span>
          {!isDiff && condition.period != null && (
            <span className="overview-quality-gate-condition-period text-ellipsis little-spacer-right">
              {translate('quality_gates.conditions.new_code')}
            </span>
          )}
          <span className="little-spacer-top small text-muted">
            {operator} {formatMeasure(threshold, metric.type)}
          </span>
        </div>
      </div>
    );
  }
}
