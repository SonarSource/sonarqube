/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import _ from 'underscore';
import moment from 'moment';
import React from 'react';

import { IssuesLink } from './issues-link';
import { getComponentDrilldownUrl } from '../../helpers/urls';


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


export const DrilldownLink = React.createClass({
  isIssueMeasure() {
    return ISSUE_MEASURES.indexOf(this.props.metric) !== -1;
  },

  propsToIssueParams() {
    const params = {};

    if (this.props.periodDate) {
      params.createdAfter = moment(this.props.periodDate).format('YYYY-MM-DDTHH:mm:ssZZ');
    }

    switch (this.props.metric) {
      case 'blocker_violations':
      case 'new_blocker_violations':
        _.extend(params, { resolved: 'false', severities: 'BLOCKER' });
        break;
      case 'critical_violations':
      case 'new_critical_violations':
        _.extend(params, { resolved: 'false', severities: 'CRITICAL' });
        break;
      case 'major_violations':
      case 'new_major_violations':
        _.extend(params, { resolved: 'false', severities: 'MAJOR' });
        break;
      case 'minor_violations':
      case 'new_minor_violations':
        _.extend(params, { resolved: 'false', severities: 'MINOR' });
        break;
      case 'info_violations':
      case 'new_info_violations':
        _.extend(params, { resolved: 'false', severities: 'INFO' });
        break;
      case 'open_issues':
        _.extend(params, { resolved: 'false', statuses: 'OPEN' });
        break;
      case 'reopened_issues':
        _.extend(params, { resolved: 'false', statuses: 'REOPENED' });
        break;
      case 'confirmed_issues':
        _.extend(params, { resolved: 'false', statuses: 'CONFIRMED' });
        break;
      case 'false_positive_issues':
        _.extend(params, { resolutions: 'FALSE-POSITIVE' });
        break;
      case 'code_smells':
      case 'new_code_smells':
        _.extend(params, { resolved: 'false', types: 'CODE_SMELL' });
        break;
      case 'bugs':
      case 'new_bugs':
        _.extend(params, { resolved: 'false', types: 'BUG' });
        break;
      case 'vulnerabilities':
      case 'new_vulnerabilities':
        _.extend(params, { resolved: 'false', types: 'VULNERABILITY' });
        break;
      default:
        _.extend(params, { resolved: 'false' });
    }
    return params;
  },

  renderIssuesLink() {
    return <IssuesLink component={this.props.component} params={this.propsToIssueParams()}>
      {this.props.children}
    </IssuesLink>;
  },

  render() {
    if (this.isIssueMeasure()) {
      return this.renderIssuesLink();
    }

    const url = getComponentDrilldownUrl(this.props.component, this.props.metric);
    return <a className={this.props.className} href={url}>{this.props.children}</a>;
  }
});
