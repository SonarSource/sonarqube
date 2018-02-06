/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import enhance from './enhance';
import Tooltip from '../../../components/controls/Tooltip';
import DateFromNow from '../../../components/intl/DateFromNow';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
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
    const { branch, measures, component } = this.props;
    const measure = measures.find(measure => measure.metric.key === metric);
    const value = this.props.getValue(measure);
    const params = { branch, resolved: 'false', facetMode: 'effort', types: type };

    if (isDiffMetric(metric)) {
      Object.assign(params, { sinceLeakPeriod: 'true' });
    }

    const tooltip = (
      <DateTimeFormatter date={component.analysisDate}>
        {formattedAnalysisDate => (
          <span>{translateWithParameters('widget.as_calculated_on_x', formattedAnalysisDate)}</span>
        )}
      </DateTimeFormatter>
    );
    return (
      <Tooltip overlay={tooltip} placement="top">
        <Link to={getComponentIssuesUrl(component.key, params)}>
          {formatMeasure(value, 'SHORT_WORK_DUR')}
        </Link>
      </Tooltip>
    );
  }

  renderTimelineStartDate() {
    if (!this.props.historyStartDate) {
      return null;
    }
    return (
      <DateFromNow date={this.props.historyStartDate}>
        {fromNow => (
          <span className="overview-domain-timeline-date">
            {translateWithParameters('overview.started_x', fromNow)}
          </span>
        )}
      </DateFromNow>
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
            <div className="overview-domain-measure-label">{getMetricName('new_effort')}</div>
          </div>
          <div className="overview-domain-measure">
            <div className="overview-domain-measure-value">
              {this.props.renderIssues('new_code_smells', 'CODE_SMELL')}
            </div>
            <div className="overview-domain-measure-label">
              <CodeSmellIcon className="little-spacer-right" />
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
            <div className="overview-domain-measure-value">
              {this.renderDebt('sqale_index', 'CODE_SMELL')}
              {this.props.renderRating('sqale_rating')}
            </div>
            <div className="overview-domain-measure-label">
              {getMetricName('effort')}
              {this.props.renderHistoryLink('sqale_index')}
            </div>
          </div>
          <div className="overview-domain-measure">
            <div className="overview-domain-measure-value">
              {this.props.renderIssues('code_smells', 'CODE_SMELL')}
            </div>
            <div className="overview-domain-measure-label offset-left">
              <CodeSmellIcon className="little-spacer-right " />
              {getMetricName('code_smells')}
              {this.props.renderHistoryLink('code_smells')}
            </div>
          </div>
        </div>

        {this.renderTimeline('before', true)}
      </div>
    );
  }

  render() {
    const { measures } = this.props;
    const codeSmellsMeasure = measures.find(measure => measure.metric.key === 'code_smells');
    if (codeSmellsMeasure == null) {
      return null;
    }
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
