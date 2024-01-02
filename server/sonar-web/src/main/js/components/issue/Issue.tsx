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
import { setIssueAssignee } from '../../api/issues';
import { isInput, isShortcut } from '../../helpers/keyboardEventHelpers';
import { KeyboardKeys } from '../../helpers/keycodes';
import { getKeyboardShortcutEnabled } from '../../helpers/preferences';
import { BranchLike } from '../../types/branch-like';
import { Issue as TypeIssue } from '../../types/types';
import { updateIssue } from './actions';
import './Issue.css';
import IssueView from './IssueView';

interface Props {
  branchLike?: BranchLike;
  checked?: boolean;
  displayWhyIsThisAnIssue?: boolean;
  displayLocationsCount?: boolean;
  displayLocationsLink?: boolean;
  issue: TypeIssue;
  onChange: (issue: TypeIssue) => void;
  onCheck?: (issue: string) => void;
  onClick?: (issueKey: string) => void;
  onFilter?: (property: string, issue: TypeIssue) => void;
  onPopupToggle: (issue: string, popupName: string, open?: boolean) => void;
  openPopup?: string;
  selected: boolean;
}

export default class Issue extends React.PureComponent<Props> {
  static defaultProps = {
    selected: false,
  };

  componentDidMount() {
    if (this.props.selected) {
      document.addEventListener('keydown', this.handleKeyDown, { capture: true });
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (!prevProps.selected && this.props.selected) {
      document.addEventListener('keydown', this.handleKeyDown, { capture: true });
    } else if (prevProps.selected && !this.props.selected) {
      document.removeEventListener('keydown', this.handleKeyDown, { capture: true });
    }
  }

  componentWillUnmount() {
    if (this.props.selected) {
      document.removeEventListener('keydown', this.handleKeyDown, { capture: true });
    }
  }

  handleKeyDown = (event: KeyboardEvent) => {
    if (!getKeyboardShortcutEnabled() || isInput(event) || isShortcut(event)) {
      return true;
    } else if (event.key === KeyboardKeys.KeyF) {
      event.preventDefault();
      return this.togglePopup('transition');
    } else if (event.key === KeyboardKeys.KeyA) {
      event.preventDefault();
      return this.togglePopup('assign');
    } else if (event.key === KeyboardKeys.KeyM && this.props.issue.actions.includes('assign')) {
      event.preventDefault();
      return this.handleAssignement('_me');
    } else if (event.key === KeyboardKeys.KeyI) {
      event.preventDefault();
      return this.togglePopup('set-severity');
    } else if (event.key === KeyboardKeys.KeyC) {
      event.preventDefault();
      return this.togglePopup('comment');
    } else if (event.key === KeyboardKeys.KeyT) {
      event.preventDefault();
      return this.togglePopup('edit-tags');
    } else if (event.key === KeyboardKeys.Space) {
      event.preventDefault();
      if (this.props.onCheck) {
        return this.props.onCheck(this.props.issue.key);
      }
    }
    return true;
  };

  togglePopup = (popupName: string, open?: boolean) => {
    this.props.onPopupToggle(this.props.issue.key, popupName, open);
  };

  handleAssignement = (login: string) => {
    const { issue } = this.props;
    if (issue.assignee !== login) {
      updateIssue(this.props.onChange, setIssueAssignee({ issue: issue.key, assignee: login }));
    }
    this.togglePopup('assign', false);
  };

  render() {
    return (
      <IssueView
        branchLike={this.props.branchLike}
        checked={this.props.checked}
        currentPopup={this.props.openPopup}
        displayWhyIsThisAnIssue={this.props.displayWhyIsThisAnIssue}
        displayLocationsCount={this.props.displayLocationsCount}
        displayLocationsLink={this.props.displayLocationsLink}
        issue={this.props.issue}
        onAssign={this.handleAssignement}
        onChange={this.props.onChange}
        onCheck={this.props.onCheck}
        onClick={this.props.onClick}
        onFilter={this.props.onFilter}
        selected={this.props.selected}
        togglePopup={this.togglePopup}
      />
    );
  }
}
