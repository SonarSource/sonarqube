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
import { find, without } from 'lodash';
import * as React from 'react';
import { addUserToGroup, getUsersInGroup, removeUserFromGroup } from '../../../api/user_groups';
import Modal from '../../../components/controls/Modal';
import SelectList, {
  SelectListFilter,
  SelectListSearchParams,
} from '../../../components/controls/SelectList';
import { ResetButtonLink } from '../../../components/controls/buttons';
import { translate } from '../../../helpers/l10n';
import { Group, UserSelected } from '../../../types/types';

interface Props {
  group: Group;
  onClose: () => void;
}

export default function EditMembersModal(props: Readonly<Props>) {
  const [needToReload, setNeedToReload] = React.useState(false);
  const [users, setUsers] = React.useState<UserSelected[]>([]);
  const [selectedUsers, setSelectedUsers] = React.useState<string[]>([]);
  const [usersTotalCount, setUsersTotalCount] = React.useState<number | undefined>(undefined);
  const [lastSearchParams, setLastSearchParams] = React.useState<
    SelectListSearchParams | undefined
  >(undefined);

  const { group } = props;
  const modalHeader = translate('users.update');

  const fetchUsers = (searchParams: SelectListSearchParams) =>
    getUsersInGroup({
      name: props.group.name,
      p: searchParams.page,
      ps: searchParams.pageSize,
      q: searchParams.query !== '' ? searchParams.query : undefined,
      selected: searchParams.filter,
    }).then((data) => {
      const more = searchParams.page != null && searchParams.page > 1;

      setUsers(more ? [...users, ...data.users] : data.users);
      const newSelectedUsers = data.users.filter((user) => user.selected).map((user) => user.login);
      setSelectedUsers(more ? [...selectedUsers, ...newSelectedUsers] : newSelectedUsers);
      setNeedToReload(false);
      setLastSearchParams(searchParams);
      setUsersTotalCount(data.paging.total);
    });

  const handleSelect = (login: string) =>
    addUserToGroup({
      name: group.name,
      login,
    }).then(() => {
      setNeedToReload(true);
      setSelectedUsers([...selectedUsers, login]);
    });

  const handleUnselect = (login: string) =>
    removeUserFromGroup({
      name: group.name,
      login,
    }).then(() => {
      setNeedToReload(true);
      setSelectedUsers(without(selectedUsers, login));
    });

  const renderElement = (login: string): React.ReactNode => {
    const user = find(users, { login });
    return (
      <div className="select-list-list-item">
        {user === undefined ? (
          login
        ) : (
          <>
            {user.name}
            <br />
            <span className="note">{user.login}</span>
          </>
        )}
      </div>
    );
  };

  return (
    <Modal
      className="group-menbers-modal"
      contentLabel={modalHeader}
      onRequestClose={props.onClose}
    >
      <header className="modal-head">
        <h2>{modalHeader}</h2>
      </header>

      <div className="modal-body modal-container">
        <SelectList
          elements={users.map((user) => user.login)}
          elementsTotalCount={usersTotalCount}
          needToReload={
            needToReload && lastSearchParams && lastSearchParams.filter !== SelectListFilter.All
          }
          onSearch={fetchUsers}
          onSelect={handleSelect}
          onUnselect={handleUnselect}
          renderElement={renderElement}
          selectedElements={selectedUsers}
          withPaging
        />
      </div>

      <footer className="modal-foot">
        <ResetButtonLink onClick={props.onClose}>{translate('done')}</ResetButtonLink>
      </footer>
    </Modal>
  );
}
