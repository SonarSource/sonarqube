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

import React from 'react';
import { NavLink as RouterNavLink, NavLinkProps as RouterNavLinkProps } from 'react-router-dom';

export interface NavLinkProps extends RouterNavLinkProps {
  blurAfterClick?: boolean;
  disabled?: boolean;
  onClick?: (event: React.MouseEvent<HTMLAnchorElement>) => void;
  preventDefault?: boolean;
  stopPropagation?: boolean;
}

// Styling this component directly with Emotion should be avoided due to conflicts with react-router's classname.
// Use NavBarTabs as an example of this exception.
function NavLinkWithRef(props: NavLinkProps, ref: React.ForwardedRef<HTMLAnchorElement>) {
  const {
    blurAfterClick,
    children,
    disabled,
    onClick,
    preventDefault,
    stopPropagation,
    ...otherProps
  } = props;

  const handleClick = React.useCallback(
    (event: React.MouseEvent<HTMLAnchorElement>) => {
      if (blurAfterClick) {
        // explicitly lose focus after click
        event.currentTarget.blur();
      }

      if (preventDefault || disabled) {
        event.preventDefault();
      }

      if (stopPropagation) {
        event.stopPropagation();
      }

      if (onClick && !disabled) {
        onClick(event);
      }
    },
    [onClick, blurAfterClick, preventDefault, stopPropagation, disabled],
  );

  return (
    <RouterNavLink onClick={handleClick} ref={ref} {...otherProps}>
      {children}
    </RouterNavLink>
  );
}

const NavLink = React.forwardRef(NavLinkWithRef);
export default NavLink;
