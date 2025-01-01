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

import { Spinner } from '@sonarsource/echoes-react';
import * as React from 'react';
import { FlagMessage, Note, TextError } from '~design-system';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { parseError } from '../../../helpers/request';
import {
  useGetValueQuery,
  useResetSettingsMutation,
  useSaveValueMutation,
} from '../../../queries/settings';
import { ExtendedSettingDefinition, SettingType, SettingValue } from '../../../types/settings';
import { Component } from '../../../types/types';
import {
  combineDefinitionAndSettingValue,
  getSettingValue,
  getUniqueName,
  isDefaultOrInherited,
  isEmptyValue,
  isURLKind,
} from '../utils';
import DefinitionActions from './DefinitionActions';
import DefinitionDescription from './DefinitionDescription';
import Input from './inputs/Input';

interface Props {
  component?: Component;
  definition: ExtendedSettingDefinition;
  initialSettingValue?: SettingValue;
}

const SAFE_SET_STATE_DELAY = 3000;
const formNoop = (e: React.FormEvent<HTMLFormElement>) => e.preventDefault();
type FieldValue = string | string[] | boolean;

export default function Definition(props: Readonly<Props>) {
  const { component, definition, initialSettingValue } = props;
  const timeout = React.useRef<number | undefined>();
  const [isEditing, setIsEditing] = React.useState(false);
  const [loading, setLoading] = React.useState(false);
  const [success, setSuccess] = React.useState(false);
  const [changedValue, setChangedValue] = React.useState<FieldValue>();
  const [validationMessage, setValidationMessage] = React.useState<string>();
  const ref = React.useRef<HTMLElement>(null);
  const name = getUniqueName(definition);

  const { data: loadedSettingValue, isLoading } = useGetValueQuery({
    key: definition.key,
    component: component?.key,
  });

  // WARNING: do *not* remove `?? undefined` below, it is required to change `null` to `undefined`!
  // (Yes, it's ugly, we really shouldn't use `null` as the fallback value in useGetValueQuery)
  // prettier-ignore
  const settingValue = isLoading ? initialSettingValue : (loadedSettingValue ?? undefined);

  const { mutateAsync: resetSettingValue } = useResetSettingsMutation();
  const { mutateAsync: saveSettingValue } = useSaveValueMutation();

  React.useEffect(() => () => clearTimeout(timeout.current), []);

  const handleChange = (changedValue: FieldValue) => {
    clearTimeout(timeout.current);

    setChangedValue(changedValue);
    setSuccess(false);
    handleCheck(changedValue);
  };

  const handleReset = async () => {
    setLoading(true);
    setSuccess(false);

    try {
      await resetSettingValue({ keys: [definition.key], component: component?.key });

      setChangedValue(undefined);
      setLoading(false);
      setSuccess(true);
      ref.current?.focus();
      setValidationMessage(undefined);

      timeout.current = window.setTimeout(() => {
        setSuccess(false);
      }, SAFE_SET_STATE_DELAY);
    } catch (e) {
      const validationMessage = await parseError(e as Response);
      setLoading(false);
      setValidationMessage(validationMessage);
      ref.current?.focus();
    }
  };

  const handleCancel = () => {
    setChangedValue(undefined);
    setValidationMessage(undefined);
    setIsEditing(false);
  };

  const handleCheck = (value?: FieldValue) => {
    if (isEmptyValue(definition, value)) {
      if (definition.defaultValue === undefined) {
        setValidationMessage(translate('settings.state.value_cant_be_empty_no_default'));
      } else {
        setValidationMessage(translate('settings.state.value_cant_be_empty'));
      }
      ref.current?.focus();

      return false;
    }

    if (isURLKind(definition)) {
      try {
        // eslint-disable-next-line no-new
        new URL(value?.toString() ?? '');
      } catch (e) {
        setValidationMessage(
          translateWithParameters('settings.state.url_not_valid', value?.toString() ?? ''),
        );
        ref.current?.focus();

        return false;
      }
    }

    if (definition.type === SettingType.JSON) {
      try {
        JSON.parse(value?.toString() ?? '');
      } catch (e) {
        setValidationMessage((e as Error).message);
        ref.current?.focus();

        return false;
      }
    }

    setValidationMessage(undefined);
    return true;
  };

  const handleSave = async () => {
    if (changedValue !== undefined) {
      setSuccess(false);

      if (isEmptyValue(definition, changedValue)) {
        setValidationMessage(translate('settings.state.value_cant_be_empty'));
        ref.current?.focus();

        return;
      }

      setLoading(true);

      try {
        await saveSettingValue({ definition, newValue: changedValue, component: component?.key });

        setChangedValue(undefined);
        setIsEditing(false);
        setLoading(false);
        setSuccess(true);
        ref.current?.focus();

        timeout.current = window.setTimeout(() => {
          setSuccess(false);
        }, SAFE_SET_STATE_DELAY);
      } catch (e) {
        const validationMessage = await parseError(e as Response);
        setLoading(false);
        setValidationMessage(validationMessage);
        ref.current?.focus();
      }
    }
  };

  const hasError = validationMessage != null;
  const hasValueChanged = changedValue != null;
  const effectiveValue = hasValueChanged ? changedValue : getSettingValue(definition, settingValue);
  const isDefault = isDefaultOrInherited(settingValue);

  const settingDefinitionAndValue = combineDefinitionAndSettingValue(definition, settingValue);

  return (
    <div data-key={definition.key} data-testid={definition.key} className="sw-flex sw-gap-12">
      <DefinitionDescription definition={definition} />
      <div className="sw-flex-1">
        <form onSubmit={formNoop}>
          <Input
            ariaDescribedBy={`definition-stats-${name}`}
            hasValueChanged={hasValueChanged}
            onCancel={handleCancel}
            onChange={handleChange}
            onSave={handleSave}
            onEditing={() => setIsEditing(true)}
            ref={ref}
            isEditing={isEditing}
            isInvalid={hasError}
            setting={settingDefinitionAndValue}
            value={effectiveValue}
          />

          <div className="sw-mt-2">
            {loading && (
              <div id={`definition-stats-${name}`} className="sw-flex">
                <Spinner aria-busy />

                <Note className="sw-ml-2">{translate('settings.state.saving')}</Note>
              </div>
            )}

            {!loading && validationMessage && (
              <div id={`definition-stats-${name}`}>
                <TextError
                  as="output"
                  className="sw-whitespace-break-spaces"
                  text={translateWithParameters(
                    'settings.state.validation_failed',
                    validationMessage,
                  )}
                />
              </div>
            )}

            {!loading && !hasError && success && (
              <FlagMessage id={`definition-stats-${name}`} variant="success">
                {translate('settings.state.saved')}
              </FlagMessage>
            )}
          </div>

          <DefinitionActions
            changedValue={changedValue}
            definition={definition}
            hasError={hasError}
            hasValueChanged={hasValueChanged}
            isDefault={isDefault}
            isEditing={isEditing}
            onCancel={handleCancel}
            onReset={handleReset}
            onSave={handleSave}
            setting={settingDefinitionAndValue}
          />
        </form>
      </div>
    </div>
  );
}
