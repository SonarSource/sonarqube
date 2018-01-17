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
import * as React from 'react';
import { Link } from 'react-router';
import { getComponentDrilldownUrl, getComponentIssuesUrl } from '../../helpers/urls';

const ISSUE_MEASURES = [
  'violations',
  'new_violations',
  'blocker_violations',
  'critical_violations',
  'major_violations',
  'minor_violations',
  'info_violations',
  'new_blocker_violations',
  'new_critical_violations',
  'new_major_violations',
  'new_minor_violations',
  'new_info_violations',
  'open_issues',
  'reopened_issues',
  'confirmed_issues',
  'false_positive_issues',
  'code_smells',
  'new_code_smells',
  'bugs',
  'new_bugs',
  'vulnerabilities',
  'new_vulnerabilities'
];

interface Props {
  branch?: string;
  children?: React.ReactNode;
  className?: string;
  component: string;
  metric: string;
  sinceLeakPeriod?: boolean;
}

export default class DrilldownLink extends React.PureComponent<Props> {
  isIssueMeasure = () => {
    return ISSUE_MEASURES.indexOf(this.props.metric) !== -1;
  };

  propsToIssueParams = () => {
    const params: { [key: string]: string | boolean } = {};

    if (this.props.sinceLeakPeriod) {
      params.sinceLeakPeriod = true;
    }

    switch (this.props.metric) {
      case 'blocker_violations':
      case 'new_blocker_violations':
        Object.assign(params, { resolved: 'false', severities: 'BLOCKER' });
        break;
      case 'critical_violations':
      case 'new_critical_violations':
        Object.assign(params, { resolved: 'false', severities: 'CRITICAL' });
        break;
      case 'major_violations':
      case 'new_major_violations':
        Object.assign(params, { resolved: 'false', severities: 'MAJOR' });
        break;
      case 'minor_violations':
      case 'new_minor_violations':
        Object.assign(params, { resolved: 'false', severities: 'MINOR' });
        break;
      case 'info_violations':
      case 'new_info_violations':
        Object.assign(params, { resolved: 'false', severities: 'INFO' });
        break;
      case 'open_issues':
        Object.assign(params, { resolved: 'false', statuses: 'OPEN' });
        break;
      case 'reopened_issues':
        Object.assign(params, { resolved: 'false', statuses: 'REOPENED' });
        break;
      case 'confirmed_issues':
        Object.assign(params, { resolved: 'false', statuses: 'CONFIRMED' });
        break;
      case 'false_positive_issues':
        Object.assign(params, { resolutions: 'FALSE-POSITIVE' });
        break;
      case 'code_smells':
      case 'new_code_smells':
        Object.assign(params, { resolved: 'false', types: 'CODE_SMELL' });
        break;
      case 'bugs':
      case 'new_bugs':
        Object.assign(params, { resolved: 'false', types: 'BUG' });
        break;
      case 'vulnerabilities':
      case 'new_vulnerabilities':
        Object.assign(params, { resolved: 'false', types: 'VULNERABILITY' });
        break;
      default:
        Object.assign(params, { resolved: 'false' });
    }
    return params;
  };

  renderIssuesLink = () => {
    const url = getComponentIssuesUrl(this.props.component, {
      ...this.propsToIssueParams(),
      branch: this.props.branch
    });

    return (
      <Link to={url} className={this.props.className}>
        {this.props.children}
      </Link>
    );
  };

  render() {
    if (this.isIssueMeasure()) {
      return this.renderIssuesLink();
    }

    const url = getComponentDrilldownUrl(
      this.props.component,
      this.props.metric,
      this.props.branch
    );
    return (
      <Link to={url} className={this.props.className}>
        {this.props.children}
      </Link>
    );
  }
}
