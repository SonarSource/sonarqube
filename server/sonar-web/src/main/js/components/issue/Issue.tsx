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
import { flow } from 'lodash';
import * as React from 'react';
import { useCallback } from 'react';
import { setIssueAssignee } from '../../api/issues';
import { isInput, isShortcut } from '../../helpers/keyboardEventHelpers';
import { KeyboardKeys } from '../../helpers/keycodes';
import { getKeyboardShortcutEnabled } from '../../helpers/preferences';
import { useRefreshBranchStatus } from '../../queries/branch';
import { BranchLike } from '../../types/branch-like';
import { Issue as TypeIssue } from '../../types/types';
import { updateIssue } from './actions';
import IssueView from './components/IssueView';

interface Props {
  branchLike?: BranchLike;
  checked?: boolean;
  displayWhyIsThisAnIssue?: boolean;
  issue: TypeIssue;
  onChange: (issue: TypeIssue) => void;
  onCheck?: (issue: string) => void;
  onPopupToggle: (issue: string, popupName: string, open?: boolean) => void;
  onSelect: (issueKey: string) => void;
  openPopup?: string;
  selected: boolean;
}

export default function Issue(props: Props) {
  const {
    selected = false,
    issue,
    branchLike,
    checked,
    openPopup,
    displayWhyIsThisAnIssue,
    onCheck,
    onPopupToggle,
  } = props;

  const refreshStatus = useRefreshBranchStatus();

  const onChange = flow([props.onChange, refreshStatus]);

  const togglePopup = useCallback(
    (popupName: string, open?: boolean) => {
      onPopupToggle(issue.key, popupName, open);
    },
    [issue.key, onPopupToggle],
  );

  const handleAssignement = useCallback(
    (login: string) => {
      if (issue.assignee !== login) {
        updateIssue(onChange, setIssueAssignee({ issue: issue.key, assignee: login }));
      }
      togglePopup('assign', false);
    },
    [issue.assignee, issue.key, onChange, togglePopup],
  );

  const handleKeyDown = useCallback(
    (event: KeyboardEvent) => {
      if (!getKeyboardShortcutEnabled() || isInput(event) || isShortcut(event)) {
        return true;
      } else if (event.key === KeyboardKeys.KeyF) {
        event.preventDefault();
        return togglePopup('transition');
      } else if (event.key === KeyboardKeys.KeyA) {
        event.preventDefault();
        return togglePopup('assign');
      } else if (event.key === KeyboardKeys.KeyM && issue.actions.includes('assign')) {
        event.preventDefault();
        return handleAssignement('_me');
      } else if (event.key === KeyboardKeys.KeyI) {
        event.preventDefault();
        return togglePopup('set-severity');
      } else if (event.key === KeyboardKeys.KeyT) {
        event.preventDefault();
        return togglePopup('edit-tags');
      } else if (event.key === KeyboardKeys.Space) {
        event.preventDefault();
        if (onCheck) {
          return onCheck(issue.key);
        }
      }
      return true;
    },
    [issue.actions, issue.key, togglePopup, handleAssignement, onCheck],
  );

  React.useEffect(() => {
    if (selected) {
      document.addEventListener('keydown', handleKeyDown, { capture: true });
    }
    return () => document.removeEventListener('keydown', handleKeyDown, { capture: true });
  }, [handleKeyDown, selected]);

  return (
    <IssueView
      branchLike={branchLike}
      checked={checked}
      currentPopup={openPopup}
      displayWhyIsThisAnIssue={displayWhyIsThisAnIssue}
      issue={issue}
      onAssign={handleAssignement}
      onChange={onChange}
      onCheck={props.onCheck}
      onSelect={props.onSelect}
      selected={selected}
      togglePopup={togglePopup}
    />
  );
}
