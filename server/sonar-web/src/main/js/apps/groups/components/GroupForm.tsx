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
import { useCallback, useEffect, useState } from 'react';
import { createGroup, updateGroup } from '../../../api/user_groups';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import SimpleModal from '../../../components/controls/SimpleModal';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import MandatoryFieldMarker from '../../../components/ui/MandatoryFieldMarker';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate } from '../../../helpers/l10n';
import { omitNil } from '../../../helpers/request';
import { Group } from '../../../types/types';

type Props =
  | {
      create: true;
      group?: undefined;
      onClose: () => void;
      reload: () => void;
    }
  | {
      create: false;
      group: Group;
      onClose: () => void;
      reload: () => void;
    };

export default function GroupForm(props: Props) {
  const { group, create, reload, onClose } = props;

  const [name, setName] = useState<string>('');
  const [description, setDescription] = useState<string>('');

  const handleSubmit = useCallback(async () => {
    try {
      if (create) {
        await createGroup({ name, description });
      } else {
        const data = {
          currentName: group.name,
          description,
          // pass `name` only if it has changed, otherwise the WS fails
          ...omitNil({ name: name !== group.name ? name : undefined }),
        };
        await updateGroup(data);
      }
    } finally {
      reload();
      onClose();
    }
  }, [name, description, group, create, reload, onClose]);

  useEffect(() => {
    if (!create) {
      setDescription(group.description ?? '');
      setName(group.name);
    }
  }, []);

  return (
    <SimpleModal
      header={create ? translate('groups.create_group') : translate('groups.update_group')}
      onClose={props.onClose}
      onSubmit={handleSubmit}
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
                autoFocus={true}
                id="create-group-name"
                maxLength={255}
                name="name"
                onChange={(event: React.SyntheticEvent<HTMLInputElement>) => {
                  setName(event.currentTarget.value);
                }}
                required={true}
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
            <DeferredSpinner className="spacer-right" loading={submitting} />
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
