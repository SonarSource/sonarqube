/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { groupBy } from 'lodash';
import * as React from 'react';
import { BranchLike } from '../../../types/branch-like';
import { Component, Issue } from '../../../types/types';
import { Query } from '../utils';
import ComponentBreadcrumbs from './ComponentBreadcrumbs';
import ListItem from './ListItem';

interface Props {
  branchLike: BranchLike | undefined;
  checked: string[];
  component: Component | undefined;
  issues: Issue[];
  onFilterChange: (changes: Partial<Query>) => void;
  onIssueChange: (issue: Issue) => void;
  onIssueCheck: ((issueKey: string) => void) | undefined;
  onIssueClick: (issueKey: string) => void;
  onPopupToggle: (issue: string, popupName: string, open?: boolean) => void;
  openPopup: { issue: string; name: string } | undefined;
  selectedIssue: Issue | undefined;
}

interface State {
  prerender: boolean;
}

export default class IssuesList extends React.PureComponent<Props, State> {
  state: State = {
    prerender: true,
  };

  componentDidMount() {
    if (this.props.issues.length > 0) {
      this.setState({ prerender: false });
    }
  }

  componentDidUpdate() {
    if (this.props.issues.length > 0) {
      this.setState({ prerender: false });
    }
  }

  renderIssueComponentList = (issues: Issue[], index: number) => {
    const { branchLike, checked, component, openPopup, selectedIssue } = this.props;
    return (
      <React.Fragment key={index}>
        <li>
          <div className="issues-workspace-list-component note">
            <ComponentBreadcrumbs component={component} issue={issues[0]} />
          </div>
        </li>
        <ul>
          {issues.map((issue) => (
            <ListItem
              branchLike={branchLike}
              checked={checked.includes(issue.key)}
              issue={issue}
              key={issue.key}
              onChange={this.props.onIssueChange}
              onCheck={this.props.onIssueCheck}
              onClick={this.props.onIssueClick}
              onFilterChange={this.props.onFilterChange}
              onPopupToggle={this.props.onPopupToggle}
              openPopup={openPopup && openPopup.issue === issue.key ? openPopup.name : undefined}
              selected={selectedIssue != null && selectedIssue.key === issue.key}
            />
          ))}
        </ul>
      </React.Fragment>
    );
  };

  render() {
    const { issues } = this.props;
    const { prerender } = this.state;

    if (prerender) {
      return (
        <div>
          <i className="spinner" />
        </div>
      );
    }

    const issuesByComponent = groupBy(issues, (issue) => `(${issue.component} : ${issue.branch})`);

    return <ul>{Object.values(issuesByComponent).map(this.renderIssueComponentList)}</ul>;
  }
}
