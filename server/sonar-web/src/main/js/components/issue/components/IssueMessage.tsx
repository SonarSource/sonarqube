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
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { getComponentIssuesUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { RuleStatus } from '../../../types/rules';
import { Issue } from '../../../types/types';
import Link from '../../common/Link';
import { IssueMessageHighlighting } from '../IssueMessageHighlighting';
import IssueMessageTags from './IssueMessageTags';

export interface IssueMessageProps {
  issue: Issue;
  branchLike?: BranchLike;
  displayWhyIsThisAnIssue?: boolean;
}

export default function IssueMessage(props: IssueMessageProps) {
  const { issue, branchLike, displayWhyIsThisAnIssue } = props;

  const { externalRuleEngine, quickFixAvailable, message, messageFormattings, ruleStatus } = issue;

  const whyIsThisAnIssueUrl = getComponentIssuesUrl(issue.project, {
    ...getBranchLikeQuery(branchLike),
    files: issue.componentLongName,
    open: issue.key,
    resolved: 'false',
    why: '1',
  });

  return (
    <>
      <div className="display-inline-flex-center issue-message break-word">
        <span className="spacer-right">
          <IssueMessageHighlighting message={message} messageFormattings={messageFormattings} />
        </span>
        <IssueMessageTags
          engine={externalRuleEngine}
          quickFixAvailable={quickFixAvailable}
          ruleStatus={ruleStatus as RuleStatus | undefined}
        />
      </div>
      {displayWhyIsThisAnIssue && (
        <Link
          aria-label={translate('issue.why_this_issue.long')}
          className="spacer-right"
          target="_blank"
          to={whyIsThisAnIssueUrl}
        >
          {translate('issue.why_this_issue')}
        </Link>
      )}
    </>
  );
}
