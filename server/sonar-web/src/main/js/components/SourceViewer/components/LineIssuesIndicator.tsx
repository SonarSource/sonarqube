/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import * as classNames from 'classnames';
import { uniq } from 'lodash';
import * as React from 'react';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import IssueIcon from 'sonar-ui-common/components/icons/IssueIcon';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { sortByType } from '../../../helpers/issues';

export interface LineIssuesIndicatorProps {
  issues: T.Issue[];
  issuesOpen?: boolean;
  line: T.SourceLine;
  onClick: () => void;
}

export function LineIssuesIndicator(props: LineIssuesIndicatorProps) {
  const { issues, issuesOpen, line } = props;
  const hasIssues = issues.length > 0;
  const className = classNames('source-meta', 'source-line-issues', {
    'source-line-with-issues': hasIssues
  });

  if (!hasIssues) {
    return <td className={className} data-line-number={line.line} />;
  }

  const mostImportantIssue = sortByType(issues)[0];
  const issueTypes = uniq(issues.map(i => i.type));

  let tooltipContent;
  if (issueTypes.length > 1) {
    tooltipContent = translate('source_viewer.issues_on_line.multiple_issues');
  } else if (issues.length === 1) {
    tooltipContent = translateWithParameters(
      'source_viewer.issues_on_line.issue_of_type_X',
      translate('issue.type', mostImportantIssue.type)
    );
  } else {
    tooltipContent = translateWithParameters(
      'source_viewer.issues_on_line.X_issues_of_type_Y',
      issues.length,
      translate('issue.type', mostImportantIssue.type, 'plural')
    );
  }

  return (
    <td className={className} data-line-number={line.line}>
      <span
        aria-label={translate('source_viewer.issues_on_line', issuesOpen ? 'hide' : 'show')}
        onClick={(e: React.MouseEvent<HTMLElement>) => {
          e.preventDefault();
          e.currentTarget.blur();
          props.onClick();
        }}
        role="button"
        tabIndex={0}>
        <Tooltip overlay={tooltipContent}>
          <IssueIcon type={mostImportantIssue.type} />
        </Tooltip>
        {issues.length > 1 && <span className="source-line-issues-counter">{issues.length}</span>}
      </span>
    </td>
  );
}

export default React.memo(LineIssuesIndicator);
