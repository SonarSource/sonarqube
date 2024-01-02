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
import { ReactNode, SyntheticEvent, useCallback } from 'react';
import tw, { theme as twTheme } from 'twin.macro';
import { themeBorder, themeColor, themeContrast } from '../../helpers/theme';

interface Props {
  active?: boolean;
  children: ReactNode;
  className?: string;
  innerRef?: (node: HTMLAnchorElement) => void;
  onClick: (value?: string) => void;
  value?: string;
}

export function SubnavigationItem(props: Readonly<Props>) {
  const { active, className, children, innerRef, onClick, value } = props;
  const handleClick = useCallback(
    (e: SyntheticEvent<HTMLAnchorElement>) => {
      e.preventDefault();
      onClick(value);
    },
    [onClick, value],
  );
  return (
    <StyledSubnavigationItem
      className={classNames({ active }, className)}
      data-testid="js-subnavigation-item"
      href="#"
      onClick={handleClick}
      ref={innerRef}
    >
      {children}
    </StyledSubnavigationItem>
  );
}

const StyledSubnavigationItem = styled.a`
  ${tw`sw-flex sw-items-center sw-justify-between`}
  ${tw`sw-box-border`}
  ${tw`sw-body-sm`}
  ${tw`sw-py-4 sw-pr-4`}
  ${tw`sw-w-full`}
  ${tw`sw-cursor-pointer`}

  padding-left: calc(${twTheme('spacing.4')} - 3px);
  color: ${themeContrast('subnavigation')};
  background-color: ${themeColor('subnavigation')};
  border-bottom: none;
  border-left: ${themeBorder('active', 'transparent')};
  transition: 0.2 ease;
  transition-property: border-left, background-color, color;

  &:hover,
  &:focus,
  &.active {
    background-color: ${themeColor('subnavigationHover')};
  }

  &.active {
    color: ${themeContrast('subnavigationHover')};
    border-left: ${themeBorder('active')};
  }
`;
