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
import IssueTypeIcon from '../../../components/icon-mappers/IssueTypeIcon';
import MeasureIndicator from '../../../components/measure/MeasureIndicator';
import {
  DEFAULT_ISSUES_QUERY,
  isIssueMeasure,
  propsToIssueParams,
} from '../../../components/shared/utils';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { formatMeasure, isDiffMetric, localizeMetric } from '../../../helpers/measures';
import { getOperatorLabel } from '../../../helpers/qualityGates';
import {
  getComponentDrilldownUrl,
  getComponentIssuesUrl,
  getComponentSecurityHotspotsUrl,
} from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { IssueType } from '../../../types/issues';
import { MetricKey, MetricType } from '../../../types/metrics';
import { QualityGateStatusConditionEnhanced } from '../../../types/quality-gates';
import { Component, Dict } from '../../../types/types';
import { RATING_TO_SEVERITIES_MAPPING } from '../utils';

interface Props {
  branchLike?: BranchLike;
  component: Pick<Component, 'key'>;
  condition: QualityGateStatusConditionEnhanced;
}

export default class QualityGateCondition extends React.PureComponent<Props> {
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
    const isDiff = isDiffMetric(metric.key);

    const subText =
      !isDiff && condition.period != null
        ? `${localizeMetric(metric.key)} ${translate('quality_gates.conditions.new_code')}`
        : localizeMetric(metric.key);

    if (metric.type !== MetricType.Rating) {
      const actual = (condition.period ? measure.period?.value : measure.value) as string;
      const formattedValue = formatMeasure(actual, metric.type, {
        decimal: 2,
        omitExtraDecimalZeros: metric.type === MetricType.Percent,
      });
      return `${formattedValue} ${subText}`;
    }

    return subText;
  };

  render() {
    const { condition } = this.props;
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
