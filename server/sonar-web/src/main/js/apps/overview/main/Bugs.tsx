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
import BugIcon from 'sonar-ui-common/components/icons/BugIcon';
import { translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import DocTooltip from '../../../components/docs/DocTooltip';
import DateFromNow from '../../../components/intl/DateFromNow';
import { isLongLivingBranch } from '../../../helpers/branches';
import ApplicationLeakPeriodLegend from '../components/ApplicationLeakPeriodLegend';
import LeakPeriodLegend from '../components/LeakPeriodLegend';
import { getMetricName } from '../utils';
import enhance, { ComposedProps } from './enhance';

export class Bugs extends React.PureComponent<ComposedProps> {
  renderHeader() {
    return this.props.renderHeader('Reliability');
  }

  renderTimelineStartDate(historyStartDate?: Date) {
    if (!historyStartDate) {
      return undefined;
    }
    return (
      <DateFromNow date={historyStartDate}>
        {fromNow => (
          <span className="overview-domain-timeline-date">
            {translateWithParameters('overview.started_x', fromNow)}
          </span>
        )}
      </DateFromNow>
    );
  }

  renderTimeline(range: string, historyStartDate?: Date) {
    return this.props.renderTimeline('bugs', range, this.renderTimelineStartDate(historyStartDate));
  }

  renderLeak() {
    const { branchLike, component, leakPeriod } = this.props;
    if (!leakPeriod) {
      return null;
    }

    return (
      <div className="overview-domain-leak">
        {component.qualifier === 'APP' ? (
          <ApplicationLeakPeriodLegend
            branch={isLongLivingBranch(branchLike) ? branchLike : undefined}
            component={component}
          />
        ) : (
          <LeakPeriodLegend period={leakPeriod} />
        )}

        <div className="overview-domain-measures">
          <div className="overview-domain-measure">
            <div className="overview-domain-measure-value">
              <span className="offset-left">{this.props.renderIssues('new_bugs', 'BUG')}</span>
              {this.props.renderRating('new_reliability_rating')}
            </div>
            <div className="overview-domain-measure-label">
              <BugIcon className="little-spacer-right" />
              {getMetricName('new_bugs')}
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
              <span className="offset-left">{this.props.renderIssues('bugs', 'BUG')}</span>
              {this.props.renderRating('reliability_rating')}
            </div>
            <div className="overview-domain-measure-label display-flex-center display-flex-justify-center">
              <BugIcon className="little-spacer-right " />
              {getMetricName('bugs')}
              <DocTooltip
                className="little-spacer-left"
                doc={import(/* webpackMode: "eager" */ 'Docs/tooltips/metrics/bugs.md')}
              />
            </div>
            {this.props.renderHistoryLink('bugs')}
          </div>
        </div>
        {this.renderTimeline('before', this.props.historyStartDate)}
      </div>
    );
  }

  render() {
    const { measures } = this.props;
    const bugsMeasure = measures.find(measure => measure.metric.key === 'bugs');
    if (!bugsMeasure) {
      return null;
    }
    return (
      <div className="overview-card">
        {this.renderHeader()}

        <div className="overview-domain-panel">
          {this.renderNutshell()}
          {this.renderLeak()}
        </div>
      </div>
    );
  }
}

export default enhance(Bugs);
