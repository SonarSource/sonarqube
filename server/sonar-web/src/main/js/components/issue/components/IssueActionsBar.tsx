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

import { HighlightRing } from 'design-system';
import * as React from 'react';
import { IssueActions } from '../../../types/issues';
import { Issue } from '../../../types/types';
import SoftwareImpactPillList from '../../shared/SoftwareImpactPillList';
import IssueAssign from './IssueAssign';
import { SonarLintBadge } from './IssueBadges';
import IssueCommentAction from './IssueCommentAction';
import IssueSeverity from './IssueSeverity';
import IssueTransition from './IssueTransition';
import IssueType from './IssueType';

interface Props {
  issue: Issue;
  currentPopup?: string;
  onAssign: (login: string) => void;
  onChange: (issue: Issue) => void;
  togglePopup: (popup: string, show?: boolean) => void;
  showIssueImpact?: boolean;
  showSonarLintBadge?: boolean;
}

export default function IssueActionsBar(props: Props) {
  const {
    issue,
    currentPopup,
    onAssign,
    onChange,
    togglePopup,
    showIssueImpact,
    showSonarLintBadge,
  } = props;

  const [commentPlaceholder, setCommentPlaceholder] = React.useState('');

  const toggleComment = (open: boolean, placeholder = '') => {
    setCommentPlaceholder(placeholder);

    togglePopup('comment', open);
  };

  const canAssign = issue.actions.includes(IssueActions.Assign);
  const canComment = issue.actions.includes(IssueActions.Comment);

  return (
    <div className="sw-flex sw-gap-3">
      <ul className="it__issue-header-actions sw-flex sw-items-center sw-gap-3 sw-body-sm">
        <HighlightRing
          as="li"
          className="sw-relative"
          data-guiding-id={`issue-transition-${issue.key}`}
        >
          <IssueTransition
            isOpen={currentPopup === 'transition'}
            togglePopup={togglePopup}
            issue={issue}
            onChange={onChange}
          />
        </HighlightRing>

        <li>
          <IssueAssign
            isOpen={currentPopup === 'assign'}
            togglePopup={togglePopup}
            canAssign={canAssign}
            issue={issue}
            onAssign={onAssign}
          />
        </li>

        {showIssueImpact && (
          <li data-guiding-id="issue-2">
            <SoftwareImpactPillList className="sw-gap-3" softwareImpacts={issue.impacts} />
          </li>
        )}

        {showSonarLintBadge && issue.quickFixAvailable && (
          <li>
            <SonarLintBadge quickFixAvailable={issue.quickFixAvailable} />
          </li>
        )}
      </ul>
      <ul className="sw-flex sw-items-center sw-gap-3 sw-body-sm" data-guiding-id="issue-4">
        <li>
          <IssueType issue={issue} />
        </li>

        <li>
          <IssueSeverity issue={issue} />
        </li>
      </ul>

      {canComment && (
        <IssueCommentAction
          commentPlaceholder={commentPlaceholder}
          currentPopup={currentPopup === 'comment'}
          issueKey={issue.key}
          onChange={onChange}
          toggleComment={toggleComment}
        />
      )}
    </div>
  );
}
