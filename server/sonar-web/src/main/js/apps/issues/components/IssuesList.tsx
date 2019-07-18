/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { Query, scrollToIssue } from '../utils';
import ListItem from './ListItem';

interface Props {
  branchLike: T.BranchLike | undefined;
  checked: string[];
  component: T.Component | undefined;
  issues: T.Issue[];
  onFilterChange: (changes: Partial<Query>) => void;
  onIssueChange: (issue: T.Issue) => void;
  onIssueCheck: ((issueKey: string) => void) | undefined;
  onIssueClick: (issueKey: string) => void;
  onPopupToggle: (issue: string, popupName: string, open?: boolean) => void;
  openPopup: { issue: string; name: string } | undefined;
  organization: { key: string } | undefined;
  selectedIssue: T.Issue | undefined;
}

interface State {
  prerender: boolean;
}

export default class IssuesList extends React.PureComponent<Props, State> {
  state: State = {
    prerender: true
  };

  componentDidMount() {
    // ! \\ This prerender state variable is to enable the page to be displayed
    //      immediately, displaying a loader before attempting to render the
    //      list of issues. See https://jira.sonarsource.com/browse/SONAR-11681
    setTimeout(() => {
      this.setState({ prerender: false });
      if (this.props.selectedIssue) {
        scrollToIssue(this.props.selectedIssue.key, false);
      }
    }, 42);
  }

  render() {
    const { branchLike, checked, component, issues, openPopup, selectedIssue } = this.props;
    const { prerender } = this.state;

    if (prerender) {
      return (
        <div>
          <i className="spinner" />
        </div>
      );
    }

    return (
      <div>
        {issues.map((issue, index) => (
          <ListItem
            branchLike={branchLike}
            checked={checked.includes(issue.key)}
            component={component}
            issue={issue}
            key={issue.key}
            onChange={this.props.onIssueChange}
            onCheck={this.props.onIssueCheck}
            onClick={this.props.onIssueClick}
            onFilterChange={this.props.onFilterChange}
            onPopupToggle={this.props.onPopupToggle}
            openPopup={openPopup && openPopup.issue === issue.key ? openPopup.name : undefined}
            organization={this.props.organization}
            previousIssue={index > 0 ? issues[index - 1] : undefined}
            selected={selectedIssue != null && selectedIssue.key === issue.key}
          />
        ))}
      </div>
    );
  }
}
