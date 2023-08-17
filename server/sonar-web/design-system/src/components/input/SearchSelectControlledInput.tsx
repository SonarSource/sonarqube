/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import React, { PropsWithChildren } from 'react';
import { useIntl } from 'react-intl';
import { INPUT_SIZES } from '../../helpers/constants';
import { isDefined } from '../../helpers/types';
import { InputSizeKeys } from '../../types/theme';
import { Spinner } from '../Spinner';
import { CloseIcon } from '../icons/CloseIcon';
import {
  InputSearchWrapper,
  StyledInputWrapper,
  StyledInteractiveIcon,
  StyledNote,
  StyledSearchIcon,
} from './InputSearch';

interface Props {
  className?: string;
  id?: string;
  loading?: boolean;
  minLength?: number;
  onChange: (value: string) => void;
  onMouseDown?: React.MouseEventHandler<HTMLInputElement>;
  size?: InputSizeKeys;
  value: string;
}

export function SearchSelectControlledInput({
  id,
  className,
  onChange,
  onMouseDown,
  loading,
  minLength,
  size = 'medium',
  value,
  children,
}: PropsWithChildren<Props>) {
  const intl = useIntl();
  const tooShort = isDefined(minLength) && value.length > 0 && value.length < minLength;

  return (
    <InputSearchWrapper
      className={className}
      id={id}
      onMouseDown={onMouseDown}
      style={{ '--inputSize': INPUT_SIZES[size] }}
      title={
        tooShort && isDefined(minLength)
          ? intl.formatMessage({ id: 'select2.tooShort' }, { 0: minLength })
          : ''
      }
    >
      <StyledInputWrapper className="sw-flex sw-items-center">
        {children}
        <Spinner className="sw-z-normal" loading={loading ?? false}>
          <StyledSearchIcon />
        </Spinner>
        {value !== '' && (
          <StyledInteractiveIcon
            Icon={CloseIcon}
            aria-label={intl.formatMessage({ id: 'clear' })}
            className="it__search-box-clear"
            onClick={() => {
              onChange('');
            }}
            size="small"
          />
        )}

        {tooShort && isDefined(minLength) && (
          <StyledNote className="sw-ml-1" role="note">
            {intl.formatMessage({ id: 'select2.tooShort' }, { 0: minLength })}
          </StyledNote>
        )}
      </StyledInputWrapper>
    </InputSearchWrapper>
  );
}

SearchSelectControlledInput.displayName = 'SearchSelectControlledInput'; // so that tests don't see the obfuscated production name
