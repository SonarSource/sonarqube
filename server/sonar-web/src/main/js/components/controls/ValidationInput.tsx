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
import { translate } from '../../helpers/l10n';
import AlertErrorIcon from '../icons/AlertErrorIcon';
import AlertSuccessIcon from '../icons/AlertSuccessIcon';
import MandatoryFieldMarker from '../ui/MandatoryFieldMarker';
import HelpTooltip from './HelpTooltip';

export interface ValidationInputProps {
  description?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
  error?: string;
  errorPlacement?: ValidationInputErrorPlacement;
  help?: string;
  labelHtmlFor?: string;
  isInvalid: boolean;
  isValid: boolean;
  label?: React.ReactNode;
  required?: boolean;
  showValidIcon?: boolean;
}

export enum ValidationInputErrorPlacement {
  Right,
  Bottom,
}

export default function ValidationInput(props: ValidationInputProps) {
  const {
    children,
    className,
    description,
    error,
    errorPlacement = ValidationInputErrorPlacement.Right,
    help,
    labelHtmlFor,
    isInvalid,
    isValid,
    label,
    required,
    showValidIcon = true,
  } = props;

  const hasError = isInvalid && error !== undefined;

  let childrenWithStatus: React.ReactNode;

  if (errorPlacement === ValidationInputErrorPlacement.Right) {
    childrenWithStatus = (
      <>
        {children}
        {showValidIcon && isValid && (
          <AlertSuccessIcon label={translate('valid_input')} className="spacer-left text-middle" />
        )}
        {isInvalid && <AlertErrorIcon className="spacer-left text-middle" />}
        {hasError && <span className="little-spacer-left text-danger text-middle">{error}</span>}
      </>
    );
  } else {
    childrenWithStatus = (
      <>
        {children}
        {showValidIcon && isValid && (
          <AlertSuccessIcon label={translate('valid_input')} className="spacer-left text-middle" />
        )}
        <div className="spacer-top" style={{ display: 'flex' }}>
          {isInvalid && <AlertErrorIcon className="text-middle" />}
          {hasError && <span className="little-spacer-left text-danger text-middle">{error}</span>}
        </div>
      </>
    );
  }

  return (
    <div className={className}>
      {labelHtmlFor && label && (
        <label htmlFor={labelHtmlFor}>
          <span className="text-middle">
            <strong>{label}</strong>
            {required && <MandatoryFieldMarker />}
          </span>
          {help && <HelpTooltip className="spacer-left" overlay={help} />}
        </label>
      )}
      <div className="little-spacer-top spacer-bottom">{childrenWithStatus}</div>
      {description && <div className="note abs-width-400">{description}</div>}
    </div>
  );
}
