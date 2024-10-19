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
import { InputField } from 'design-system';
import * as React from 'react';
import ModalValidationField from './ModalValidationField';

interface Props {
  autoFocus?: boolean;
  className?: string;
  description?: string;
  dirty: boolean;
  disabled: boolean;
  error: string | undefined;
  id?: string;
  label?: React.ReactNode;
  name: string;
  onBlur: (event: React.FocusEvent<HTMLInputElement>) => void;
  onChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  placeholder?: string;
  required?: boolean;
  touched: boolean | undefined;
  type?: string;
  value: string;
}

export default function InputValidationField({ ...props }: Readonly<Props>) {
  const { description, dirty, error, label, touched, required, ...inputProps } = props;
  const modalValidationProps = { description, dirty, error, label, touched, required };
  return (
    <ModalValidationField id={props.id} {...modalValidationProps}>
      {({ isInvalid, isValid }) => (
        <InputField size="full" isInvalid={isInvalid} isValid={isValid} {...inputProps} />
      )}
    </ModalValidationField>
  );
}
