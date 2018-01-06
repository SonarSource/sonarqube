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
import ListItem from './ListItem';
/*:: import type { Issue } from '../../../components/issue/types'; */
/*:: import type { Component } from '../utils'; */

/*::
type Props = {|
  branch?: string,
  checked: Array<string>,
  component?: Component,
  issues: Array<Issue>,
  onFilterChange: (changes: {}) => void,
  onIssueChange: Issue => void,
  onIssueCheck?: string => void,
  onIssueClick: string => void,
  onPopupToggle: (issue: string, popupName: string, open: ?boolean ) => void,
  openPopup: ?{ issue: string, name: string},
  organization?: { key: string },
  selectedIssue: ?Issue
|};
*/

export default class IssuesList extends React.PureComponent {
  /*:: props: Props; */

  render() {
    const { branch, checked, component, issues, openPopup, selectedIssue } = this.props;

    return (
      <div>
        {issues.map((issue, index) => (
          <ListItem
            branch={branch}
            checked={checked.includes(issue.key)}
            component={component}
            key={issue.key}
            issue={issue}
            onChange={this.props.onIssueChange}
            onCheck={this.props.onIssueCheck}
            onClick={this.props.onIssueClick}
            onFilterChange={this.props.onFilterChange}
            onPopupToggle={this.props.onPopupToggle}
            openPopup={openPopup && openPopup.issue === issue.key ? openPopup.name : null}
            organization={this.props.organization}
            previousIssue={index > 0 ? issues[index - 1] : null}
            selected={selectedIssue != null && selectedIssue.key === issue.key}
          />
        ))}
      </div>
    );
  }
}
