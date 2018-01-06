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
import ComponentBreadcrumbs from './ComponentBreadcrumbs';
import Issue from '../../../components/issue/Issue';
/*:: import type { Issue as IssueType } from '../../../components/issue/types'; */
/*:: import type { Component } from '../utils'; */

/*::
type Props = {|
  branch?: string,
  checked: boolean,
  component?: Component,
  issue: IssueType,
  onChange: IssueType => void,
  onCheck?: string => void,
  onClick: string => void,
  onFilterChange: (changes: {}) => void,
  onPopupToggle: (issue: string, popupName: string, open: ?boolean ) => void,
  openPopup: ?string,
  organization?: { key: string },
  previousIssue: ?Object,
  selected: boolean
|};
*/

/*::
type State = {
  similarIssues: boolean
};
*/

export default class ListItem extends React.PureComponent {
  /*:: props: Props; */
  state /*: State */ = { similarIssues: false };

  handleFilter = (property /*: string */, issue /*: IssueType */) => {
    const { onFilterChange } = this.props;

    const issuesReset = { issues: [] };

    if (property.startsWith('tag###')) {
      const tag = property.substr(6);
      return onFilterChange({ ...issuesReset, tags: [tag] });
    }

    switch (property) {
      case 'type':
        return onFilterChange({ ...issuesReset, types: [issue.type] });
      case 'severity':
        return onFilterChange({ ...issuesReset, severities: [issue.severity] });
      case 'status':
        return onFilterChange({ ...issuesReset, statuses: [issue.status] });
      case 'resolution':
        return issue.resolution != null
          ? onFilterChange({ ...issuesReset, resolved: true, resolutions: [issue.resolution] })
          : onFilterChange({ ...issuesReset, resolved: false, resolutions: [] });
      case 'assignee':
        return issue.assignee != null
          ? onFilterChange({ ...issuesReset, assigned: true, assignees: [issue.assignee] })
          : onFilterChange({ ...issuesReset, assigned: false, assignees: [] });
      case 'rule':
        return onFilterChange({ ...issuesReset, rules: [issue.rule] });
      case 'project':
        return onFilterChange({ ...issuesReset, projects: [issue.projectUuid] });
      case 'module':
        return onFilterChange({ ...issuesReset, modules: [issue.subProjectUuid] });
      case 'file':
        return onFilterChange({ ...issuesReset, files: [issue.componentUuid] });
    }
  };

  render() {
    const { branch, component, issue, previousIssue } = this.props;

    const displayComponent = previousIssue == null || previousIssue.component !== issue.component;

    return (
      <div className="issues-workspace-list-item">
        {displayComponent && (
          <div className="issues-workspace-list-component">
            <ComponentBreadcrumbs
              branch={branch}
              component={component}
              issue={this.props.issue}
              organization={this.props.organization}
            />
          </div>
        )}
        <Issue
          branch={branch}
          checked={this.props.checked}
          displayLocationsLink={false}
          issue={issue}
          onChange={this.props.onChange}
          onCheck={this.props.onCheck}
          onClick={this.props.onClick}
          onFilter={this.handleFilter}
          onPopupToggle={this.props.onPopupToggle}
          openPopup={this.props.openPopup}
          selected={this.props.selected}
        />
      </div>
    );
  }
}
