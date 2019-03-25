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
import { getComponentDrilldownUrl, getComponentIssuesUrl } from '../../helpers/urls';
import { getBranchLikeQuery } from '../../helpers/branches';

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

const issueParamsPerMetric: T.Dict<T.Dict<string>> = {
  blocker_violations: { resolved: 'false', severities: 'BLOCKER' },
  new_blocker_violations: { resolved: 'false', severities: 'BLOCKER' },
  critical_violations: { resolved: 'false', severities: 'CRITICAL' },
  new_critical_violations: { resolved: 'false', severities: 'CRITICAL' },
  major_violations: { resolved: 'false', severities: 'MAJOR' },
  new_major_violations: { resolved: 'false', severities: 'MAJOR' },
  minor_violations: { resolved: 'false', severities: 'MINOR' },
  new_minor_violations: { resolved: 'false', severities: 'MINOR' },
  info_violations: { resolved: 'false', severities: 'INFO' },
  new_info_violations: { resolved: 'false', severities: 'INFO' },
  open_issues: { resolved: 'false', statuses: 'OPEN' },
  reopened_issues: { resolved: 'false', statuses: 'REOPENED' },
  confirmed_issues: { resolved: 'false', statuses: 'CONFIRMED' },
  false_positive_issues: { resolutions: 'FALSE-POSITIVE' },
  code_smells: { resolved: 'false', types: 'CODE_SMELL' },
  new_code_smells: { resolved: 'false', types: 'CODE_SMELL' },
  bugs: { resolved: 'false', types: 'BUG' },
  new_bugs: { resolved: 'false', types: 'BUG' },
  vulnerabilities: { resolved: 'false', types: 'VULNERABILITY' },
  new_vulnerabilities: { resolved: 'false', types: 'VULNERABILITY' }
};

interface Props {
  branchLike?: T.BranchLike;
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
    const params: T.Dict<string | boolean> = {
      ...(issueParamsPerMetric[this.props.metric] || { resolved: 'false' })
    };

    if (this.props.sinceLeakPeriod) {
      params.sinceLeakPeriod = true;
    }

    return params;
  };

  renderIssuesLink = () => {
    const url = getComponentIssuesUrl(this.props.component, {
      ...this.propsToIssueParams(),
      ...getBranchLikeQuery(this.props.branchLike)
    });

    return (
      <Link className={this.props.className} to={url}>
        {this.props.children}
      </Link>
    );
  };

  render() {
    if (this.isIssueMeasure()) {
      return this.renderIssuesLink();
    }

    const url = getComponentDrilldownUrl({
      componentKey: this.props.component,
      metric: this.props.metric,
      branchLike: this.props.branchLike,
      listView: true
    });
    return (
      <Link className={this.props.className} to={url}>
        {this.props.children}
      </Link>
    );
  }
}
