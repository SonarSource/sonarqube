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
import styled from '@emotion/styled';
import classNames from 'classnames';
import { forwardRef, LegacyRef } from 'react';
import tw from 'twin.macro';
import { themeColor, themeContrast } from '../helpers/theme';
import { isDefined } from '../helpers/types';
import { IssueLocationIcon } from './icons/IssueLocationIcon';

interface Props {
  className?: string;
  onClick?: () => void;
  selected: boolean;
  text?: number | string;
}

function InternalLocationMarker(
  { className, onClick, text, selected }: Props,
  ref: LegacyRef<HTMLDivElement>,
) {
  return (
    <StyledMarker
      className={classNames(className, {
        selected,
        concealed: !isDefined(text),
        'sw-cursor-pointer': isDefined(onClick),
      })}
      onClick={onClick}
      ref={ref}
    >
      {isDefined(text) ? text : <IssueLocationIcon />}
    </StyledMarker>
  );
}

export const LocationMarker = forwardRef<HTMLDivElement, Props>(InternalLocationMarker);

export const StyledMarker = styled.div`
  ${tw`sw-flex sw-grow-0 sw-items-center sw-justify-center`}
  ${tw`sw-body-sm-highlight`}
  ${tw`sw-rounded-1/2`}

  height: 1.125rem;
  color: ${themeContrast('codeLineLocationMarker')};
  background-color: ${themeColor('codeLineLocationMarker')};

  &.selected,
  &:hover {
    background-color: ${themeColor('codeLineLocationMarkerSelected')};
  }

  &:not(.concealed) {
    ${tw`sw-px-1`}
    ${tw`sw-self-start`}
  }
`;
