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
import { IconCheck, IconCheckCircle, IconError, IconX, Text } from '@sonarsource/echoes-react';
import { FormField, InputField, InputSizeKeys } from 'design-system';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import FocusOutHandler from '../controls/FocusOutHandler';

const MIN_PASSWORD_LENGTH = 12;

export type PasswordChangeHandlerParams = { isValid: boolean; value: string };

export interface Props {
  onChange: (password: PasswordChangeHandlerParams) => void;
  size?: InputSizeKeys;
  value: string;
}

export default function UserPasswordInput(props: Readonly<Props>) {
  const { onChange, size = 'full', value } = props;

  const [isFocused, setIsFocused] = React.useState(false);
  const [confirmValue, setConfirmValue] = React.useState('');

  const isInvalid = !isFocused && value !== '' && !isPasswordValid(value);
  const isValid = !isFocused && isPasswordValid(value);
  const passwordMatch = isPasswordConfirmed(value, confirmValue);
  const passwordDontMatch = value !== confirmValue && confirmValue !== '';

  React.useEffect(() => {
    if (value === '') {
      setConfirmValue('');
    }
  }, [value]);

  return (
    <>
      <FocusOutHandler className="sw-flex sw-items-center" onFocusOut={() => setIsFocused(false)}>
        <FormField required label={translate('password')} htmlFor="create-password">
          <div className="sw-flex sw-items-center">
            <InputField
              isInvalid={isInvalid}
              isValid={isValid}
              onFocus={() => setIsFocused(true)}
              id="create-password"
              size={size}
              onChange={({ currentTarget }) => {
                onChange({
                  value: currentTarget.value,
                  isValid:
                    isPasswordValid(currentTarget.value) &&
                    isPasswordConfirmed(currentTarget.value, confirmValue),
                });
              }}
              type="password"
              value={value}
            />
            {isInvalid && <IconError className="sw-ml-2" color="echoes-color-icon-danger" />}
            {isValid && <IconCheckCircle color="echoes-color-icon-success" className="sw-ml-2" />}
          </div>
          {isInvalid && (
            <Text colorOverride="echoes-color-text-danger" className="sw-mt-2">
              {translate('user.password.invalid')}
            </Text>
          )}
          {isFocused && <PasswordConstraint value={value} />}
        </FormField>
      </FocusOutHandler>

      <FormField
        className="sw-mt-4"
        required
        label={translate('confirm_password')}
        htmlFor="confirm-password"
      >
        <div className="sw-flex sw-items-center">
          <InputField
            isInvalid={passwordDontMatch}
            isValid={passwordMatch}
            onFocus={() => setIsFocused(true)}
            id="confirm-password"
            size={size}
            onChange={({ currentTarget }) => {
              setConfirmValue(currentTarget.value);
              onChange({
                value,
                isValid:
                  isPasswordValid(currentTarget.value) &&
                  isPasswordConfirmed(value, currentTarget.value),
              });
            }}
            type="password"
            value={confirmValue}
          />
          {passwordDontMatch && <IconError className="sw-ml-2" color="echoes-color-icon-danger" />}
          {passwordMatch && (
            <IconCheckCircle color="echoes-color-icon-success" className="sw-ml-2" />
          )}
        </div>
        {passwordDontMatch && (
          <Text colorOverride="echoes-color-text-danger" className="sw-mt-2">
            {translate('user.password.do_not_match')}
          </Text>
        )}
      </FormField>
    </>
  );
}

function PasswordConstraint({ value }: Readonly<{ value: string }>) {
  return (
    <div className="sw-mt-2">
      <Text isSubdued>{translate('user.password.conditions')}</Text>
      <ul className="sw-list-none sw-p-0 sw-mt-1">
        <Condition
          condition={contains12Characters(value)}
          label={translate('user.password.condition.12_characters')}
        />
        <Condition
          condition={containsUppercase(value)}
          label={translate('user.password.condition.1_upper_case')}
        />
        <Condition
          condition={containsLowercase(value)}
          label={translate('user.password.condition.1_lower_case')}
        />
        <Condition
          condition={containsDigit(value)}
          label={translate('user.password.condition.1_number')}
        />
        <Condition
          condition={containsSpecialCharacter(value)}
          label={translate('user.password.condition.1_special_character')}
        />
      </ul>
    </div>
  );
}

function Condition({ condition, label }: Readonly<{ condition: boolean; label: string }>) {
  return (
    <li className="sw-mb-1">
      {condition ? (
        <Text colorOverride="echoes-color-text-success" data-testid="valid-condition">
          <IconCheck className="sw-mr-1" />
          {label}
        </Text>
      ) : (
        <Text isSubdued data-testid="failed-condition">
          <IconX className="sw-mr-1" />
          {label}
        </Text>
      )}
    </li>
  );
}

const contains12Characters = (password: string) => password.length >= MIN_PASSWORD_LENGTH;
const containsUppercase = (password: string) => /[A-Z]/.test(password);
const containsLowercase = (password: string) => /[a-z]/.test(password);
const containsDigit = (password: string) => /\d/.test(password);
const containsSpecialCharacter = (password: string) => /[^a-zA-Z0-9]/.test(password);

const isPasswordValid = (password: string) =>
  contains12Characters(password) &&
  containsUppercase(password) &&
  containsLowercase(password) &&
  containsDigit(password) &&
  containsSpecialCharacter(password);

const isPasswordConfirmed = (password: string, confirm: string) =>
  password === confirm && password !== '';
