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

import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import * as React from 'react';
import { FormField, Modal } from '~design-system';
import { translate } from '../../../helpers/l10n';
import { useAddGroupMutation, useAddUserMutation } from '../../../queries/quality-profiles';
import { UserSelected } from '../../../types/types';
import { Group } from './ProfilePermissions';
import ProfilePermissionsFormSelect from './ProfilePermissionsFormSelect';

interface Props {
  organization: string;
  onClose: () => void;
  onGroupAdd: (group: Group) => void;
  onUserAdd: (user: UserSelected) => void;
  profile: { language: string; name: string };
}

export default function ProfilePermissionForm(props: Readonly<Props>) {
  const { organization, profile } = props;
  const [selected, setSelected] = React.useState<UserSelected | Group>();

  const { mutate: addUser, isPending: addingUser } = useAddUserMutation(() =>
    props.onUserAdd(selected as UserSelected),
  );
  const { mutate: addGroup, isPending: addingGroup } = useAddGroupMutation(() =>
    props.onGroupAdd(selected as Group),
  );

  const loading = addingUser || addingGroup;

  const header = translate('quality_profiles.grant_permissions_to_user_or_group');
  const submitDisabled = !selected || loading;

  const handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (selected) {
      if (isSelectedUser(selected)) {
        addUser({
          organization,
          language: profile.language,
          login: selected.login,
          qualityProfile: profile.name,
        });
      } else {
        addGroup({
          organization,
          language: profile.language,
          group: selected.name,
          qualityProfile: profile.name,
        });
      }
    }
  };

  return (
    <Modal
      isOverflowVisible
      headerTitle={header}
      onClose={props.onClose}
      loading={loading}
      primaryButton={
        <Button
          type="submit"
          form="grant_permissions_form"
          isDisabled={submitDisabled}
          variety={ButtonVariety.Primary}
        >
          {translate('add_verb')}
        </Button>
      }
      secondaryButtonLabel={translate('cancel')}
      body={
        <form onSubmit={handleFormSubmit} id="grant_permissions_form">
          <FormField label={translate('quality_profiles.search_description')}>
            <ProfilePermissionsFormSelect
              organization={organization}
              onChange={(option) => setSelected(option)}
              selected={selected}
              profile={profile}
            />
          </FormField>
        </form>
      }
    />
  );
}

function isSelectedUser(selected: UserSelected | Group): selected is UserSelected {
  return (selected as UserSelected).login !== undefined;
}
