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
import HelpTooltip from './HelpTooltip';
import AlertErrorIcon from '../icons-components/AlertErrorIcon';
import AlertSuccessIcon from '../icons-components/AlertSuccessIcon';

interface Props {
  description?: string;
  children: React.ReactNode;
  className?: string;
  error: string | undefined;
  help?: string;
  id: string;
  isInvalid: boolean;
  isValid: boolean;
  label: React.ReactNode;
  required?: boolean;
}

export default function ValidationInput(props: Props) {
  const hasError = props.isInvalid && props.error !== undefined;
  return (
    <div className={props.className}>
      <label htmlFor={props.id}>
        <span className="text-middle">
          <strong>{props.label}</strong>
          {props.required && <em className="mandatory">*</em>}
        </span>
        {props.help && <HelpTooltip className="spacer-left" overlay={props.help} />}
      </label>
      <div className="little-spacer-top spacer-bottom">
        {props.children}
        {props.isInvalid && <AlertErrorIcon className="spacer-left text-middle" />}
        {hasError && (
          <span className="little-spacer-left text-danger text-middle">{props.error}</span>
        )}
        {props.isValid && <AlertSuccessIcon className="spacer-left text-middle" />}
      </div>
      {props.description && <div className="note abs-width-400">{props.description}</div>}
    </div>
  );
}
