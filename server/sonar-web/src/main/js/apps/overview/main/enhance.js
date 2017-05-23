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
import React from 'react';
import { Link } from 'react-router';
import moment from 'moment';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import Rating from './../../../components/ui/Rating';
import Timeline from '../components/Timeline';
import {
  formatMeasure,
  formatMeasureVariation,
  isDiffMetric,
  getPeriodValue,
  getShortType,
  getRatingTooltip
} from '../../../helpers/measures';
import { translateWithParameters } from '../../../helpers/l10n';
import { getPeriodDate } from '../../../helpers/periods';
import { getComponentIssuesUrl } from '../../../helpers/urls';

export default function enhance(ComposedComponent) {
  return class extends React.PureComponent {
    static displayName = `enhance(${ComposedComponent.displayName})}`;

    getValue(measure) {
      const { leakPeriod } = this.props;

      if (!measure) {
        return 0;
      }

      return isDiffMetric(measure.metric.key)
        ? getPeriodValue(measure, leakPeriod.index)
        : measure.value;
    }

    renderHeader(domain, label) {
      const { component } = this.props;
      const domainUrl = {
        pathname: `/component_measures/domain/${domain}`,
        query: { id: component.key }
      };

      return (
        <div className="overview-card-header">
          <div className="overview-title">
            <Link to={domainUrl}>{label}</Link>
          </div>
        </div>
      );
    }

    renderMeasure(metricKey) {
      const { measures, component } = this.props;
      const measure = measures.find(measure => measure.metric.key === metricKey);

      if (measure == null) {
        return null;
      }

      return (
        <div className="overview-domain-measure">
          <div className="overview-domain-measure-value">
            <DrilldownLink component={component.key} metric={metricKey}>
              <span className="js-overview-main-tests">
                {formatMeasure(measure.value, getShortType(measure.metric.type))}
              </span>
            </DrilldownLink>
          </div>

          <div className="overview-domain-measure-label">
            {measure.metric.name}
          </div>
        </div>
      );
    }

    renderMeasureVariation(metricKey, customLabel) {
      const NO_VALUE = '—';
      const { measures, leakPeriod } = this.props;
      const measure = measures.find(measure => measure.metric.key === metricKey);
      const periodValue = getPeriodValue(measure, leakPeriod.index);
      const formatted = periodValue != null
        ? formatMeasureVariation(periodValue, getShortType(measure.metric.type))
        : NO_VALUE;
      return (
        <div className="overview-domain-measure">
          <div className="overview-domain-measure-value">
            {formatted}
          </div>

          <div className="overview-domain-measure-label">
            {customLabel || measure.metric.name}
          </div>
        </div>
      );
    }
    renderRating(metricKey) {
      const { component, measures } = this.props;
      const measure = measures.find(measure => measure.metric.key === metricKey);
      if (!measure) {
        return null;
      }
      const value = this.getValue(measure);
      const title = getRatingTooltip(metricKey, value);
      return (
        <div className="overview-domain-measure-sup" title={title} data-toggle="tooltip">
          <DrilldownLink className="link-no-underline" component={component.key} metric={metricKey}>
            <Rating value={value} />
          </DrilldownLink>
        </div>
      );
    }
    renderIssues(metric, type) {
      const { measures, component } = this.props;
      const measure = measures.find(measure => measure.metric.key === metric);
      const value = this.getValue(measure);
      const params = {
        resolved: 'false',
        types: type
      };
      if (isDiffMetric(metric)) {
        Object.assign(params, { sinceLeakPeriod: 'true' });
      }
      const formattedAnalysisDate = moment(component.analysisDate).format('LLL');
      const tooltip = translateWithParameters('widget.as_calculated_on_x', formattedAnalysisDate);
      return (
        <Link to={getComponentIssuesUrl(component.key, params)}>
          <span title={tooltip} data-toggle="tooltip">
            {formatMeasure(value, 'SHORT_INT')}
          </span>
        </Link>
      );
    }
    renderTimeline(metricKey, range, children) {
      if (!this.props.history) {
        return null;
      }
      const history = this.props.history[metricKey];
      if (!history) {
        return null;
      }
      const props = {
        history,
        [range]: getPeriodDate(this.props.leakPeriod)
      };
      return (
        <div className="overview-domain-timeline">
          <Timeline {...props} />
          {children}
        </div>
      );
    }
    render() {
      return (
        <ComposedComponent
          {...this.props}
          getValue={this.getValue.bind(this)}
          renderHeader={this.renderHeader.bind(this)}
          renderMeasure={this.renderMeasure.bind(this)}
          renderMeasureVariation={this.renderMeasureVariation.bind(this)}
          renderRating={this.renderRating.bind(this)}
          renderIssues={this.renderIssues.bind(this)}
          renderTimeline={this.renderTimeline.bind(this)}
        />
      );
    }
  };
}
