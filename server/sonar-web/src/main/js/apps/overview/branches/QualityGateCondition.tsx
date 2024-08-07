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
import { LinkBox, TextMuted } from 'design-system';
import * as React from 'react';
import { Path } from 'react-router-dom';
import { getBranchLikeQuery } from '~sonar-aligned/helpers/branch-like';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import {
  getComponentIssuesUrl,
  getComponentSecurityHotspotsUrl,
} from '~sonar-aligned/helpers/urls';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import withMetricsContext from '../../../app/components/metrics/withMetricsContext';
import IssueTypeIcon from '../../../components/icon-mappers/IssueTypeIcon';
import MeasureIndicator from '../../../components/measure/MeasureIndicator';
import {
  DEFAULT_ISSUES_QUERY,
  isIssueMeasure,
  propsToIssueParams,
} from '../../../components/shared/utils';
import { getOperatorLabel } from '../../../helpers/qualityGates';
import { getComponentDrilldownUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { IssueType } from '../../../types/issues';
import { QualityGateStatusConditionEnhanced } from '../../../types/quality-gates';
import { Component, Dict, Metric } from '../../../types/types';
import { getLocalizedMetricNameNoDiffMetric } from '../../quality-gates/utils';
import { RATING_TO_SEVERITIES_MAPPING } from '../utils';

interface Props {
  branchLike?: BranchLike;
  component: Pick<Component, 'key'>;
  condition: QualityGateStatusConditionEnhanced;
  metrics: Dict<Metric>;
}

export class QualityGateCondition extends React.PureComponent<Props> {
  getIssuesUrl = (inNewCodePeriod: boolean, customQuery: Dict<string>) => {
    const query: Dict<string | undefined> = {
      ...DEFAULT_ISSUES_QUERY,
      ...getBranchLikeQuery(this.props.branchLike),
      ...customQuery,
    };
    if (inNewCodePeriod) {
      Object.assign(query, { inNewCodePeriod: 'true' });
    }
    return getComponentIssuesUrl(this.props.component.key, query);
  };

  getUrlForSecurityHotspot(inNewCodePeriod: boolean) {
    return getComponentSecurityHotspotsUrl(
      this.props.component.key,
      this.props.branchLike,
      inNewCodePeriod ? { inNewCodePeriod: 'true' } : {},
    );
  }

  getUrlForCodeSmells(inNewCodePeriod: boolean) {
    return this.getIssuesUrl(inNewCodePeriod, { types: 'CODE_SMELL' });
  }

  getUrlForBugsOrVulnerabilities(type: string, inNewCodePeriod: boolean) {
    const { condition } = this.props;
    const threshold = condition.level === 'ERROR' ? condition.error : condition.warning;

    return this.getIssuesUrl(inNewCodePeriod, {
      types: type,
      severities: RATING_TO_SEVERITIES_MAPPING[Number(threshold) - 1],
    });
  }

  wrapWithLink(children: React.ReactNode) {
    const { branchLike, component, condition } = this.props;

    const metricKey = condition.measure.metric.key;

    const METRICS_TO_URL_MAPPING: Dict<() => Partial<Path>> = {
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

    if (METRICS_TO_URL_MAPPING[metricKey]) {
      return <LinkBox to={METRICS_TO_URL_MAPPING[metricKey]()}>{children}</LinkBox>;
    }

    const url = isIssueMeasure(condition.measure.metric.key)
      ? getComponentIssuesUrl(component.key, {
          ...propsToIssueParams(condition.measure.metric.key, condition.period != null),
          ...getBranchLikeQuery(branchLike),
        })
      : getComponentDrilldownUrl({
          componentKey: component.key,
          metric: condition.measure.metric.key,
          branchLike,
          listView: true,
        });

    return <LinkBox to={url}>{children}</LinkBox>;
  }

  getPrimaryText = () => {
    const { condition } = this.props;
    const { measure } = condition;
    const { metric } = measure;

    const subText = getLocalizedMetricNameNoDiffMetric(metric, this.props.metrics);

    if (metric.type !== MetricType.Rating) {
      const actual = (condition.period ? measure.period?.value : measure.value) as string;
      const formattedValue = formatMeasure(actual, metric.type, {
        decimals: 1,
        omitExtraDecimalZeros: metric.type === MetricType.Percent,
      });
      return `${formattedValue} ${subText}`;
    }

    return subText;
  };

  render() {
    const { condition, component } = this.props;
    const { measure } = condition;
    const { metric } = measure;

    const threshold = (condition.level === 'ERROR' ? condition.error : condition.warning) as string;
    const actual = (condition.period ? measure.period?.value : measure.value) as string;

    const operator = getOperatorLabel(condition.op, metric);

    return this.wrapWithLink(
      <div className="sw-flex sw-items-center sw-p-2">
        <MeasureIndicator
          className="sw-flex sw-justify-center sw-w-6 sw-mx-4"
          decimals={2}
          componentKey={component.key}
          metricKey={measure.metric.key}
          metricType={measure.metric.type}
          value={actual}
        />
        <div className="sw-flex sw-flex-col sw-text-sm">
          <div className="sw-flex sw-items-center">
            <IssueTypeIcon className="sw-mr-2" type={metric.key} />
            <span className="sw-body-sm-highlight sw-text-ellipsis sw-max-w-abs-300">
              {this.getPrimaryText()}
            </span>
          </div>
          <TextMuted text={`${operator} ${formatMeasure(threshold, metric.type)}`} />
        </div>
      </div>,
    );
  }
}

export default withMetricsContext(QualityGateCondition);
