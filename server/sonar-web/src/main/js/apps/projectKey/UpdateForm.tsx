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
import ProjectKeyInput from '../../components/common/ProjectKeyInput';
import { Button, SubmitButton } from '../../components/controls/buttons';
import ConfirmButton from '../../components/controls/ConfirmButton';
import MandatoryFieldsExplanation from '../../components/ui/MandatoryFieldsExplanation';
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
  const [newKey, setNewKey] = React.useState<string | undefined>(undefined);
  const value = newKey !== undefined ? newKey : component.key;
  const hasChanged = value !== component.key;

  const validationResult = validateProjectKey(value);
  const error =
    validationResult === ProjectKeyValidationResult.Valid
      ? undefined
      : translate('onboarding.create_project.project_key.error', validationResult);

  return (
    <ConfirmButton
      confirmButtonText={translate('update_verb')}
      confirmData={newKey}
      modalBody={
        <>
          {translateWithParameters('update_key.are_you_sure_to_change_key', component.name)}
          <div className="spacer-top">
            {translate('update_key.old_key')}
            {': '}
            <strong>{component.key}</strong>
          </div>
          <div className="spacer-top">
            {translate('update_key.new_key')}
            {': '}
            <strong>{newKey}</strong>
          </div>
        </>
      }
      modalHeader={translate('update_key.page')}
      onConfirm={props.onKeyChange}
    >
      {({ onFormSubmit }) => (
        <form onSubmit={onFormSubmit}>
          <MandatoryFieldsExplanation className="spacer-bottom" />

          <ProjectKeyInput
            error={error}
            label={translate('update_key.new_key')}
            onProjectKeyChange={(e: React.ChangeEvent<HTMLInputElement>) => {
              setNewKey(e.currentTarget.value);
            }}
            touched={hasChanged}
            placeholder={translate('update_key.new_key')}
            projectKey={value}
            autofocus={true}
          />

          <div className="spacer-top">
            <SubmitButton disabled={!hasChanged || error !== undefined} id="update-key-submit">
              {translate('update_verb')}
            </SubmitButton>

            <Button
              className="spacer-left"
              disabled={!hasChanged}
              id="update-key-reset"
              onClick={() => {
                setNewKey(undefined);
              }}
              type="reset"
            >
              {translate('reset_verb')}
            </Button>
          </div>
        </form>
      )}
    </ConfirmButton>
  );
}
