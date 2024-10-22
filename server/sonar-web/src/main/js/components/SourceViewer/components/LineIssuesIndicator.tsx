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

import { uniq } from 'lodash';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { IssueIndicatorButton, LineIssuesIndicatorIcon, LineMeta } from '~design-system';
import Tooltip from '../../../components/controls/Tooltip';
import { useStandardExperienceMode } from '../../../queries/settings';
import { Issue, SourceLine } from '../../../types/types';

const MOUSE_LEAVE_DELAY = 0.25;

export interface LineIssuesIndicatorProps {
  as?: React.ElementType;
  issues: Issue[];
  issuesOpen?: boolean;
  line: SourceLine;
  onClick: () => void;
}

export function LineIssuesIndicator(props: LineIssuesIndicatorProps) {
  const { issues, issuesOpen, line, as = 'td' } = props;
  const hasIssues = issues.length > 0;
  const intl = useIntl();
  const { data: isStandardMode } = useStandardExperienceMode();

  if (!hasIssues) {
    return <LineMeta />;
  }

  const issueAttributeCategories = uniq(issues.map((issue) => issue.cleanCodeAttributeCategory));
  const issueTypes = uniq(issues.map((issue) => issue.type));
  let tooltipContent;

  if (isStandardMode ? issueTypes.length > 1 : issueAttributeCategories.length > 1) {
    tooltipContent = intl.formatMessage(
      { id: 'source_viewer.issues_on_line.multiple_issues' },
      { show: !issuesOpen },
    );
  } else {
    tooltipContent = intl.formatMessage(
      {
        id: `source_viewer.issues_on_line.multiple_issues_same_category${isStandardMode ? '.legacy' : ''}`,
      },
      {
        show: !issuesOpen,
        count: issues.length,
        issueType: intl
          .formatMessage({
            id: `issue.type.${issueTypes[0]}${issues.length > 1 ? '.plural' : ''}`,
          })
          .toLowerCase(),
        category: intl
          .formatMessage({
            id: `issue.clean_code_attribute_category.${issueAttributeCategories[0]}`,
          })
          .toLowerCase(),
      },
    );
  }

  return (
    <LineMeta className="it__source-line-with-issues" data-line-number={line.line} as={as}>
      <Tooltip mouseLeaveDelay={MOUSE_LEAVE_DELAY} content={tooltipContent}>
        <IssueIndicatorButton
          aria-label={tooltipContent}
          aria-expanded={issuesOpen}
          onClick={props.onClick}
        >
          <LineIssuesIndicatorIcon issuesCount={issues.length} />
        </IssueIndicatorButton>
      </Tooltip>
    </LineMeta>
  );
}

export default React.memo(LineIssuesIndicator);
