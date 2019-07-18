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
import LinkIcon from 'sonar-ui-common/components/icons/LinkIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getBranchLikeQuery } from '../../../helpers/branches';
import { getComponentIssuesUrl } from '../../../helpers/urls';
import { WorkspaceContext } from '../../workspace/context';
import IssueChangelog from './IssueChangelog';
import IssueMessage from './IssueMessage';
import SimilarIssuesFilter from './SimilarIssuesFilter';

interface Props {
  branchLike?: T.BranchLike;
  currentPopup?: string;
  issue: T.Issue;
  onFilter?: (property: string, issue: T.Issue) => void;
  togglePopup: (popup: string, show?: boolean) => void;
}

export default function IssueTitleBar(props: Props) {
  const { issue } = props;
  const hasSimilarIssuesFilter = props.onFilter != null;

  const issueUrl = getComponentIssuesUrl(issue.project, {
    ...getBranchLikeQuery(props.branchLike),
    issues: issue.key,
    open: issue.key,
    types: issue.type === 'SECURITY_HOTSPOT' ? issue.type : undefined
  });

  return (
    <div className="issue-row">
      <WorkspaceContext.Consumer>
        {({ openRule }) => (
          <IssueMessage
            engine={issue.externalRuleEngine}
            manualVulnerability={issue.fromHotspot && issue.type === 'VULNERABILITY'}
            message={issue.message}
            openRule={openRule}
            organization={issue.organization}
            rule={issue.rule}
          />
        )}
      </WorkspaceContext.Consumer>

      <div className="issue-row-meta">
        <ul className="issue-meta-list">
          <li className="issue-meta">
            <IssueChangelog
              creationDate={issue.creationDate}
              isOpen={props.currentPopup === 'changelog'}
              issue={issue}
              togglePopup={props.togglePopup}
            />
          </li>
          {issue.textRange != null && (
            <li className="issue-meta">
              <span className="issue-meta-label" title={translate('line_number')}>
                L{issue.textRange.endLine}
              </span>
            </li>
          )}
          <li className="issue-meta">
            <Link
              className="js-issue-permalink link-no-underline"
              target="_blank"
              title={translate('permalink')}
              to={issueUrl}>
              <LinkIcon />
            </Link>
          </li>
          {hasSimilarIssuesFilter && (
            <li className="issue-meta">
              <SimilarIssuesFilter
                isOpen={props.currentPopup === 'similarIssues'}
                issue={issue}
                onFilter={props.onFilter}
                togglePopup={props.togglePopup}
              />
            </li>
          )}
        </ul>
      </div>
    </div>
  );
}
