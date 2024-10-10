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
import { IconCheckCircle, IconError, Text } from '@sonarsource/echoes-react';
import { InputField } from 'design-system';
import * as React from 'react';
import isEmail from 'validator/lib/isEmail';
import { translate } from '../../helpers/l10n';
import FocusOutHandler from '../controls/FocusOutHandler';

export interface Props {
  id: string;
  isDisabled?: boolean;
  isMandotory?: boolean;
  onChange: (email: { isValid: boolean; value: string }) => void;
  value: string;
}

export type EmailChangeHandlerParams = { isValid: boolean; value: string };

export default function EmailIput(props: Readonly<Props>) {
  const { id, value, onChange, isDisabled, isMandotory = false } = props;

  const [isEmailValid, setIsEmailValid] = React.useState<boolean>();

  React.useEffect(() => {
    if (!isMandotory) {
      onChange({ value, isValid: true });
    }
  }, []);

  return (
    <FocusOutHandler onFocusOut={() => value !== '' && setIsEmailValid(isEmail(value))}>
      <div className="sw-flex sw-items-center">
        <InputField
          id={id}
          isInvalid={isEmailValid === false}
          isValid={isEmailValid === true}
          size="full"
          onChange={({ currentTarget }) => {
            const isValid = isMandotory
              ? isEmail(currentTarget.value)
              : currentTarget.value === '' || isEmail(currentTarget.value);
            onChange({ value: currentTarget.value, isValid });
            if (!isMandotory && currentTarget.value === '') {
              setIsEmailValid(undefined);
            } else if (isValid) {
              setIsEmailValid(true);
            }
          }}
          value={value}
          disabled={isDisabled === true}
        />
        {isEmailValid === false && (
          <IconError className="sw-ml-2" color="echoes-color-icon-danger" />
        )}
        {isEmailValid === true && (
          <IconCheckCircle color="echoes-color-icon-success" className="sw-ml-2" />
        )}
      </div>
      {isEmailValid === false && (
        <Text className="sw-mt-2" colorOverride="echoes-color-text-danger">
          {translate('users.email.invalid')}
        </Text>
      )}
    </FocusOutHandler>
  );
}
