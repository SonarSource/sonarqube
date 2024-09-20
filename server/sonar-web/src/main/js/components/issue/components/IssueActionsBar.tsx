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
import { HighlightRing } from 'design-system';
import * as React from 'react';
import { IssueActions } from '../../../types/issues';
import { Issue } from '../../../types/types';
import IssueAssign from './IssueAssign';
import IssueTags from './IssueTags';
import IssueTransition from './IssueTransition';
import SonarLintBadge from './SonarLintBadge';

interface Props {
  canSetTags?: boolean;
  currentPopup?: string;
  issue: Issue;
  onAssign: (login: string) => void;
  onChange: (issue: Issue) => void;
  showSonarLintBadge?: boolean;
  showTags?: boolean;
  togglePopup: (popup: string, show?: boolean) => void;
}

export default function IssueActionsBar(props: Readonly<Props>) {
  const {
    issue,
    currentPopup,
    onAssign,
    onChange,
    togglePopup,
    showSonarLintBadge,
    showTags,
    canSetTags,
  } = props;

  const canAssign = issue.actions.includes(IssueActions.Assign);
  const tagsPopupOpen = currentPopup === 'edit-tags' && canSetTags;

  return (
    <div className="sw-flex sw-gap-3 sw-min-w-0">
      <ul className="it__issue-header-actions sw-flex sw-items-center sw-gap-3 sw-typo-default sw-min-w-0">
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

        <li className="sw-min-w-0">
          <IssueAssign
            isOpen={currentPopup === 'assign'}
            togglePopup={togglePopup}
            canAssign={canAssign}
            issue={issue}
            onAssign={onAssign}
          />
        </li>

        {showTags && (
          <li>
            <IssueTags
              canSetTags={canSetTags}
              issue={issue}
              onChange={props.onChange}
              open={tagsPopupOpen}
              togglePopup={props.togglePopup}
              tagsToDisplay={1}
            />
          </li>
        )}

        {showSonarLintBadge && issue.quickFixAvailable && (
          <li>
            <SonarLintBadge />
          </li>
        )}
      </ul>
    </div>
  );
}
