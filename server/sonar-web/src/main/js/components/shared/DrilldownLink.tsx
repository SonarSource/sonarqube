/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { getBranchLikeQuery } from '../../helpers/branch-like';
import { getComponentDrilldownUrl, getComponentIssuesUrl } from '../../helpers/urls';
import { BranchLike } from '../../types/branch-like';
import { MetricKey } from '../../types/metrics';
import { Dict } from '../../types/types';
import Link from '../common/Link';

const ISSUE_MEASURES = [
  MetricKey.violations,
  MetricKey.new_violations,
  MetricKey.blocker_violations,
  MetricKey.critical_violations,
  MetricKey.major_violations,
  MetricKey.minor_violations,
  MetricKey.info_violations,
  MetricKey.new_blocker_violations,
  MetricKey.new_critical_violations,
  MetricKey.new_major_violations,
  MetricKey.new_minor_violations,
  MetricKey.new_info_violations,
  MetricKey.open_issues,
  MetricKey.reopened_issues,
  MetricKey.confirmed_issues,
  MetricKey.false_positive_issues,
  MetricKey.code_smells,
  MetricKey.new_code_smells,
  MetricKey.bugs,
  MetricKey.new_bugs,
  MetricKey.vulnerabilities,
  MetricKey.new_vulnerabilities,
];

const issueParamsPerMetric: Dict<Dict<string>> = {
  [MetricKey.blocker_violations]: { resolved: 'false', severities: 'BLOCKER' },
  [MetricKey.new_blocker_violations]: { resolved: 'false', severities: 'BLOCKER' },
  [MetricKey.critical_violations]: { resolved: 'false', severities: 'CRITICAL' },
  [MetricKey.new_critical_violations]: { resolved: 'false', severities: 'CRITICAL' },
  [MetricKey.major_violations]: { resolved: 'false', severities: 'MAJOR' },
  [MetricKey.new_major_violations]: { resolved: 'false', severities: 'MAJOR' },
  [MetricKey.minor_violations]: { resolved: 'false', severities: 'MINOR' },
  [MetricKey.new_minor_violations]: { resolved: 'false', severities: 'MINOR' },
  [MetricKey.info_violations]: { resolved: 'false', severities: 'INFO' },
  [MetricKey.new_info_violations]: { resolved: 'false', severities: 'INFO' },
  [MetricKey.open_issues]: { resolved: 'false', statuses: 'OPEN' },
  [MetricKey.reopened_issues]: { resolved: 'false', statuses: 'REOPENED' },
  [MetricKey.confirmed_issues]: { resolved: 'false', statuses: 'CONFIRMED' },
  [MetricKey.false_positive_issues]: { resolutions: 'FALSE-POSITIVE' },
  [MetricKey.code_smells]: { resolved: 'false', types: 'CODE_SMELL' },
  [MetricKey.new_code_smells]: { resolved: 'false', types: 'CODE_SMELL' },
  [MetricKey.bugs]: { resolved: 'false', types: 'BUG' },
  [MetricKey.new_bugs]: { resolved: 'false', types: 'BUG' },
  [MetricKey.vulnerabilities]: { resolved: 'false', types: 'VULNERABILITY' },
  [MetricKey.new_vulnerabilities]: { resolved: 'false', types: 'VULNERABILITY' },
};

interface Props {
  ariaLabel?: string;
  branchLike?: BranchLike;
  children?: React.ReactNode;
  className?: string;
  component: string;
  metric: string;
  inNewCodePeriod?: boolean;
}

export default class DrilldownLink extends React.PureComponent<Props> {
  isIssueMeasure = () => {
    return ISSUE_MEASURES.indexOf(this.props.metric as MetricKey) !== -1;
  };

  propsToIssueParams = () => {
    const params: Dict<string | boolean> = {
      ...(issueParamsPerMetric[this.props.metric] || { resolved: 'false' }),
    };

    if (this.props.inNewCodePeriod) {
      params.inNewCodePeriod = true;
    }

    return params;
  };

  renderIssuesLink = () => {
    const { ariaLabel, className, component, children, branchLike } = this.props;

    const url = getComponentIssuesUrl(component, {
      ...this.propsToIssueParams(),
      ...getBranchLikeQuery(branchLike),
    });

    return (
      <Link aria-label={ariaLabel} className={className} to={url}>
        {children}
      </Link>
    );
  };

  render() {
    if (this.isIssueMeasure()) {
      return this.renderIssuesLink();
    }
    const { ariaLabel, className, metric, component, children, branchLike } = this.props;

    const url = getComponentDrilldownUrl({
      componentKey: component,
      metric,
      branchLike,
      listView: true,
    });
    return (
      <Link aria-label={ariaLabel} className={className} to={url}>
        {children}
      </Link>
    );
  }
}
