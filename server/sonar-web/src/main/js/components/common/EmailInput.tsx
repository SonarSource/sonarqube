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
import { InputField, TextError } from 'design-system';
import * as React from 'react';
import isEmail from 'validator/lib/isEmail';
import { translate } from '../../helpers/l10n';
import FocusOutHandler from '../controls/FocusOutHandler';

export interface Props {
  id: string;
  onChange: (email: { isValid: boolean; value: string }) => void;
  value: string;
}

export default function EmailIput(props: Readonly<Props>) {
  const { id, value, onChange } = props;

  const [isEmailValid, setIsEmailValid] = React.useState(true);

  return (
    <FocusOutHandler onFocusOut={() => setIsEmailValid(isEmail(value))}>
      <InputField
        id={id}
        size="full"
        onChange={({ currentTarget }) => {
          onChange({ value: currentTarget.value, isValid: isEmail(currentTarget.value) });
        }}
        type="email"
        value={value}
      />
      {isEmailValid === false && (
        <TextError className="sw-mt-2" text={translate('user.email.invalid')} />
      )}
    </FocusOutHandler>
  );
}
