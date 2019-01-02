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
import enhance, { ComposedProps } from './enhance';
import DateFromNow from '../../../components/intl/DateFromNow';
import { getMetricName } from '../utils';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
import CodeSmellIcon from '../../../components/icons-components/CodeSmellIcon';
import DrilldownLink from '../../../components/shared/DrilldownLink';

export class CodeSmells extends React.PureComponent<ComposedProps> {
  renderHeader() {
    return this.props.renderHeader('Maintainability', translate('metric.code_smells.name'));
  }

  renderDebt(metric: string) {
    const { branchLike, measures, component } = this.props;
    const measure = measures.find(measure => measure.metric.key === metric);
    const value = measure ? this.props.getValue(measure) : undefined;

    return (
      <DrilldownLink branchLike={branchLike} component={component.key} metric={metric}>
        {formatMeasure(value, 'SHORT_WORK_DUR')}
      </DrilldownLink>
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

  renderTimeline(range: string, displayDate?: boolean) {
    return this.props.renderTimeline(
      'sqale_index',
      range,
      displayDate ? this.renderTimelineStartDate() : null
    );
  }

  renderLeak() {
    const { leakPeriod } = this.props;
    if (!leakPeriod) {
      return null;
    }

    return (
      <div className="overview-domain-leak">
        <div className="overview-domain-measures">
          <div className="overview-domain-measure">
            <div className="overview-domain-measure-value">
              <span style={{ marginLeft: 30 }}>{this.renderDebt('new_technical_debt')}</span>
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
              {this.renderDebt('sqale_index')}
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
    if (!codeSmellsMeasure) {
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
