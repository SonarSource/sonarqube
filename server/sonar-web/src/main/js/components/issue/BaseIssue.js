/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import IssueView from './IssueView';
import { setIssueAssignee } from '../../api/issues';
import type { Issue } from './types';

type Props = {
  checked?: boolean,
  issue: Issue,
  onCheck?: () => void,
  onClick: (string) => void,
  onFail: (Error) => void,
  onFilterClick?: () => void,
  onIssueChange: (Promise<*>, oldIssue?: Issue, newIssue?: Issue) => void,
  selected: boolean
};

type State = {
  currentPopup: string
};

export default class BaseIssue extends React.PureComponent {
  mounted: boolean;
  props: Props;
  state: State;

  static defaultProps = {
    selected: false
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      currentPopup: ''
    };
  }

  componentDidMount() {
    this.mounted = true;
    if (this.props.selected) {
      this.bindShortcuts();
    }
  }

  componentWillUpdate(nextProps: Props) {
    if (!nextProps.selected && this.props.selected) {
      this.unbindShortcuts();
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (!prevProps.selected && this.props.selected) {
      this.bindShortcuts();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    if (this.props.selected) {
      this.unbindShortcuts();
    }
  }

  bindShortcuts() {
    document.addEventListener('keypress', this.handleKeyPress);
  }

  unbindShortcuts() {
    document.removeEventListener('keypress', this.handleKeyPress);
  }

  togglePopup = (popupName: string, open?: boolean) => {
    if (this.mounted) {
      this.setState((prevState: State) => {
        if (prevState.currentPopup !== popupName && open !== false) {
          return { currentPopup: popupName };
        } else if (prevState.currentPopup === popupName && open !== true) {
          return { currentPopup: '' };
        }
        return prevState;
      });
    }
  };

  handleAssignement = (login: string) => {
    const { issue } = this.props;
    if (issue.assignee !== login) {
      this.props.onIssueChange(setIssueAssignee({ issue: issue.key, assignee: login }));
    }
    this.togglePopup('assign', false);
  };

  handleKeyPress = (e: Object) => {
    const tagName = e.target.tagName.toUpperCase();
    const shouldHandle = tagName !== 'INPUT' && tagName !== 'TEXTAREA' && tagName !== 'BUTTON';

    if (shouldHandle) {
      switch (e.key) {
        case 'f':
          return this.togglePopup('transition');
        case 'a':
          return this.togglePopup('assign');
        case 'm':
          return this.props.issue.actions.includes('assign_to_me') && this.handleAssignement('_me');
        case 'p':
          return this.togglePopup('plan');
        case 'i':
          return this.togglePopup('set-severity');
        case 'c':
          return this.togglePopup('comment');
        case 't':
          return this.togglePopup('edit-tags');
      }
    }
  };

  render() {
    return (
      <IssueView
        issue={this.props.issue}
        checked={this.props.checked}
        onAssign={this.handleAssignement}
        onCheck={this.props.onCheck}
        onClick={this.props.onClick}
        onFail={this.props.onFail}
        onFilterClick={this.props.onFilterClick}
        onIssueChange={this.props.onIssueChange}
        togglePopup={this.togglePopup}
        currentPopup={this.state.currentPopup}
        selected={this.props.selected}
      />
    );
  }
}
