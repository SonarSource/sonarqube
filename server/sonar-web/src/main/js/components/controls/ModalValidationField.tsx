/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as classNames from 'classnames';
import AlertErrorIcon from '../icons-components/AlertErrorIcon';
import AlertSuccessIcon from '../icons-components/AlertSuccessIcon';

interface Props {
  children: (props: { className?: string }) => React.ReactNode;
  description?: string;
  dirty: boolean;
  error: string | undefined;
  label?: React.ReactNode;
  touched: boolean | undefined;
}

export default function ModalValidationField(props: Props) {
  const { description, dirty, error } = props;

  const isValid = dirty && props.touched && error === undefined;
  const showError = dirty && props.touched && error !== undefined;
  return (
    <div className="modal-validation-field">
      {props.label}
      {props.children({ className: classNames({ 'is-invalid': showError, 'is-valid': isValid }) })}
      {showError && <AlertErrorIcon className="little-spacer-top" />}
      {isValid && <AlertSuccessIcon className="little-spacer-top" />}
      {showError && <p className="text-danger">{error}</p>}
      {description && <div className="modal-field-description">{description}</div>}
    </div>
  );
}
