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
import key from 'keymaster';
import IssueView from './IssueView';
import { updateIssue } from './actions';
import { setIssueAssignee } from '../../api/issues';
import { onFail } from '../../store/rootActions';
import type { Issue } from './types';

type Props = {|
  checked?: boolean,
  issue: Issue,
  onChange: Issue => void,
  onCheck?: string => void,
  onClick: string => void,
  onFilter?: (property: string, issue: Issue) => void,
  selected: boolean
|};

type State = {
  currentPopup: string
};

export default class BaseIssue extends React.PureComponent {
  mounted: boolean;
  props: Props;
  state: State;

  static contextTypes = {
    store: React.PropTypes.object
  };

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
    key('f', 'issues', () => {
      this.togglePopup('transition');
      return false;
    });
    key('a', 'issues', () => {
      this.togglePopup('assign');
      return false;
    });
    key('m', 'issues', () => {
      this.props.issue.actions.includes('assign_to_me') && this.handleAssignement('_me');
      return false;
    });
    key('i', 'issues', () => {
      this.togglePopup('set-severity');
      return false;
    });
    key('c', 'issues', () => {
      this.togglePopup('comment');
      return false;
    });
    key('t', 'issues', () => {
      this.togglePopup('edit-tags');
      return false;
    });
  }

  unbindShortcuts() {
    key.unbind('f', 'issues');
    key.unbind('a', 'issues');
    key.unbind('m', 'issues');
    key.unbind('i', 'issues');
    key.unbind('c', 'issues');
    key.unbind('t', 'issues');
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
      updateIssue(
        this.props.onChange,
        this.handleFail,
        setIssueAssignee({ issue: issue.key, assignee: login })
      );
    }
    this.togglePopup('assign', false);
  };

  handleFail = (error: Error) => {
    onFail(this.context.store.dispatch)(error);
  };

  render() {
    return (
      <IssueView
        issue={this.props.issue}
        checked={this.props.checked}
        onAssign={this.handleAssignement}
        onCheck={this.props.onCheck}
        onClick={this.props.onClick}
        onFail={this.handleFail}
        onFilter={this.props.onFilter}
        onChange={this.props.onChange}
        togglePopup={this.togglePopup}
        currentPopup={this.state.currentPopup}
        selected={this.props.selected}
      />
    );
  }
}
