/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { StandoutLink } from 'design-system';
import * as React from 'react';
import { parseQuery, serializeQuery } from '../../../apps/issues/utils';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { getComponentIssuesUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { Issue } from '../../../types/types';
import { useLocation } from '../../hoc/withRouter';
import { IssueMessageHighlighting } from '../IssueMessageHighlighting';

export interface IssueMessageProps {
  issue: Issue;
  branchLike?: BranchLike;
  displayWhyIsThisAnIssue?: boolean;
}

export default function IssueMessage(props: IssueMessageProps) {
  const { issue, branchLike, displayWhyIsThisAnIssue } = props;
  const location = useLocation();
  const query = parseQuery(location.query);

  const { message, messageFormattings } = issue;

  const whyIsThisAnIssueUrl = getComponentIssuesUrl(issue.project, {
    ...getBranchLikeQuery(branchLike),
    files: issue.componentLongName,
    open: issue.key,
    resolved: 'false',
    why: '1',
  });

  const issueUrl = getComponentIssuesUrl(issue.project, {
    ...getBranchLikeQuery(branchLike),
    ...serializeQuery(query),
    open: issue.key,
  });
  return (
    <>
      {issueUrl?.pathname ? (
        <StandoutLink className="it__issue-message" to={issueUrl}>
          <IssueMessageHighlighting message={message} messageFormattings={messageFormattings} />
        </StandoutLink>
      ) : (
        <span className="spacer-right">
          <IssueMessageHighlighting message={message} messageFormattings={messageFormattings} />
        </span>
      )}

      {displayWhyIsThisAnIssue && (
        <StandoutLink
          aria-label={translate('issue.why_this_issue.long')}
          target="_blank"
          className="sw-ml-2"
          to={whyIsThisAnIssueUrl}
        >
          {translate('issue.why_this_issue')}
        </StandoutLink>
      )}
    </>
  );
}
