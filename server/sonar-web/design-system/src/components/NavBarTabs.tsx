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
import React from 'react';
import tw, { theme } from 'twin.macro';
import { themeBorder, themeColor, themeContrast } from '../helpers/theme';
import { isDefined } from '../helpers/types';
import NavLink, { NavLinkProps } from './NavLink';
import { Tooltip } from './Tooltip';
import { ChevronDownIcon } from './icons/ChevronDownIcon';

interface Props extends React.HTMLAttributes<HTMLUListElement> {
  children?: React.ReactNode;
  className?: string;
}

export function NavBarTabs({ children, className, ...other }: Props) {
  return (
    <ul className={`sw-flex sw-items-end sw-gap-8 ${className ?? ''}`} {...other}>
      {children}
    </ul>
  );
}

interface NavBarTabLinkProps extends Omit<NavLinkProps, 'children'> {
  active?: boolean;
  children?: React.ReactNode;
  className?: string;
  text: string;
  withChevron?: boolean;
}

export function NavBarTabLink(props: NavBarTabLinkProps) {
  const { active, children, className, text, withChevron = false, ...linkProps } = props;
  return (
    <NavBarTabLinkWrapper>
      <NavLink
        className={({ isActive }) =>
          classNames(
            'sw-flex sw-items-center',
            { active: isDefined(active) ? active : isActive },
            className,
          )
        }
        {...linkProps}
      >
        <span className="sw-inline-block sw-text-center" data-text={text}>
          {text}
        </span>
        {children}
        {withChevron && <ChevronDownIcon className="sw-ml-1" />}
      </NavLink>
    </NavBarTabLinkWrapper>
  );
}

export function DisabledTabLink(props: { label: string; overlay: React.ReactNode }) {
  return (
    <NavBarTabLinkWrapper>
      <Tooltip overlay={props.overlay}>
        <a aria-disabled="true" className="disabled-link" role="link">
          {props.label}
        </a>
      </Tooltip>
    </NavBarTabLinkWrapper>
  );
}

// Styling for <NavLink> due to its special className function, it conflicts when styled with Emotion.
const NavBarTabLinkWrapper = styled.li`
  ${tw`sw-body-md`};
  & > a {
    ${tw`sw-pb-3`};
    ${tw`sw-block`};
    ${tw`sw-box-border`};
    ${tw`sw-transition-none`};

    color: ${themeContrast('buttonSecondary')};
    text-decoration: none;
    border-bottom: ${themeBorder('xsActive', 'transparent')};
    padding-bottom: calc(${theme('spacing.3')} + 1px); // 12px spacing + 3px border + 1px = 16px
  }

  & > a.active,
  & > a:active,
  & > a:hover,
  & > a:focus {
    border-bottom-color: ${themeColor('tabBorder')};
  }

  & > a.active > span[data-text],
  & > a:active > span {
    ${tw`sw-body-md-highlight`};
  }

  // This is a hack to have a link take the space of the bold font, so when active other ones are not moving
  & > a > span[data-text]::before {
    ${tw`sw-block`};
    ${tw`sw-body-md-highlight`};
    ${tw`sw-h-0`};
    ${tw`sw-overflow-hidden`};
    ${tw`sw-invisible`};
    content: attr(data-text);
  }

  & > a.disabled-link,
  & > a.disabled-link:hover,
  & > a.disabled-link.hover {
    ${tw`sw-cursor-default`};
    border-bottom: ${themeBorder('xsActive', 'transparent', 1)};
    color: ${themeContrast('subnavigationDisabled')};
  }
`;
