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
import { FormField, InputField, InputTextArea, Modal } from 'design-system';
import * as React from 'react';
import { useState } from 'react';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate } from '../../../helpers/l10n';
import { useCreateGroupMutation, useUpdateGroupMutation } from '../../../queries/groups';
import { Group } from '../../../types/types';

type Props =
  | {
      create: true;
      group?: undefined;
      onClose: () => void;
    }
  | {
      create: false;
      group: Group;
      onClose: () => void;
    };

export default function GroupForm(props: Props) {
  const { group, create } = props;

  const [name, setName] = useState<string>(create ? '' : group.name);
  const [description, setDescription] = useState<string>(create ? '' : group.description ?? '');

  const { mutate: createGroup, isPending: isCreating } = useCreateGroupMutation();
  const { mutate: updateGroup, isPending: isUpdating } = useUpdateGroupMutation();

  const handleCreateGroup = () => {
    createGroup({ name, description }, { onSuccess: props.onClose });
  };

  const handleUpdateGroup = () => {
    if (!group) {
      return;
    }
    updateGroup(
      {
        id: group.id,
        data: {
          name,
          description,
        },
      },
      { onSuccess: props.onClose },
    );
  };

  return (
    <Modal
      headerTitle={create ? translate('groups.create_group') : translate('groups.update_group')}
      body={
        <>
          <MandatoryFieldsExplanation className="sw-block sw-mb-4" />
          <FormField htmlFor="create-group-name" label={translate('name')} required>
            <InputField
              autoFocus
              id="create-group-name"
              maxLength={255}
              name="name"
              onChange={(event: React.SyntheticEvent<HTMLInputElement>) => {
                setName(event.currentTarget.value);
              }}
              required
              size="full"
              type="text"
              value={name}
            />
          </FormField>
          <FormField htmlFor="create-group-description" label={translate('description')}>
            <InputTextArea
              id="create-group-description"
              name="description"
              onChange={(event: React.SyntheticEvent<HTMLTextAreaElement>) => {
                setDescription(event.currentTarget.value);
              }}
              size="full"
              value={description}
            />
          </FormField>
        </>
      }
      onClose={props.onClose}
      primaryButton={
        <Button
          isDisabled={isUpdating || isCreating || name === ''}
          onClick={create ? handleCreateGroup : handleUpdateGroup}
          variety={ButtonVariety.Primary}
        >
          {create ? translate('create') : translate('update_verb')}
        </Button>
      }
      secondaryButtonLabel={translate('cancel')}
    />
  );
}
