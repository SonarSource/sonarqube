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

import { FlagMessage, LightPrimary, Modal, Note } from 'design-system';
import { find } from 'lodash';
import * as React from 'react';
import SelectList, {
  SelectListFilter,
  SelectListSearchParams,
} from '../../../components/controls/SelectList';
import { translate } from '../../../helpers/l10n';
import { definitions } from '../../../helpers/mocks/definitions-list';
import {
  useAddGroupMembershipMutation,
  useRemoveGroupMembershipMutation,
  useUserGroupsQuery,
} from '../../../queries/group-memberships';
import { RestUserDetailed } from '../../../types/users';
import useSamlConfiguration from '../../settings/components/authentication/hook/useSamlConfiguration';
import { SAML } from '../../settings/components/authentication/SamlAuthenticationTab';

const samlDefinitions = definitions.filter((def) => def.subCategory === SAML);

interface Props {
  onClose: () => void;
  user: RestUserDetailed;
}

export default function GroupsForm(props: Props) {
  const { user } = props;
  const [query, setQuery] = React.useState<string>('');
  const [filter, setFilter] = React.useState<SelectListFilter>(SelectListFilter.Selected);
  const [changedGroups, setChangedGroups] = React.useState<Map<string, boolean>>(new Map());
  const {
    data: groups,
    isLoading,
    refetch,
  } = useUserGroupsQuery({
    q: query,
    filter,
    userId: user.id,
  });
  const { mutateAsync: addUserToGroup } = useAddGroupMembershipMutation();
  const { mutateAsync: removeUserFromGroup } = useRemoveGroupMembershipMutation();

  const { samlEnabled } = useSamlConfiguration(samlDefinitions);

  const onSearch = (searchParams: SelectListSearchParams) => {
    if (query === searchParams.query && filter === searchParams.filter) {
      refetch();
    } else {
      setQuery(searchParams.query);
      setFilter(searchParams.filter);
    }

    setChangedGroups(new Map());
  };

  const handleSelect = (groupId: string) =>
    addUserToGroup({
      userId: user.id,
      groupId,
    }).then(() => {
      const newChangedGroups = new Map(changedGroups);
      newChangedGroups.set(groupId, true);
      setChangedGroups(newChangedGroups);
    });

  const handleUnselect = (groupId: string) =>
    removeUserFromGroup({
      groupId,
      userId: user.id,
    }).then(() => {
      const newChangedGroups = new Map(changedGroups);
      newChangedGroups.set(groupId, false);
      setChangedGroups(newChangedGroups);
    });

  const renderElement = (groupId: string): React.ReactNode => {
    const group = find(groups, { id: groupId });
    return (
      <div>
        {group === undefined ? (
          <LightPrimary>{groupId}</LightPrimary>
        ) : (
          <>
            <LightPrimary>{group.name}</LightPrimary>
            <br />
            <Note>{group.description}</Note>
          </>
        )}
      </div>
    );
  };

  const header = translate('users.update_groups');

  return (
    <Modal
      headerTitle={header}
      body={
        <div className="sw-pt-1">
          {samlEnabled && (
            <FlagMessage className="sw-mb-2" variant="warning">
              {translate('users.update_groups.saml_enabled')}
            </FlagMessage>
          )}
          <SelectList
            elements={groups?.map((group) => group.id.toString()) ?? []}
            elementsTotalCount={groups?.length}
            needToReload={changedGroups.size > 0 && filter !== SelectListFilter.All}
            onSearch={onSearch}
            onSelect={handleSelect}
            onUnselect={handleUnselect}
            renderElement={renderElement}
            selectedElements={
              groups
                ?.filter((g) => (changedGroups.has(g.id) ? changedGroups.get(g.id) : g.selected))
                .map((g) => g.id) ?? []
            }
            loading={isLoading}
          />
        </div>
      }
      onClose={props.onClose}
      primaryButton={null}
      secondaryButtonLabel={translate('done')}
    />
  );
}
