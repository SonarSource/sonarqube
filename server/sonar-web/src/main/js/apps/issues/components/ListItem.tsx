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
import Issue from '../../../components/issue/Issue';
import { BranchLike } from '../../../types/branch-like';
import { Component, Issue as TypeIssue } from '../../../types/types';
import { Query } from '../utils';
import ComponentBreadcrumbs from './ComponentBreadcrumbs';

interface Props {
  branchLike: BranchLike | undefined;
  checked: boolean;
  component: Component | undefined;
  issue: TypeIssue;
  onChange: (issue: TypeIssue) => void;
  onCheck: ((issueKey: string) => void) | undefined;
  onClick: (issueKey: string) => void;
  onFilterChange: (changes: Partial<Query>) => void;
  onPopupToggle: (issue: string, popupName: string, open?: boolean) => void;
  openPopup: string | undefined;
  previousIssue: TypeIssue | undefined;
  selected: boolean;
}

export default class ListItem extends React.PureComponent<Props> {
  nodeRef: HTMLLIElement | null = null;

  componentDidMount() {
    const { selected } = this.props;
    if (this.nodeRef && selected) {
      this.nodeRef.scrollIntoView({ block: 'center', inline: 'center' });
    }
  }

  componentDidUpdate(prevProps: Props) {
    const { selected } = this.props;
    if (!prevProps.selected && selected && this.nodeRef) {
      this.nodeRef.scrollIntoView({ block: 'center', inline: 'center' });
    }
  }

  handleFilter = (property: string, issue: TypeIssue) => {
    const { onFilterChange } = this.props;

    const issuesReset = { issues: [] };

    if (property.startsWith('tag###')) {
      const tag = property.substr(6);
      onFilterChange({ ...issuesReset, tags: [tag] });
    } else {
      switch (property) {
        case 'type':
          onFilterChange({ ...issuesReset, types: [issue.type] });
          break;
        case 'severity':
          onFilterChange({ ...issuesReset, severities: [issue.severity] });
          break;
        case 'status':
          onFilterChange({ ...issuesReset, statuses: [issue.status] });
          break;
        case 'resolution':
          if (issue.resolution) {
            onFilterChange({ ...issuesReset, resolved: true, resolutions: [issue.resolution] });
          } else {
            onFilterChange({ ...issuesReset, resolved: false, resolutions: [] });
          }
          break;
        case 'assignee':
          if (issue.assignee) {
            onFilterChange({ ...issuesReset, assigned: true, assignees: [issue.assignee] });
          } else {
            onFilterChange({ ...issuesReset, assigned: false, assignees: [] });
          }
          break;
        case 'rule':
          onFilterChange({ ...issuesReset, rules: [issue.rule] });
          break;
        case 'project':
          onFilterChange({ ...issuesReset, projects: [issue.projectKey] });
          break;
        case 'file':
          onFilterChange({ ...issuesReset, files: [issue.componentUuid] });
      }
    }
  };

  render() {
    const { branchLike, component, issue, previousIssue } = this.props;

    const displayComponent =
      !previousIssue ||
      previousIssue.component !== issue.component ||
      previousIssue.branch !== issue.branch;

    return (
      <li className="issues-workspace-list-item" ref={(node) => (this.nodeRef = node)}>
        {displayComponent && (
          <div className="issues-workspace-list-component note">
            <ComponentBreadcrumbs component={component} issue={this.props.issue} />
          </div>
        )}
        <Issue
          branchLike={branchLike}
          checked={this.props.checked}
          issue={issue}
          onChange={this.props.onChange}
          onCheck={this.props.onCheck}
          onClick={this.props.onClick}
          onFilter={this.handleFilter}
          onPopupToggle={this.props.onPopupToggle}
          openPopup={this.props.openPopup}
          selected={this.props.selected}
        />
      </li>
    );
  }
}
