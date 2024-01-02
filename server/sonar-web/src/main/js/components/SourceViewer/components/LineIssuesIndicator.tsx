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
import { IssueIndicatorButton, LineIssuesIndicatorIcon, LineMeta } from 'design-system';
import { uniq } from 'lodash';
import * as React from 'react';
import Tooltip from '../../../components/controls/Tooltip';
import { sortByType } from '../../../helpers/issues';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Issue, SourceLine } from '../../../types/types';

const MOUSE_LEAVE_DELAY = 0.25;

export interface LineIssuesIndicatorProps {
  issues: Issue[];
  issuesOpen?: boolean;
  line: SourceLine;
  onClick: () => void;
}

export function LineIssuesIndicator(props: LineIssuesIndicatorProps) {
  const { issues, issuesOpen, line } = props;
  const hasIssues = issues.length > 0;

  if (!hasIssues) {
    return <LineMeta />;
  }

  const mostImportantIssue = sortByType(issues)[0];
  const issueTypes = uniq(issues.map((i) => i.type));

  const tooltipShowHide = translate('source_viewer.issues_on_line', issuesOpen ? 'hide' : 'show');
  let tooltipContent;
  if (issueTypes.length > 1) {
    tooltipContent = translateWithParameters(
      'source_viewer.issues_on_line.multiple_issues',
      tooltipShowHide,
    );
  } else if (issues.length === 1) {
    tooltipContent = translateWithParameters(
      'source_viewer.issues_on_line.issue_of_type_X',
      tooltipShowHide,
      translate('issue.type', mostImportantIssue.type),
    );
  } else {
    tooltipContent = translateWithParameters(
      'source_viewer.issues_on_line.X_issues_of_type_Y',
      tooltipShowHide,
      issues.length,
      translate('issue.type', mostImportantIssue.type, 'plural'),
    );
  }

  return (
    <LineMeta className="it__source-line-with-issues" data-line-number={line.line}>
      <Tooltip mouseLeaveDelay={MOUSE_LEAVE_DELAY} overlay={tooltipContent}>
        <IssueIndicatorButton
          aria-label={tooltipContent}
          aria-expanded={issuesOpen}
          onClick={props.onClick}
        >
          <LineIssuesIndicatorIcon
            issuesCount={issues.length}
            mostImportantIssueType={mostImportantIssue.type}
          />
        </IssueIndicatorButton>
      </Tooltip>
    </LineMeta>
  );
}

export default React.memo(LineIssuesIndicator);
