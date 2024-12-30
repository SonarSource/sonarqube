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

import {
  ButtonIcon,
  ButtonVariety,
  IconDelete,
  IconEdit,
  InputSize,
  Select,
} from '@sonarsource/echoes-react';
import { isEmpty, isUndefined } from 'lodash';
import React, { useEffect } from 'react';
import isEmail from 'validator/lib/isEmail';
import { FormField, InputField, TextError } from '~design-system';
import { translate, translateWithParameters } from '../../../../helpers/l10n';

type InputType = 'email' | 'number' | 'password' | 'select' | 'text';

interface Props {
  children?: (props: { onChange: (value: string) => void }) => React.ReactNode;
  description: React.ReactNode;
  hasValue?: boolean;
  id: string;
  name: string;
  onChange: (value: string | undefined) => void;
  options?: string[];
  required?: boolean;
  requiresRevaluation?: boolean;
  type?: InputType;
  value: string | undefined;
}

export function EmailNotificationFormField(props: Readonly<Props>) {
  const {
    description,
    hasValue,
    id,
    name,
    options,
    required,
    requiresRevaluation,
    type = 'text',
    value,
  } = props;

  const [validationMessage, setValidationMessage] = React.useState<string>();

  const handleCheck = (changedValue: string | undefined) => {
    if (changedValue !== undefined && isEmpty(changedValue) && required) {
      setValidationMessage(translate('settings.state.value_cant_be_empty_no_default'));
      return false;
    }

    if (type === 'email' && !isEmail(changedValue ?? '')) {
      setValidationMessage(translate('email_notification.state.value_should_be_valid_email'));
      return false;
    }

    setValidationMessage(undefined);
    return true;
  };

  const onChange = (newValue: string | undefined) => {
    handleCheck(newValue);
    props.onChange(newValue);
  };

  const hasValidationMessage = !isUndefined(validationMessage);

  return (
    <FormField
      className="sw-grid sw-grid-cols-2 sw-gap-x-4 sw-py-6 sw-px-4"
      htmlFor={id}
      label={translate(name)}
      required={required}
      requiredAriaLabel={translate('field_required')}
    >
      <div className="sw-row-span-2 sw-grid">
        <EmailInput
          hasValue={hasValue}
          id={id}
          name={name}
          options={options ?? []}
          onChange={onChange}
          required={required}
          requiresRevaluation={requiresRevaluation}
          type={type}
          value={value}
        />

        {hasValidationMessage && (
          <TextError
            className="sw-mt-2"
            text={translateWithParameters('settings.state.validation_failed', validationMessage)}
          />
        )}
      </div>

      <div className="sw-w-abs-300">
        {!isUndefined(description) && <div className="markdown sw-mt-1">{description}</div>}
      </div>
    </FormField>
  );
}

function EmailInput(
  props: Readonly<{
    hasValue: boolean | undefined;
    id: string;
    name: string;
    onChange: (value: string | undefined) => void;
    options: string[];
    required?: boolean;
    requiresRevaluation?: boolean;
    type: InputType;
    value: string | undefined;
  }>,
) {
  const { type } = props;
  switch (type) {
    case 'password':
      return <PasswordInput {...props} />;
    case 'select':
      return <SelectInput {...props} />;
    default:
      return <BasicInput {...props} />;
  }
}

function BasicInput(
  props: Readonly<{
    hasValue: boolean | undefined;
    id: string;
    name: string;
    onChange: (value: string) => void;
    required?: boolean;
    type: InputType;
    value: string | undefined;
  }>,
) {
  const { hasValue, id, onChange, name, required, type, value } = props;

  return (
    <InputField
      id={id}
      min={type === 'number' ? 0 : undefined}
      name={name}
      onChange={(event) => onChange(event.target.value)}
      placeholder={
        type === 'password' && hasValue ? translate('email_notification.form.private') : undefined
      }
      required={required}
      size="large"
      step={type === 'number' ? 1 : undefined}
      type={type}
      value={value ?? ''}
    />
  );
}

function PasswordInput(
  props: Readonly<{
    hasValue: boolean | undefined;
    id: string;
    name: string;
    onChange: (value: string | undefined) => void;
    required?: boolean;
    requiresRevaluation?: boolean;
    value: string | undefined;
  }>,
) {
  const { hasValue, id, onChange, name, required, requiresRevaluation, value } = props;
  const [isEditing, setIsEditing] = React.useState<boolean>(requiresRevaluation === true);

  useEffect(() => {
    if (!requiresRevaluation) {
      setIsEditing(!hasValue);
    }
  }, [hasValue, requiresRevaluation]);

  return (
    <div className="sw-flex">
      <InputField
        disabled={!isEditing && !requiresRevaluation}
        id={id}
        name={name}
        onChange={(event) => onChange(event.target.value)}
        required={isEditing && required}
        size="large"
        type="password"
        value={
          hasValue && !isEditing && !requiresRevaluation
            ? translate('email_notification.form.private')
            : (value ?? '')
        }
      />
      {!requiresRevaluation && (
        <ButtonIcon
          ariaLabel={isEditing ? translate('reset_verb') : translate('edit')}
          data-testid={`${name}-${isEditing ? 'reset' : 'edit'}`}
          className="sw-ml-2"
          Icon={isEditing ? IconDelete : IconEdit}
          onClick={() => {
            if (isEditing) {
              onChange(undefined);
            }
            setIsEditing(!isEditing);
          }}
          variety={ButtonVariety.Default}
        />
      )}
    </div>
  );
}

function SelectInput(
  props: Readonly<{
    id: string;
    name: string;
    onChange: (value: string) => void;
    options: string[];
    required?: boolean;
    value: string | undefined;
  }>,
) {
  const { id, name, onChange, options, required, value } = props;

  return (
    <Select
      data={options?.map((option) => ({ label: option, value: option })) ?? []}
      id={id}
      isNotClearable
      isRequired={required}
      name={name}
      onChange={onChange}
      size={InputSize.Large}
      value={value}
    />
  );
}
