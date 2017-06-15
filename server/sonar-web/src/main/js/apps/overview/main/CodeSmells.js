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
import moment from 'moment';
import React from 'react';
import { Link } from 'react-router';
import enhance from './enhance';
import { getMetricName } from '../helpers/metrics';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { formatMeasure, isDiffMetric } from '../../../helpers/measures';
import { getComponentIssuesUrl } from '../../../helpers/urls';
import CodeSmellIcon from '../../../components/icons-components/CodeSmellIcon';

class CodeSmells extends React.PureComponent {
  renderHeader() {
    return this.props.renderHeader('Maintainability', translate('metric.code_smells.name'));
  }

  renderDebt(metric, type) {
    const { measures, component } = this.props;
    const measure = measures.find(measure => measure.metric.key === metric);
    const value = this.props.getValue(measure);
    const params = { resolved: 'false', facetMode: 'effort', types: type };

    if (isDiffMetric(metric)) {
      Object.assign(params, { sinceLeakPeriod: 'true' });
    }

    const formattedAnalysisDate = moment(component.analysisDate).format('LLL');
    const tooltip = translateWithParameters('widget.as_calculated_on_x', formattedAnalysisDate);

    return (
      <Link to={getComponentIssuesUrl(component.key, params)}>
        <span title={tooltip} data-toggle="tooltip">
          {formatMeasure(value, 'SHORT_WORK_DUR')}
        </span>
      </Link>
    );
  }

  renderTimelineStartDate() {
    const momentDate = moment(this.props.historyStartDate);
    const fromNow = momentDate.fromNow();
    return (
      <span className="overview-domain-timeline-date">
        {translateWithParameters('overview.started_x', fromNow)}
      </span>
    );
  }

  renderTimeline(range, displayDate) {
    return this.props.renderTimeline(
      'sqale_index',
      range,
      displayDate ? this.renderTimelineStartDate() : null
    );
  }

  renderLeak() {
    const { leakPeriod } = this.props;

    if (leakPeriod == null) {
      return null;
    }

    return (
      <div className="overview-domain-leak">
        <div className="overview-domain-measures">
          <div className="overview-domain-measure">
            <div className="overview-domain-measure-value">
              <span style={{ marginLeft: 30 }}>
                {this.renderDebt('new_technical_debt', 'CODE_SMELL')}
              </span>
              {this.props.renderRating('new_maintainability_rating')}
            </div>
            <div className="overview-domain-measure-label">
              {getMetricName('new_effort')}
            </div>
          </div>

          <div className="overview-domain-measure">
            <div className="overview-domain-measure-value">
              {this.props.renderIssues('new_code_smells', 'CODE_SMELL')}
            </div>
            <div className="overview-domain-measure-label">
              <span className="little-spacer-right"><CodeSmellIcon /></span>
              {getMetricName('new_code_smells')}
            </div>
          </div>
        </div>

        {this.renderTimeline('after')}
      </div>
    );
  }

  renderNutshell() {
    return (
      <div className="overview-domain-nutshell">
        <div className="overview-domain-measures">

          <div className="overview-domain-measure">
            <div className="display-inline-block text-middle" style={{ paddingLeft: 56 }}>
              <div className="overview-domain-measure-value">
                {this.renderDebt('sqale_index', 'CODE_SMELL')}
                {this.props.renderRating('sqale_rating')}
              </div>
              <div className="overview-domain-measure-label">
                {getMetricName('effort')}
              </div>
            </div>
          </div>

          <div className="overview-domain-measure">
            <div className="display-inline-block text-middle">
              <div className="overview-domain-measure-value">
                {this.props.renderIssues('code_smells', 'CODE_SMELL')}
              </div>
              <div className="overview-domain-measure-label">
                <span className="little-spacer-right"><CodeSmellIcon /></span>
                {getMetricName('code_smells')}
              </div>
            </div>
          </div>
        </div>

        {this.renderTimeline('before', true)}
      </div>
    );
  }

  render() {
    return (
      <div className="overview-card" id="overview-code-smells">
        {this.renderHeader()}

        <div className="overview-domain-panel">
          {this.renderNutshell()}
          {this.renderLeak()}
        </div>
      </div>
    );
  }
}

export default enhance(CodeSmells);
