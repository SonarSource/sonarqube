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
import enhance from './enhance';
import LeakPeriodLegend from '../components/LeakPeriodLegend';
import { getMetricName } from '../helpers/metrics';
import { translate } from '../../../helpers/l10n';
import BugIcon from '../../../components/icons-components/BugIcon';
import VulnerabilityIcon from '../../../components/icons-components/VulnerabilityIcon';

class BugsAndVulnerabilities extends React.PureComponent {
  renderHeader() {
    const { component } = this.props;
    const bugsDomainUrl = {
      pathname: '/component_measures/domain/Reliability',
      query: { id: component.key }
    };
    const vulnerabilitiesDomainUrl = {
      pathname: '/component_measures/domain/Security',
      query: { id: component.key }
    };

    return (
      <div className="overview-card-header">
        <div className="overview-title">
          <Link to={bugsDomainUrl}>{translate('metric.bugs.name')}</Link>
          {' & '}
          <Link to={vulnerabilitiesDomainUrl}>{translate('metric.vulnerabilities.name')}</Link>
        </div>
      </div>
    );
  }

  renderLeak() {
    const { leakPeriod } = this.props;

    if (leakPeriod == null) {
      return null;
    }

    return (
      <div className="overview-domain-leak">
        <LeakPeriodLegend period={leakPeriod} />

        <div className="overview-domain-measures">
          <div className="overview-domain-measure">
            <div className="overview-domain-measure-value">
              <span style={{ marginLeft: 30 }}>
                {this.props.renderIssues('new_bugs', 'BUG')}
              </span>
              {this.props.renderRating('new_reliability_rating')}
            </div>
            <div className="overview-domain-measure-label">
              <span className="little-spacer-right"><BugIcon /></span>
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
              <span className="little-spacer-right"><VulnerabilityIcon /></span>
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
            <div className="display-inline-block text-middle" style={{ paddingLeft: 56 }}>
              <div className="overview-domain-measure-value">
                {this.props.renderIssues('bugs', 'BUG')}
                {this.props.renderRating('reliability_rating')}
              </div>
              <div className="overview-domain-measure-label">
                <span className="little-spacer-right"><BugIcon /></span>
                {getMetricName('bugs')}
              </div>
            </div>
          </div>

          <div className="overview-domain-measure">
            <div className="display-inline-block text-middle">
              <div className="overview-domain-measure-value">
                {this.props.renderIssues('vulnerabilities', 'VULNERABILITY')}
                {this.props.renderRating('security_rating')}
              </div>
              <div className="overview-domain-measure-label">
                <span className="little-spacer-right"><VulnerabilityIcon /></span>
                {getMetricName('vulnerabilities')}
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  render() {
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
