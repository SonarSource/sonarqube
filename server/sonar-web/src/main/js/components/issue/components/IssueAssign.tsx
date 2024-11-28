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
import { LabelValueSelectOption, PopupZLevel, SearchSelectDropdown } from 'design-system';
import * as React from 'react';
import { Options, SingleValue } from 'react-select';
import { getUsers } from '../../../api/users';
import { CurrentUserContext } from '../../../app/components/current-user/CurrentUserContext';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Issue } from '../../../types/types';
import { RestUser, isLoggedIn, isUserActive } from '../../../types/users';
import Avatar from '../../ui/Avatar';

interface Props {
  organization: string;
  canAssign: boolean;
  isOpen: boolean;
  issue: Pick<Issue, 'assignee' | 'assigneeActive' | 'assigneeAvatar' | 'assigneeName' | 'projectOrganization' | 'organization'>;
  onAssign: (login: string) => void;
  togglePopup: (popup: string, show?: boolean) => void;
}

const minSearchLength = 2;

const UNASSIGNED = { value: '', label: translate('unassigned') };

const renderAvatar = (name?: string, avatar?: string) => (
  <Avatar hash={avatar} name={name} size="xs" className="sw-my-1" />
);

export default function IssueAssignee(props: Props) {
  const {
    canAssign,
    issue: { assignee, assigneeName, assigneeLogin, assigneeAvatar, organization },
  } = props;

  const assinedUser = assigneeName ?? assignee;
  const { currentUser } = React.useContext(CurrentUserContext);

  const allowCurrentUserSelection = isLoggedIn(currentUser) && currentUser?.login !== assigneeLogin;

  const defaultOptions = allowCurrentUserSelection
    ? [
        UNASSIGNED,
        {
          value: currentUser.login,
          label: currentUser.name,
          Icon: renderAvatar(currentUser.name, currentUser.avatar),
        },
      ]
    : [UNASSIGNED];

  const controlLabel = assinedUser ? (
    <>
      {renderAvatar(assinedUser, assigneeAvatar)} {assinedUser}
    </>
  ) : (
    UNASSIGNED.label
  );

  const toggleAssign = (open?: boolean) => {
    props.togglePopup('assign', open);
  };

  const handleClose = () => {
    toggleAssign(false);
  };

  const handleSearchAssignees = (
    query: string,
    cb: (options: Options<LabelValueSelectOption<string>>) => void,
  ) => {
    getUsers<RestUser>({ q: query, organization: organization})
      .then((result) => {
        const options: Array<LabelValueSelectOption<string>> = result.users
          .filter(isUserActive)
          .map((u) => ({
            label: u.name ?? u.login,
            value: u.login,
            Icon: renderAvatar(u.name, u.avatar),
          }));
        cb(options);
      })
      .catch(() => {
        cb([]);
      });
  };

  const renderAssignee = () => {
    const { issue } = props;
    const assigneeName = (issue.assigneeActive && issue.assigneeName) || issue.assignee;

    if (assigneeName) {
      return (
        <span className="sw-flex sw-items-center sw-gap-1">
          <Avatar className="sw-mr-1" hash={issue.assigneeAvatar} name={assigneeName} size="xs" />
          <span className="sw-truncate sw-max-w-abs-300 fs-mask">
            {issue.assigneeActive
              ? assigneeName
              : translateWithParameters('user.x_deleted', assigneeName)}
          </span>
        </span>
      );
    }

    return <span className="sw-flex sw-items-center sw-gap-1">{translate('unassigned')}</span>;
  };

  const handleAssign = (userOption: SingleValue<LabelValueSelectOption<string>>) => {
    if (userOption) {
      props.onAssign(userOption.value);
    }
  };

  if (!canAssign) {
    return renderAssignee();
  }

  return (
    <div className="sw-relative">
      <SearchSelectDropdown
        size="medium"
        className="it__issue-assign"
        controlAriaLabel={
          assinedUser
            ? translateWithParameters('issue.assign.assigned_to_x_click_to_change', assinedUser)
            : translate('issue.assign.unassigned_click_to_assign')
        }
        defaultOptions={defaultOptions}
        onChange={handleAssign}
        loadOptions={handleSearchAssignees}
        menuIsOpen={props.isOpen}
        minLength={minSearchLength}
        onMenuOpen={() => toggleAssign(true)}
        onMenuClose={handleClose}
        isDiscreet
        controlLabel={controlLabel}
        placeholder={translate('search.search_for_users')}
        aria-label={translate('search.search_for_users')}
        zLevel={PopupZLevel.Absolute}
      />
    </div>
  );
}
