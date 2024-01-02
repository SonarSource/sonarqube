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
import classNames from 'classnames';
import * as React from 'react';
import ValidationInput from '../../components/controls/ValidationInput';
import { PROJECT_KEY_MAX_LEN } from '../../helpers/constants';
import { translate } from '../../helpers/l10n';

export interface ProjectKeyInputProps {
  error?: string;
  help?: string;
  label?: string;
  onProjectKeyChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  placeholder?: string;
  projectKey?: string;
  touched: boolean;
  validating?: boolean;
  autofocus?: boolean;
}

export default function ProjectKeyInput(props: ProjectKeyInputProps) {
  const {
    error,
    help,
    label,
    placeholder,
    projectKey,
    touched,
    validating,
    autofocus = false,
  } = props;

  const isInvalid = touched && error !== undefined;
  const isValid = touched && !validating && error === undefined;

  return (
    <ValidationInput
      className="form-field"
      description={translate('onboarding.create_project.project_key.description')}
      error={error}
      help={help}
      labelHtmlFor="project-key"
      isInvalid={isInvalid}
      isValid={isValid}
      label={label}
      required={label !== undefined}
    >
      <input
        autoFocus={autofocus}
        className={classNames('input-super-large', {
          'is-invalid': isInvalid,
          'is-valid': isValid,
        })}
        id="project-key"
        maxLength={PROJECT_KEY_MAX_LEN}
        minLength={1}
        onChange={props.onProjectKeyChange}
        placeholder={placeholder}
        type="text"
        value={projectKey}
      />
    </ValidationInput>
  );
}
