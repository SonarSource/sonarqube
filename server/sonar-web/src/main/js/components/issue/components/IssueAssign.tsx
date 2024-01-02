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
import { ButtonLink } from '../../../components/controls/buttons';
import Toggler from '../../../components/controls/Toggler';
import DropdownIcon from '../../../components/icons/DropdownIcon';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Issue } from '../../../types/types';
import Avatar from '../../ui/Avatar';
import SetAssigneePopup from '../popups/SetAssigneePopup';

interface Props {
  isOpen: boolean;
  issue: Issue;
  canAssign: boolean;
  onAssign: (login: string) => void;
  togglePopup: (popup: string, show?: boolean) => void;
}

export default class IssueAssign extends React.PureComponent<Props> {
  toggleAssign = (open?: boolean) => {
    this.props.togglePopup('assign', open);
  };

  handleClose = () => {
    this.toggleAssign(false);
  };

  renderAssignee() {
    const { issue } = this.props;
    const assigneeName = issue.assigneeName || issue.assignee;

    if (assigneeName) {
      const assigneeDisplay =
        issue.assigneeActive === false
          ? translateWithParameters('user.x_deleted', assigneeName)
          : assigneeName;
      return (
        <>
          <span className="text-top">
            <Avatar className="little-spacer-right" hash={issue.assigneeAvatar} name="" size={16} />
          </span>
          <span className="issue-meta-label" title={assigneeDisplay}>
            {assigneeDisplay}
          </span>
        </>
      );
    }

    return <span className="issue-meta-label">{translate('unassigned')}</span>;
  }

  render() {
    const { canAssign, isOpen, issue } = this.props;
    const assigneeName = issue.assigneeName || issue.assignee;

    if (canAssign) {
      return (
        <div className="dropdown">
          <Toggler
            closeOnEscape={true}
            onRequestClose={this.handleClose}
            open={isOpen}
            overlay={<SetAssigneePopup onSelect={this.props.onAssign} />}
          >
            <ButtonLink
              aria-expanded={isOpen}
              aria-label={
                assigneeName
                  ? translateWithParameters(
                      'issue.assign.assigned_to_x_click_to_change',
                      assigneeName
                    )
                  : translate('issue.assign.unassigned_click_to_assign')
              }
              className="issue-action issue-action-with-options js-issue-assign"
              onClick={this.toggleAssign}
            >
              {this.renderAssignee()}
              <DropdownIcon className="little-spacer-left" />
            </ButtonLink>
          </Toggler>
        </div>
      );
    }

    return this.renderAssignee();
  }
}
