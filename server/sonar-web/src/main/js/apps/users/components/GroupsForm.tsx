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
import { find, without } from 'lodash';
import * as React from 'react';
import { UserGroup, getUserGroups } from '../../../api/users';
import Modal from '../../../components/controls/Modal';
import SelectList, {
  SelectListFilter,
  SelectListSearchParams,
} from '../../../components/controls/SelectList';
import { ResetButtonLink } from '../../../components/controls/buttons';
import { translate } from '../../../helpers/l10n';
import { useAddUserToGroupMutation, useRemoveUserToGroupMutation } from '../../../queries/users';
import { RestUserDetailed } from '../../../types/users';

interface Props {
  onClose: () => void;
  user: RestUserDetailed;
}

export default function GroupsForm(props: Props) {
  const { user } = props;
  const [needToReload, setNeedToReload] = React.useState<boolean>(false);
  const [lastSearchParams, setLastSearchParams] = React.useState<
    SelectListSearchParams | undefined
  >(undefined);
  const [groups, setGroups] = React.useState<UserGroup[]>([]);
  const [groupsTotalCount, setGroupsTotalCount] = React.useState<number | undefined>(undefined);
  const [selectedGroups, setSelectedGroups] = React.useState<string[]>([]);
  const { mutateAsync: addUserToGroup } = useAddUserToGroupMutation();
  const { mutateAsync: removeUserFromGroup } = useRemoveUserToGroupMutation();

  const fetchUsers = (searchParams: SelectListSearchParams) =>
    getUserGroups({
      login: user.login,
      p: searchParams.page,
      ps: searchParams.pageSize,
      q: searchParams.query !== '' ? searchParams.query : undefined,
      selected: searchParams.filter,
    }).then((data) => {
      const more = searchParams.page != null && searchParams.page > 1;
      const allGroups = more ? [...groups, ...data.groups] : data.groups;
      const newSeletedGroups = data.groups.filter((gp) => gp.selected).map((gp) => gp.name);
      const allSelectedGroups = more ? [...selectedGroups, ...newSeletedGroups] : newSeletedGroups;

      setLastSearchParams(searchParams);
      setNeedToReload(false);
      setGroups(allGroups);
      setGroupsTotalCount(data.paging.total);
      setSelectedGroups(allSelectedGroups);
    });

  const handleSelect = (name: string) =>
    addUserToGroup({
      name,
      login: user.login,
    }).then(() => {
      setNeedToReload(true);
      setSelectedGroups([...selectedGroups, name]);
    });

  const handleUnselect = (name: string) =>
    removeUserFromGroup({
      name,
      login: user.login,
    }).then(() => {
      setNeedToReload(true);
      setSelectedGroups(without(selectedGroups, name));
    });

  const renderElement = (name: string): React.ReactNode => {
    const group = find(groups, { name });
    return (
      <div className="select-list-list-item">
        {group === undefined ? (
          name
        ) : (
          <>
            {group.name}
            <br />
            <span className="note">{group.description}</span>
          </>
        )}
      </div>
    );
  };

  const header = translate('users.update_groups');

  return (
    <Modal contentLabel={header} onRequestClose={props.onClose}>
      <div className="modal-head">
        <h2>{header}</h2>
      </div>

      <div className="modal-body modal-container">
        <SelectList
          elements={groups.map((group) => group.name)}
          elementsTotalCount={groupsTotalCount}
          needToReload={
            needToReload && lastSearchParams && lastSearchParams.filter !== SelectListFilter.All
          }
          onSearch={fetchUsers}
          onSelect={handleSelect}
          onUnselect={handleUnselect}
          renderElement={renderElement}
          selectedElements={selectedGroups}
          withPaging
        />
      </div>

      <footer className="modal-foot">
        <ResetButtonLink onClick={props.onClose}>{translate('done')}</ResetButtonLink>
      </footer>
    </Modal>
  );
}
