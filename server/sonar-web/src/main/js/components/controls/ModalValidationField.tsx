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
import styled from '@emotion/styled';
import { FlagErrorIcon, FlagSuccessIcon, FormField, Note, themeColor } from 'design-system';
import * as React from 'react';
import { translate } from '../../helpers/l10n';

interface Props {
  children: (props: { isInvalid?: boolean; isValid?: boolean }) => React.ReactNode;
  description?: string;
  dirty: boolean;
  error: string | undefined;
  id?: string;
  label?: React.ReactNode;
  required?: boolean;
  touched: boolean | undefined;
}

export default function ModalValidationField(props: Props) {
  const { description, dirty, error, label, id, required } = props;

  const isValid = dirty && props.touched && error === undefined;
  const showError = dirty && props.touched && error !== undefined;
  return (
    <FormField
      label={label}
      htmlFor={id}
      required={required}
      requiredAriaLabel={translate('field_required')}
    >
      <div className="sw-flex sw-items-center sw-justify-between">
        {props.children({ isInvalid: showError, isValid })}
        {showError && <FlagErrorIcon className="sw-ml-2" />}
        {isValid && <FlagSuccessIcon className="sw-ml-2" />}
      </div>

      {showError && <StyledNote className="sw-mt-2">{error}</StyledNote>}
      {description && <Note className="sw-mt-2">{description}</Note>}
    </FormField>
  );
}

const StyledNote = styled(Note)`
  color: ${themeColor('errorText')};
`;
