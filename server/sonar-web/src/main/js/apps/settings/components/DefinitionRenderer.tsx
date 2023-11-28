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
import { FlagMessage, Note, Spinner, TextError } from 'design-system';
import * as React from 'react';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { ExtendedSettingDefinition, SettingValue } from '../../../types/settings';
import { combineDefinitionAndSettingValue, getSettingValue, isDefaultOrInherited } from '../utils';
import DefinitionActions from './DefinitionActions';
import DefinitionDescription from './DefinitionDescription';
import Input from './inputs/Input';

export interface DefinitionRendererProps {
  definition: ExtendedSettingDefinition;
  changedValue?: string;
  loading: boolean;
  success: boolean;
  validationMessage?: string;
  settingValue?: SettingValue;
  isEditing: boolean;
  onCancel: () => void;
  onChange: (value: any) => void;
  onEditing: () => void;
  onSave: () => void;
  onReset: () => void;
}

const formNoop = (e: React.FormEvent<HTMLFormElement>) => e.preventDefault();

export default function DefinitionRenderer(props: Readonly<DefinitionRendererProps>) {
  const { changedValue, loading, validationMessage, settingValue, success, definition, isEditing } =
    props;

  const hasError = validationMessage != null;
  const hasValueChanged = changedValue != null;
  const effectiveValue = hasValueChanged ? changedValue : getSettingValue(definition, settingValue);
  const isDefault = isDefaultOrInherited(settingValue);

  const settingDefinitionAndValue = combineDefinitionAndSettingValue(definition, settingValue);

  return (
    <div data-key={definition.key} className="sw-flex sw-gap-12">
      <DefinitionDescription definition={definition} />

      <div className="sw-flex-1">
        <form onSubmit={formNoop}>
          <Input
            hasValueChanged={hasValueChanged}
            onCancel={props.onCancel}
            onChange={props.onChange}
            onSave={props.onSave}
            onEditing={props.onEditing}
            isEditing={isEditing}
            isInvalid={hasError}
            setting={settingDefinitionAndValue}
            value={effectiveValue}
          />

          <div className="sw-mt-2">
            {loading && (
              <div className="sw-flex">
                <Spinner />
                <Note className="sw-ml-2">{translate('settings.state.saving')}</Note>
              </div>
            )}

            {!loading && validationMessage && (
              <div>
                <TextError
                  text={translateWithParameters(
                    'settings.state.validation_failed',
                    validationMessage,
                  )}
                />
              </div>
            )}

            {!loading && !hasError && success && (
              <FlagMessage variant="success">{translate('settings.state.saved')}</FlagMessage>
            )}
          </div>

          <DefinitionActions
            changedValue={changedValue}
            hasError={hasError}
            hasValueChanged={hasValueChanged}
            isDefault={isDefault}
            isEditing={isEditing}
            onCancel={props.onCancel}
            onReset={props.onReset}
            onSave={props.onSave}
            setting={settingDefinitionAndValue}
          />
        </form>
      </div>
    </div>
  );
}
