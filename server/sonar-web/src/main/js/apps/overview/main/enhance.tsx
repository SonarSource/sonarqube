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
import { Link } from 'react-router';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import HistoryIcon from 'sonar-ui-common/components/icons/HistoryIcon';
import Rating from 'sonar-ui-common/components/ui/Rating';
import { getLocalizedMetricName, translate } from 'sonar-ui-common/helpers/l10n';
import { formatMeasure } from 'sonar-ui-common/helpers/measures';
import { getWrappedDisplayName } from '../../../components/hoc/utils';
import DrilldownLink from '../../../components/shared/DrilldownLink';
import { getBranchLikeQuery } from '../../../helpers/branches';
import {
  getPeriodValue,
  getRatingTooltip,
  getShortType,
  isDiffMetric
} from '../../../helpers/measures';
import { getPeriodDate } from '../../../helpers/periods';
import {
  getComponentDrilldownUrl,
  getComponentIssuesUrl,
  getMeasureHistoryUrl
} from '../../../helpers/urls';
import Timeline from '../components/Timeline';

export interface EnhanceProps {
  branchLike?: T.BranchLike;
  component: T.Component;
  measures: T.MeasureEnhanced[];
  leakPeriod?: T.Period;
  history?: {
    [metric: string]: Array<{ date: Date; value?: string }>;
  };
  historyStartDate?: Date;
}

export interface ComposedProps extends EnhanceProps {
  getValue: (measure: T.MeasureEnhanced) => string | undefined;
  renderHeader: (domain: string, label?: string) => React.ReactNode;
  renderMeasure: (metricKey: string, tooltip?: React.ReactNode) => React.ReactNode;
  renderRating: (metricKey: string) => React.ReactNode;
  renderIssues: (metric: string, type: T.IssueType) => React.ReactNode;
  renderHistoryLink: (metricKey: string) => React.ReactNode;
  renderTimeline: (metricKey: string, range: string, children?: React.ReactNode) => React.ReactNode;
}

export default function enhance(ComposedComponent: React.ComponentType<ComposedProps>) {
  return class extends React.PureComponent<EnhanceProps> {
    static displayName = getWrappedDisplayName(ComposedComponent, 'enhance');

    getValue = (measure: T.MeasureEnhanced) => {
      const { leakPeriod } = this.props;
      if (!measure) {
        return '0';
      }
      return isDiffMetric(measure.metric.key)
        ? getPeriodValue(measure, leakPeriod ? leakPeriod.index : 0)
        : measure.value;
    };

    renderHeader = (domain: string, label?: string) => {
      const { branchLike, component } = this.props;
      label = label !== undefined ? label : translate('metric_domain', domain);
      return (
        <div className="overview-card-header">
          <div className="overview-title">
            <span>{label}</span>
            <Link
              className="spacer-left small"
              to={getComponentDrilldownUrl({
                componentKey: component.key,
                metric: domain,
                branchLike
              })}>
              {translate('layout.measures')}
            </Link>
          </div>
        </div>
      );
    };

    renderMeasure = (metricKey: string, tooltip?: React.ReactNode) => {
      const { branchLike, measures, component } = this.props;
      const measure = measures.find(measure => measure.metric.key === metricKey);
      if (!measure) {
        return null;
      }

      return (
        <div className="overview-domain-measure">
          <div className="overview-domain-measure-value">
            <DrilldownLink branchLike={branchLike} component={component.key} metric={metricKey}>
              <span className="js-overview-main-tests">
                {formatMeasure(measure.value, getShortType(measure.metric.type))}
              </span>
            </DrilldownLink>
          </div>

          <div className="overview-domain-measure-label display-flex-center display-flex-justify-center">
            {getLocalizedMetricName(measure.metric)}
            {tooltip}
            {this.renderHistoryLink(measure.metric.key)}
          </div>
        </div>
      );
    };

    renderRating = (metricKey: string) => {
      const { branchLike, component, measures } = this.props;
      const measure = measures.find(measure => measure.metric.key === metricKey);
      if (!measure) {
        return null;
      }

      const value = this.getValue(measure);
      const title = value && getRatingTooltip(metricKey, value);
      return (
        <Tooltip overlay={title}>
          <div className="overview-domain-measure-sup">
            <DrilldownLink
              branchLike={branchLike}
              className="link-no-underline"
              component={component.key}
              metric={metricKey}>
              <Rating value={value} />
            </DrilldownLink>
          </div>
        </Tooltip>
      );
    };

    renderIssues = (metric: string, type: string) => {
      const { branchLike, measures, component } = this.props;
      const measure = measures.find(measure => measure.metric.key === metric);
      if (!measure) {
        return <span className="big">—</span>;
      }

      const value = this.getValue(measure);
      const params = { ...getBranchLikeQuery(branchLike), resolved: 'false', types: type };
      if (isDiffMetric(metric)) {
        Object.assign(params, { sinceLeakPeriod: 'true' });
      }

      return (
        <Link to={getComponentIssuesUrl(component.key, params)}>
          {formatMeasure(value, 'SHORT_INT')}
        </Link>
      );
    };

    renderHistoryLink = (metricKey: string) => {
      const linkClass = 'overview-domain-measure-history-link';
      return (
        <Link
          className={linkClass}
          to={getMeasureHistoryUrl(this.props.component.key, metricKey, this.props.branchLike)}>
          <HistoryIcon />
          <span>{translate('project_activity.page')}</span>
        </Link>
      );
    };

    renderTimeline = (metricKey: string, range: 'before' | 'after', children?: React.ReactNode) => {
      if (!this.props.history) {
        return null;
      }
      const history = this.props.history[metricKey];
      if (!history) {
        return null;
      }
      const props = { history, [range]: getPeriodDate(this.props.leakPeriod) };
      return (
        <div className="overview-domain-timeline">
          <Timeline {...props} />
          {children}
        </div>
      );
    };

    render() {
      return (
        <ComposedComponent
          {...this.props}
          getValue={this.getValue}
          renderHeader={this.renderHeader}
          renderHistoryLink={this.renderHistoryLink}
          renderIssues={this.renderIssues}
          renderMeasure={this.renderMeasure}
          renderRating={this.renderRating}
          renderTimeline={this.renderTimeline}
        />
      );
    }
  };
}
