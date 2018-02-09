/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import PropTypes from 'prop-types';
import IssueView from './IssueView';
import { updateIssue } from './actions';
import { onFail } from '../../store/rootActions';
import { setIssueAssignee } from '../../api/issues';
/*:: import type { Issue as IssueType } from './types'; */

/*::
type Props = {|
  branch?: string,
  checked?: boolean,
  displayLocationsCount?: boolean;
  displayLocationsLink?: boolean;
  issue: IssueType,
  onChange: IssueType => void,
  onCheck?: string => void,
  onClick: string => void,
  onFilter?: (property: string, issue: IssueType) => void,
  onPopupToggle: (issue: string, popupName: string, open: ?boolean) => void,
  openPopup: ?string,
  selected: boolean
|};
*/

export default class Issue extends React.PureComponent {
  /*:: props: Props; */

  static contextTypes = {
    store: PropTypes.object
  };

  static defaultProps = {
    displayLocationsCount: true,
    displayLocationsLink: true,
    selected: false
  };

  componentDidMount() {
    if (this.props.selected) {
      this.bindShortcuts();
    }
  }

  componentWillUpdate(nextProps /*: Props */) {
    if (!nextProps.selected && this.props.selected) {
      this.unbindShortcuts();
    }
  }

  componentDidUpdate(prevProps /*: Props */) {
    if (!prevProps.selected && this.props.selected) {
      this.bindShortcuts();
    }
  }

  componentWillUnmount() {
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
      if (this.props.issue.actions.includes('assign')) {
        this.handleAssignement('_me');
      }
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
    key('space', 'issues', () => {
      if (this.props.onCheck) {
        this.props.onCheck(this.props.issue.key);
        return false;
      }
      return undefined;
    });
  }

  unbindShortcuts() {
    key.unbind('f', 'issues');
    key.unbind('a', 'issues');
    key.unbind('m', 'issues');
    key.unbind('i', 'issues');
    key.unbind('c', 'issues');
    key.unbind('space', 'issues');
    key.unbind('t', 'issues');
  }

  togglePopup = (popupName /*: string */, open /*: ?boolean */) => {
    this.props.onPopupToggle(this.props.issue.key, popupName, open);
  };

  handleAssignement = (login /*: string */) => {
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

  handleFail = (error /*: Error */) => {
    onFail(this.context.store.dispatch)(error);
  };

  render() {
    return (
      <IssueView
        branch={this.props.branch}
        checked={this.props.checked}
        currentPopup={this.props.openPopup}
        displayLocationsCount={this.props.displayLocationsCount}
        displayLocationsLink={this.props.displayLocationsLink}
        issue={this.props.issue}
        onAssign={this.handleAssignement}
        onCheck={this.props.onCheck}
        onClick={this.props.onClick}
        onFail={this.handleFail}
        onFilter={this.props.onFilter}
        onChange={this.props.onChange}
        selected={this.props.selected}
        togglePopup={this.togglePopup}
      />
    );
  }
}
