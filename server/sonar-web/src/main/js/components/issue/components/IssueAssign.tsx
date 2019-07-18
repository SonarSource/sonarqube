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
import { ButtonLink } from 'sonar-ui-common/components/controls/buttons';
import Toggler from 'sonar-ui-common/components/controls/Toggler';
import DropdownIcon from 'sonar-ui-common/components/icons/DropdownIcon';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import Avatar from '../../ui/Avatar';
import SetAssigneePopup from '../popups/SetAssigneePopup';

interface Props {
  isOpen: boolean;
  issue: Pick<
    T.Issue,
    'assignee' | 'assigneeActive' | 'assigneeAvatar' | 'assigneeName' | 'projectOrganization'
  >;
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
    const assignee =
      issue.assigneeActive !== false ? issue.assigneeName || issue.assignee : issue.assignee;

    if (assignee) {
      return (
        <>
          <span className="text-top">
            <Avatar
              className="little-spacer-right"
              hash={issue.assigneeAvatar}
              name={issue.assigneeName || issue.assignee}
              size={16}
            />
          </span>
          <span className="issue-meta-label">
            {issue.assigneeActive === false
              ? translateWithParameters('user.x_deleted', assignee)
              : assignee}
          </span>
        </>
      );
    }

    return <span className="issue-meta-label">{translate('unassigned')}</span>;
  }

  render() {
    if (this.props.canAssign) {
      return (
        <div className="dropdown">
          <Toggler
            closeOnEscape={true}
            onRequestClose={this.handleClose}
            open={this.props.isOpen && this.props.canAssign}
            overlay={<SetAssigneePopup issue={this.props.issue} onSelect={this.props.onAssign} />}>
            <ButtonLink
              className="issue-action issue-action-with-options js-issue-assign"
              onClick={this.toggleAssign}>
              {this.renderAssignee()}
              <DropdownIcon className="little-spacer-left" />
            </ButtonLink>
          </Toggler>
        </div>
      );
    } else {
      return this.renderAssignee();
    }
  }
}
