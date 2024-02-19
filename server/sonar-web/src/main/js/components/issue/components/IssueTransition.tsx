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
import {
  Dropdown,
  DropdownMenuWrapper,
  ItemDivider,
  PopupPlacement,
  PopupZLevel,
  SearchSelectDropdownControl,
} from 'design-system';
import * as React from 'react';
import { addIssueComment, setIssueTransition } from '../../../api/issues';
import { SESSION_STORAGE_TRANSITION_GUIDE_KEY } from '../../../apps/issues/components/IssueNewStatusAndTransitionGuide';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Issue } from '../../../types/types';
import StatusHelper from '../../shared/StatusHelper';
import { updateIssue } from '../actions';
import { IssueTransitionOverlay } from './IssueTransitionOverlay';

interface Props {
  isOpen: boolean;
  issue: Pick<Issue, 'key' | 'resolution' | 'issueStatus' | 'transitions' | 'type' | 'actions'>;
  onChange: (issue: Issue) => void;
  togglePopup: (popup: string, show?: boolean) => void;
}

export default function IssueTransition(props: Readonly<Props>) {
  const { isOpen, issue, onChange, togglePopup } = props;

  const guideStepIndex = +(sessionStorage.getItem(SESSION_STORAGE_TRANSITION_GUIDE_KEY) ?? 0);
  const guideIsRunning = sessionStorage.getItem(SESSION_STORAGE_TRANSITION_GUIDE_KEY) !== null;
  const [transitioning, setTransitioning] = React.useState(false);

  async function handleSetTransition(transition: string, comment?: string) {
    setTransitioning(true);

    try {
      if (typeof comment === 'string' && comment.length > 0) {
        await setIssueTransition({ issue: issue.key, transition });
        await updateIssue(onChange, addIssueComment({ issue: issue.key, text: comment }));
      } else {
        await updateIssue(onChange, setIssueTransition({ issue: issue.key, transition }));
      }
      togglePopup('transition', false);
    } finally {
      setTransitioning(false);
    }
  }

  function handleClose() {
    togglePopup('transition', false);
  }

  function onToggleClick() {
    togglePopup('transition', !isOpen);
  }

  if (issue.transitions?.length) {
    return (
      <StyledDropdown
        allowResizing
        closeOnClick={false}
        id="issue-transition"
        onClose={handleClose}
        openDropdown={isOpen}
        withClickOutHandler={!guideIsRunning}
        withFocusOutHandler={!guideIsRunning}
        overlay={
          <IssueTransitionOverlay
            issue={issue}
            onClose={handleClose}
            onSetTransition={handleSetTransition}
            loading={transitioning}
            guideStepIndex={guideStepIndex}
          />
        }
        placement={PopupPlacement.Bottom}
        zLevel={PopupZLevel.Absolute}
        size="medium"
      >
        {({ a11yAttrs }) => (
          <SearchSelectDropdownControl
            {...a11yAttrs}
            onClick={onToggleClick}
            onClear={handleClose}
            isDiscreet
            className="it__issue-transition sw-px-1"
            label={
              <StatusHelper className="sw-flex sw-items-center" issueStatus={issue.issueStatus} />
            }
            ariaLabel={translateWithParameters(
              'issue.transition.status_x_click_to_change',
              translate('issue.issue_status', issue.issueStatus),
            )}
          />
        )}
      </StyledDropdown>
    );
  }

  return <StatusHelper issueStatus={issue.issueStatus} />;
}

const StyledDropdown = styled(Dropdown)`
  overflow: auto;

  & ${DropdownMenuWrapper} {
    border-radius: 8px;

    ${ItemDivider} {
      margin-left: 0;
      margin-right: 0;
    }
  }
`;
