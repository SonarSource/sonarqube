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
import { css } from '@emotion/react';
import styled from '@emotion/styled';
import classNames from 'classnames';
import { ReactNode, SyntheticEvent, useCallback } from 'react';
import tw, { theme as twTheme } from 'twin.macro';
import { themeBorder, themeColor, themeContrast } from '../../helpers/theme';
import { ThemedProps } from '../../types';
import NavLink, { NavLinkProps } from '../NavLink';

interface Props {
  active?: boolean;
  ariaCurrent?: boolean;
  children: ReactNode;
  className?: string;
  id?: string;
  innerRef?: (node: HTMLAnchorElement) => void;
  onClick: (value?: string) => void;
  value?: string;
}

export function SubnavigationItem(props: Readonly<Props>) {
  const { active, ariaCurrent, className, children, id, innerRef, onClick, value } = props;
  const handleClick = useCallback(
    (e: SyntheticEvent<HTMLAnchorElement>) => {
      e.preventDefault();
      onClick(value);
    },
    [onClick, value],
  );
  return (
    <StyledSubnavigationItem
      aria-current={ariaCurrent}
      className={classNames({ active }, className)}
      data-testid="js-subnavigation-item"
      href="#"
      id={id}
      onClick={handleClick}
      ref={innerRef}
    >
      {children}
    </StyledSubnavigationItem>
  );
}

export function SubnavigationLinkItem({ children, ...props }: NavLinkProps) {
  return <SubnavigationLinkItemStyled {...props}>{children}</SubnavigationLinkItemStyled>;
}

const ItemBaseStyle = (props: ThemedProps) => css`
  ${tw`sw-flex sw-items-center sw-justify-between`}
  ${tw`sw-box-border`}
  ${tw`sw-typo-default`}
  ${tw`sw-py-4 sw-pr-4`}
  ${tw`sw-w-full`}
  ${tw`sw-cursor-pointer`}

  padding-left: calc(${twTheme('spacing.4')} - 3px);
  color: ${themeContrast('subnavigation')(props)};
  background-color: ${themeColor('subnavigation')(props)};
  border-bottom: none;
  border-left: ${themeBorder('active', 'transparent')(props)};
  transition: 0.2 ease;
  transition-property: border-left, background-color, color;

  &:hover,
  &:focus,
  &.active {
    background-color: ${themeColor('subnavigationHover')(props)};
  }

  &.active {
    color: ${themeContrast('subnavigationHover')(props)};
    border-left: ${themeBorder('active')(props)};
  }
`;

const StyledSubnavigationItem = styled.a`
  ${ItemBaseStyle};
`;

const SubnavigationLinkItemStyled = styled(NavLink)`
  ${ItemBaseStyle};
  ${tw`sw-no-underline`}
`;
