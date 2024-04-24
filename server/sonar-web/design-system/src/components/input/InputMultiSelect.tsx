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
import classNames from 'classnames';
import { themeBorder } from '../../helpers';
import { ButtonProps } from '../../sonar-aligned/components/buttons';
import { Badge } from '../Badge';
import { LightLabel } from '../Text';
import { WrapperButton } from '../buttons';
import { ChevronDownIcon } from '../icons';

interface Props extends Pick<ButtonProps, 'onClick'> {
  className?: string;
  count?: number;
  id?: string;
  placeholder: string;
  selectedLabel: string;
}

export function InputMultiSelect(props: Props) {
  const { className, count, id, placeholder, selectedLabel } = props;

  return (
    <StyledWrapper
      className={classNames('sw-flex sw-justify-between sw-px-2 sw-body-sm', className)}
      id={id}
      onClick={props.onClick}
      role="combobox"
    >
      {count ? selectedLabel : <LightLabel>{placeholder}</LightLabel>}

      <div>
        {count !== undefined && count > 0 && <Badge variant="counter">{count}</Badge>}
        <ChevronDownIcon className="sw-ml-2" />
      </div>
    </StyledWrapper>
  );
}

const StyledWrapper = styled(WrapperButton)`
  border: ${themeBorder('default', 'inputBorder')};

  &:hover {
    border: ${themeBorder('default', 'inputFocus')};
  }

  &:active,
  &:focus,
  &:focus-within,
  &:focus-visible {
    border: ${themeBorder('default', 'inputFocus')};
    outline: ${themeBorder('focus', 'inputFocus')};
  }
`;
