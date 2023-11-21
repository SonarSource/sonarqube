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
import * as React from 'react';
import { useState } from 'react';
import SimpleModal from '../../../components/controls/SimpleModal';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import MandatoryFieldMarker from '../../../components/ui/MandatoryFieldMarker';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import Spinner from '../../../components/ui/Spinner';
import { translate } from '../../../helpers/l10n';
import { omitNil } from '../../../helpers/request';
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

  const { mutate: createGroup } = useCreateGroupMutation();
  const { mutate: updateGroup } = useUpdateGroupMutation();

  const handleCreateGroup = () => {
    createGroup({ name, description }, { onSuccess: props.onClose });
  };

  const handleUpdateGroup = () => {
    if (!group) {
      return;
    }
    updateGroup(
      {
        currentName: group.name,
        description,
        // pass `name` only if it has changed, otherwise the WS fails
        ...omitNil({ name: name !== group.name ? name : undefined }),
      },
      { onSuccess: props.onClose },
    );
  };

  return (
    <SimpleModal
      header={create ? translate('groups.create_group') : translate('groups.update_group')}
      onClose={props.onClose}
      onSubmit={create ? handleCreateGroup : handleUpdateGroup}
      size="small"
    >
      {({ onCloseClick, onFormSubmit, submitting }) => (
        <form onSubmit={onFormSubmit}>
          <header className="modal-head">
            <h2>{create ? translate('groups.create_group') : translate('groups.update_group')}</h2>
          </header>

          <div className="modal-body">
            <MandatoryFieldsExplanation className="modal-field" />
            <div className="modal-field">
              <label htmlFor="create-group-name">
                {translate('name')}
                <MandatoryFieldMarker />
              </label>
              <input
                autoFocus
                id="create-group-name"
                maxLength={255}
                name="name"
                onChange={(event: React.SyntheticEvent<HTMLInputElement>) => {
                  setName(event.currentTarget.value);
                }}
                required
                size={50}
                type="text"
                value={name}
              />
            </div>
            <div className="modal-field">
              <label htmlFor="create-group-description">{translate('description')}</label>
              <textarea
                id="create-group-description"
                name="description"
                onChange={(event: React.SyntheticEvent<HTMLTextAreaElement>) => {
                  setDescription(event.currentTarget.value);
                }}
                value={description}
              />
            </div>
          </div>

          <footer className="modal-foot">
            <Spinner className="spacer-right" loading={submitting} />
            <SubmitButton disabled={submitting}>
              {create ? translate('create') : translate('update_verb')}
            </SubmitButton>
            <ResetButtonLink onClick={onCloseClick}>{translate('cancel')}</ResetButtonLink>
          </footer>
        </form>
      )}
    </SimpleModal>
  );
}
