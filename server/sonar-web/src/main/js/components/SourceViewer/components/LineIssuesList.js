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
import Issue from '../../issue/Issue';
/*:: import type { Issue as IssueType } from '../../issue/types'; */

/*::
type Props = {
  branch?: string,
  displayIssueLocationsCount?: boolean;
  displayIssueLocationsLink?: boolean;
  issues: Array<IssueType>,
  onIssueChange: IssueType => void,
  onIssueClick: (issueKey: string) => void,
  onPopupToggle: (issue: string, popupName: string, open: ?boolean ) => void,
  openPopup: ?{ issue: string, name: string},
  selectedIssue: string | null
};
*/

export default class LineIssuesList extends React.PureComponent {
  /*:: props: Props; */

  render() {
    const { branch, issues, onIssueClick, openPopup, selectedIssue } = this.props;

    return (
      <div className="issue-list">
        {issues.map(issue => (
          <Issue
            branch={branch}
            displayLocationsCount={this.props.displayIssueLocationsCount}
            displayLocationsLink={this.props.displayIssueLocationsLink}
            issue={issue}
            key={issue.key}
            onChange={this.props.onIssueChange}
            onClick={onIssueClick}
            onPopupToggle={this.props.onPopupToggle}
            openPopup={openPopup && openPopup.issue === issue.key ? openPopup.name : null}
            selected={selectedIssue === issue.key}
          />
        ))}
      </div>
    );
  }
}
