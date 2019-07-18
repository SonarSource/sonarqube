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
import SecurityHotspotIcon from 'sonar-ui-common/components/icons/SecurityHotspotIcon';
import VulnerabilityIcon from 'sonar-ui-common/components/icons/VulnerabilityIcon';
import DocTooltip from '../../../components/docs/DocTooltip';
import { getMetricName } from '../utils';
import enhance, { ComposedProps } from './enhance';

export class VulnerabiltiesAndHotspots extends React.PureComponent<ComposedProps> {
  renderHeader() {
    return this.props.renderHeader('Security');
  }

  renderTimeline(range: string) {
    return this.props.renderTimeline('vulnerabilities', range);
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
              <span className="offset-left">
                {this.props.renderIssues('new_vulnerabilities', 'VULNERABILITY')}
              </span>
              {this.props.renderRating('new_security_rating')}
            </div>
            <div className="overview-domain-measure-label">
              <VulnerabilityIcon className="little-spacer-right" />
              {getMetricName('new_vulnerabilities')}
            </div>
          </div>
          <div className="overview-domain-measure">
            <div className="overview-domain-measure-value">
              {this.props.renderIssues('new_security_hotspots', 'SECURITY_HOTSPOT')}
            </div>
            <div className="overview-domain-measure-label">
              <SecurityHotspotIcon className="little-spacer-right" />
              {getMetricName('new_security_hotspots')}
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
              <span className="offset-left">
                {this.props.renderIssues('vulnerabilities', 'VULNERABILITY')}
              </span>
              {this.props.renderRating('security_rating')}
            </div>
            <div className="overview-domain-measure-label display-flex-center display-flex-justify-center">
              <VulnerabilityIcon className="little-spacer-right" />
              {getMetricName('vulnerabilities')}
              <DocTooltip
                className="little-spacer-left"
                doc={import(/* webpackMode: "eager" */ 'Docs/tooltips/metrics/vulnerabilities.md')}
              />
            </div>
            {this.props.renderHistoryLink('vulnerabilities')}
          </div>
          <div className="overview-domain-measure">
            <div className="overview-domain-measure-value">
              {this.props.renderIssues('security_hotspots', 'SECURITY_HOTSPOT')}
            </div>
            <div className="overview-domain-measure-label display-flex-center display-flex-justify-center">
              <SecurityHotspotIcon className="little-spacer-right" />
              {getMetricName('security_hotspots')}
              <DocTooltip
                className="little-spacer-left"
                doc={import(/* webpackMode: "eager" */ 'Docs/tooltips/metrics/security-hotspots.md')}
              />
            </div>
            {this.props.renderHistoryLink('security_hotspots')}
          </div>
        </div>
        {this.renderTimeline('before')}
      </div>
    );
  }

  render() {
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

export default enhance(VulnerabiltiesAndHotspots);
