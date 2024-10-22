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
import {
  ButtonPrimary,
  ButtonSecondary,
  FlagMessage,
  FormField,
  InputField,
  Note,
} from '~design-system';
import ConfirmButton from '../../components/controls/ConfirmButton';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { validateProjectKey } from '../../helpers/projects';
import { ProjectKeyValidationResult } from '../../types/component';
import { Component } from '../../types/types';

export interface UpdateFormProps {
  component: Pick<Component, 'key' | 'name'>;
  onKeyChange: (newKey: string) => Promise<void>;
}

export default function UpdateForm(props: UpdateFormProps) {
  const { component } = props;
  const [newKey, setNewKey] = React.useState(component.key);
  const hasChanged = newKey !== component.key;

  const validationResult = validateProjectKey(newKey);
  const error =
    validationResult === ProjectKeyValidationResult.Valid
      ? undefined
      : translate('onboarding.create_project.project_key.error', validationResult);

  const onInputChange = React.useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      setNewKey(e.currentTarget.value);
    },
    [setNewKey],
  );

  return (
    <ConfirmButton
      confirmButtonText={translate('update_verb')}
      confirmData={newKey}
      modalBody={
        <>
          {translateWithParameters('update_key.are_you_sure_to_change_key', component.name)}
          <div className="sw-mt-2">
            {translate('update_key.old_key')}
            {': '}
            <strong className="sw-typo-lg-semibold">{component.key}</strong>
          </div>
          <div className="sw-mt-2">
            {translate('update_key.new_key')}
            {': '}
            <strong className="sw-typo-lg-semibold">{newKey}</strong>
          </div>
        </>
      }
      modalHeader={translate('update_key.page')}
      onConfirm={props.onKeyChange}
    >
      {({ onFormSubmit }) => (
        <form onSubmit={onFormSubmit}>
          <FormField label={translate('update_key.new_key')} required>
            <InputField
              id="project-key"
              name="update_key.new_key"
              required
              isInvalid={hasChanged && error !== undefined}
              isValid={hasChanged && error === undefined}
              autoFocus
              onChange={onInputChange}
              value={newKey}
              type="text"
            />

            {error && (
              <FlagMessage className="sw-mt-2 sw-w-abs-400" variant="error">
                {error}
              </FlagMessage>
            )}

            <Note className="sw-mt-2 sw-max-w-1/2">
              {translate('onboarding.create_project.project_key.description')}
            </Note>
          </FormField>

          <div className="sw-mt-2">
            <ButtonPrimary
              disabled={!hasChanged || error !== undefined}
              id="update-key-submit"
              type="submit"
            >
              {translate('update_verb')}
            </ButtonPrimary>

            <ButtonSecondary
              className="sw-ml-2"
              disabled={!hasChanged}
              id="update-key-reset"
              onClick={() => {
                setNewKey(component.key);
              }}
              type="reset"
            >
              {translate('reset_verb')}
            </ButtonSecondary>
          </div>
        </form>
      )}
    </ConfirmButton>
  );
}
