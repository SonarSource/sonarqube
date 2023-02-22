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
import { css } from '@emotion/react';
import styled from '@emotion/styled';
import React, { HTMLAttributeAnchorTarget } from 'react';
import { Link as RouterLink, LinkProps as RouterLinkProps } from 'react-router-dom';
import tw, { theme as twTheme } from 'twin.macro';
import { themeBorder, themeColor } from '../helpers/theme';
import OpenNewTabIcon from './icons/OpenNewTabIcon';
import { TooltipWrapperInner } from './Tooltip';

export interface LinkProps extends RouterLinkProps {
  blurAfterClick?: boolean;
  disabled?: boolean;
  forceExternal?: boolean;
  icon?: React.ReactNode;
  onClick?: (event: React.MouseEvent<HTMLAnchorElement>) => void;
  preventDefault?: boolean;
  showExternalIcon?: boolean;
  stopPropagation?: boolean;
  target?: HTMLAttributeAnchorTarget;
}

function BaseLinkWithRef(props: LinkProps, ref: React.ForwardedRef<HTMLAnchorElement>) {
  const {
    children,
    blurAfterClick,
    disabled,
    icon,
    onClick,
    preventDefault,
    showExternalIcon = !icon,
    stopPropagation,
    target = '_blank',
    to,
    ...rest
  } = props;
  const isExternal = typeof to === 'string' && to.startsWith('http');
  const handleClick = React.useCallback(
    (event: React.MouseEvent<HTMLAnchorElement>) => {
      if (blurAfterClick) {
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
    [onClick, blurAfterClick, preventDefault, stopPropagation, disabled]
  );

  return isExternal ? (
    <a
      {...rest}
      href={to}
      onClick={handleClick}
      ref={ref}
      rel="noopener noreferrer"
      target={target}
    >
      {icon}
      {children}
      {showExternalIcon && <OpenNewTabIcon className="sw-ml-1" />}
    </a>
  ) : (
    <RouterLink ref={ref} {...rest} onClick={handleClick} to={to}>
      {icon}
      {children}
    </RouterLink>
  );
}

export const BaseLink = React.forwardRef(BaseLinkWithRef);

const StyledBaseLink = styled(BaseLink)`
  color: var(--color);
  border-bottom: ${({ children, icon, theme }) =>
    icon && !children ? themeBorder('default', 'transparent')({ theme }) : 'var(--border)'};

  &:visited {
    color: var(--color);
  }

  &:hover,
  &:focus,
  &:active {
    color: var(--active);
    border-bottom: ${({ children, icon, theme }) =>
      icon && !children ? themeBorder('default', 'transparent')({ theme }) : 'var(--borderActive)'};
  }

  & > svg {
    ${tw`sw-align-text-bottom!`}
  }

  ${({ icon }) =>
    icon &&
    css`
      margin-left: calc(${twTheme('width.icon')} + ${twTheme('spacing.1')});

      & > svg,
      & > img {
        ${tw`sw-mr-1`}

        margin-left: calc(-1 * (${twTheme('width.icon')} + ${twTheme('spacing.1')}));
      }
    `};
`;

export const HoverLink = styled(StyledBaseLink)`
  text-decoration: none;

  --color: ${themeColor('linkDiscreet')};
  --active: ${themeColor('linkActive')};
  --border: ${themeBorder('default', 'transparent')};
  --borderActive: ${themeBorder('default', 'linkActive')};

  ${TooltipWrapperInner} & {
    --active: ${themeColor('linkTooltipActive')};
    --borderActive: ${themeBorder('default', 'linkTooltipActive')};
  }
`;
HoverLink.displayName = 'HoverLink';

export const DiscreetLink = styled(HoverLink)`
  --border: ${themeBorder('default', 'linkDiscreet')};
`;
DiscreetLink.displayName = 'DiscreetLink';

const StandoutLink = styled(StyledBaseLink)`
  ${tw`sw-font-semibold`}
  ${tw`sw-no-underline`}

  --color: ${themeColor('linkDefault')};
  --active: ${themeColor('linkActive')};
  --border: ${themeBorder('default', 'linkDefault')};
  --borderActive: ${themeBorder('default', 'linkDefault')};

  ${TooltipWrapperInner} & {
    --color: ${themeColor('linkTooltipDefault')};
    --active: ${themeColor('linkTooltipActive')};
    --border: ${themeBorder('default', 'linkTooltipDefault')};
    --borderActive: ${themeBorder('default', 'linkTooltipActive')};
  }
`;
StandoutLink.displayName = 'StandoutLink';

export default StandoutLink;
