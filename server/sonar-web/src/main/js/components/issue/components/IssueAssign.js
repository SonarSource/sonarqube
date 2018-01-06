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
import Avatar from '../../../components/ui/Avatar';
import BubblePopupHelper from '../../../components/common/BubblePopupHelper';
import SetAssigneePopup from '../popups/SetAssigneePopup';
import { translate } from '../../../helpers/l10n';
/*:: import type { Issue } from '../types'; */

/*::
type Props = {
  isOpen: boolean,
  issue: Issue,
  canAssign: boolean,
  onAssign: string => void,
  onFail: Error => void,
  togglePopup: (string, boolean | void) => void
};
*/

export default class IssueAssign extends React.PureComponent {
  /*:: props: Props; */

  toggleAssign = (open /*: boolean | void */) => {
    this.props.togglePopup('assign', open);
  };

  renderAssignee() {
    const { issue } = this.props;
    return (
      <span>
        {issue.assignee && (
          <span className="text-top">
            <Avatar
              className="little-spacer-right"
              hash={issue.assigneeAvatar}
              name={issue.assigneeName}
              size={16}
            />
          </span>
        )}
        <span className="issue-meta-label">
          {issue.assignee ? issue.assigneeName : translate('unassigned')}
        </span>
      </span>
    );
  }

  render() {
    if (this.props.canAssign) {
      return (
        <BubblePopupHelper
          isOpen={this.props.isOpen && this.props.canAssign}
          position="bottomleft"
          togglePopup={this.toggleAssign}
          popup={
            <SetAssigneePopup
              issue={this.props.issue}
              onFail={this.props.onFail}
              onSelect={this.props.onAssign}
            />
          }>
          <button
            className="button-link issue-action issue-action-with-options js-issue-assign"
            onClick={this.toggleAssign}>
            {this.renderAssignee()}
            <i className="little-spacer-left icon-dropdown" />
          </button>
        </BubblePopupHelper>
      );
    } else {
      return this.renderAssignee();
    }
  }
}
