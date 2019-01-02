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
import enhance, { ComposedProps } from './enhance';
import ApplicationLeakPeriodLegend from '../components/ApplicationLeakPeriodLegend';
import BubblesIcon from '../../../components/icons-components/BubblesIcon';
import BugIcon from '../../../components/icons-components/BugIcon';
import LeakPeriodLegend from '../components/LeakPeriodLegend';
import VulnerabilityIcon from '../../../components/icons-components/VulnerabilityIcon';
import { getMetricName } from '../utils';
import { getComponentDrilldownUrl } from '../../../helpers/urls';
import { translate } from '../../../helpers/l10n';
import { isLongLivingBranch } from '../../../helpers/branches';

export class BugsAndVulnerabilities extends React.PureComponent<ComposedProps> {
  renderHeader() {
    const { branchLike, component } = this.props;
    return (
      <div className="overview-card-header">
        <div className="overview-title">
          <span>{translate('metric.bugs.name')}</span>
          <Link
            className="button button-small spacer-left text-text-bottom"
            to={getComponentDrilldownUrl({
              componentKey: component.key,
              metric: 'Reliability',
              branchLike
            })}>
            <BubblesIcon size={14} />
          </Link>
          <span className="big-spacer-left">{translate('metric.vulnerabilities.name')}</span>
          <Link
            className="button button-small spacer-left text-text-bottom"
            to={getComponentDrilldownUrl({
              componentKey: component.key,
              metric: 'Security',
              branchLike
            })}>
            <BubblesIcon size={14} />
          </Link>
        </div>
      </div>
    );
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
              <span style={{ marginLeft: 30 }}>{this.props.renderIssues('new_bugs', 'BUG')}</span>
              {this.props.renderRating('new_reliability_rating')}
            </div>
            <div className="overview-domain-measure-label">
              <BugIcon className="little-spacer-right" />
              {getMetricName('new_bugs')}
            </div>
          </div>
          <div className="overview-domain-measure">
            <div className="overview-domain-measure-value">
              <span style={{ marginLeft: 30 }}>
                {this.props.renderIssues('new_vulnerabilities', 'VULNERABILITY')}
              </span>
              {this.props.renderRating('new_security_rating')}
            </div>
            <div className="overview-domain-measure-label">
              <VulnerabilityIcon className="little-spacer-right" />
              {getMetricName('new_vulnerabilities')}
            </div>
          </div>
        </div>
      </div>
    );
  }

  renderNutshell() {
    return (
      <div className="overview-domain-nutshell">
        <div className="overview-domain-measures">
          <div className="overview-domain-measure">
            <div className="overview-domain-measure-value">
              {this.props.renderIssues('bugs', 'BUG')}
              {this.props.renderRating('reliability_rating')}
            </div>
            <div className="overview-domain-measure-label">
              <BugIcon className="little-spacer-right " />
              {getMetricName('bugs')}
              {this.props.renderHistoryLink('bugs')}
            </div>
          </div>
          <div className="overview-domain-measure">
            <div className="overview-domain-measure-value">
              {this.props.renderIssues('vulnerabilities', 'VULNERABILITY')}
              {this.props.renderRating('security_rating')}
            </div>
            <div className="overview-domain-measure-label">
              <VulnerabilityIcon className="little-spacer-right " />
              {getMetricName('vulnerabilities')}
              {this.props.renderHistoryLink('vulnerabilities')}
            </div>
          </div>
        </div>
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
      <div className="overview-card overview-card-special">
        {this.renderHeader()}

        <div className="overview-domain-panel">
          {this.renderNutshell()}
          {this.renderLeak()}
        </div>
      </div>
    );
  }
}

export default enhance(BugsAndVulnerabilities);
