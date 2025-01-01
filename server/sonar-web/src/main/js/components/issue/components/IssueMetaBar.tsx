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

import styled from '@emotion/styled';
import classNames from 'classnames';
import * as React from 'react';
import { Badge, CommentIcon, SeparatorCircleIcon } from '~design-system';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { isDefined } from '../../../helpers/types';
import { useStandardExperienceModeQuery } from '../../../queries/mode';
import { useLocation } from '../../../sonar-aligned/components/hoc/withRouter';
import { Issue } from '../../../types/types';
import Tooltip from '../../controls/Tooltip';
import DateFromNow from '../../intl/DateFromNow';
import { WorkspaceContext } from '../../workspace/context';
import IssuePrioritized from './IssuePrioritized';
import IssueSeverity from './IssueSeverity';
import IssueType from './IssueType';
import SonarLintBadge from './SonarLintBadge';

interface Props {
  issue: Issue;
  showLine?: boolean;
}

export default function IssueMetaBar(props: Readonly<Props>) {
  const { issue, showLine } = props;
  const location = useLocation();

  const { externalRulesRepoNames } = React.useContext(WorkspaceContext);
  const { data: isStandardMode } = useStandardExperienceModeQuery();

  const ruleEngine =
    (issue.externalRuleEngine && externalRulesRepoNames[issue.externalRuleEngine]) ||
    issue.externalRuleEngine;

  const hasComments = !!issue.comments?.length;

  const issueMetaListItemClassNames =
    'sw-typo-sm sw-overflow-hidden sw-whitespace-nowrap sw-max-w-abs-150';

  return (
    <ul className="sw-flex sw-items-center sw-gap-1 sw-typo-sm sw-whitespace-nowrap">
      {issue.line && (
        <>
          <IssueMetaListItem className={issueMetaListItemClassNames}>
            L{issue.line}
          </IssueMetaListItem>

          <SeparatorCircleIcon aria-hidden as="li" />
        </>
      )}

      {issue.quickFixAvailable && (
        <>
          <li className={issueMetaListItemClassNames}>
            <SonarLintBadge compact />
          </li>
          <SeparatorCircleIcon aria-hidden as="li" />
        </>
      )}

      {ruleEngine && (
        <>
          <li className={issueMetaListItemClassNames}>
            <Tooltip
              content={translateWithParameters('issue.from_external_rule_engine', ruleEngine)}
            >
              <span>
                <Badge>{ruleEngine}</Badge>
              </span>
            </Tooltip>
          </li>
          <SeparatorCircleIcon aria-hidden as="li" />
        </>
      )}

      {!!issue.codeVariants?.length && (
        <>
          <IssueMetaListItem>
            <Tooltip content={issue.codeVariants.join(', ')}>
              <span>
                {issue.codeVariants.length > 1
                  ? translateWithParameters('issue.x_code_variants', issue.codeVariants.length)
                  : translate('issue.1_code_variant')}
              </span>
            </Tooltip>
          </IssueMetaListItem>
          <SeparatorCircleIcon aria-hidden as="li" />
        </>
      )}

      {hasComments && (
        <>
          <IssueMetaListItem
            className={classNames(issueMetaListItemClassNames, 'sw-flex sw-gap-1')}
          >
            <CommentIcon aria-label={translate('issue.comment.formlink')} />
            {issue.comments?.length}
          </IssueMetaListItem>

          <SeparatorCircleIcon aria-hidden as="li" />
        </>
      )}

      {showLine && isDefined(issue.textRange) && (
        <>
          <Tooltip content={translate('line_number')}>
            <IssueMetaListItem className={issueMetaListItemClassNames}>
              {translateWithParameters('issue.ncloc_x.short', issue.textRange.endLine)}
            </IssueMetaListItem>
          </Tooltip>

          <SeparatorCircleIcon aria-hidden as="li" />
        </>
      )}

      {issue.effort && (
        <>
          <IssueMetaListItem className={issueMetaListItemClassNames}>
            {translateWithParameters('issue.x_effort', issue.effort)}
          </IssueMetaListItem>

          <SeparatorCircleIcon aria-hidden as="li" />
        </>
      )}

      <IssueMetaListItem className={issueMetaListItemClassNames}>
        <DateFromNow date={issue.creationDate} />
      </IssueMetaListItem>
      {!isStandardMode && (location.query.types || location.query.severities) && (
        <>
          <SeparatorCircleIcon aria-hidden as="li" />

          <IssueType issue={issue} height={12} width={12} />

          <SeparatorCircleIcon data-guiding-id="issue-4" aria-hidden as="li" />

          <IssueSeverity issue={issue} height={12} width={12} />
        </>
      )}

      {issue.prioritizedRule && (
        <>
          <SeparatorCircleIcon aria-hidden as="li" />

          <IssueMetaListItem className={issueMetaListItemClassNames}>
            <IssuePrioritized />
          </IssueMetaListItem>
        </>
      )}
    </ul>
  );
}

const IssueMetaListItem = styled.li`
  color: var(--echoes-color-text-subdued);
`;
