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
import React from 'react';
import tw from 'twin.macro';
import { themeBorder, themeColor, themeContrast } from '../../helpers/theme';
import { ThemedProps } from '../../types/theme';
import { BaseLink, LinkProps } from '../Link';

type AllowedButtonAttributes = Pick<
  React.ButtonHTMLAttributes<HTMLButtonElement>,
  'aria-label' | 'autoFocus' | 'id' | 'name' | 'role' | 'style' | 'title' | 'type' | 'form'
>;

export interface ButtonProps extends AllowedButtonAttributes {
  children?: React.ReactNode;
  className?: string;
  disabled?: boolean;
  download?: string;
  icon?: React.ReactNode;
  innerRef?: React.Ref<HTMLButtonElement>;
  isExternal?: LinkProps['isExternal'];
  onClick?: (event: React.MouseEvent<HTMLButtonElement | HTMLAnchorElement>) => unknown;

  preventDefault?: boolean;
  reloadDocument?: LinkProps['reloadDocument'];
  stopPropagation?: boolean;
  target?: LinkProps['target'];
  to?: LinkProps['to'];
}

export class Button extends React.PureComponent<ButtonProps> {
  handleClick = (event: React.MouseEvent<HTMLButtonElement | HTMLAnchorElement>) => {
    const { disabled, onClick, stopPropagation = false, type } = this.props;
    const { preventDefault = type !== 'submit' } = this.props;

    if (preventDefault || disabled) {
      event.preventDefault();
    }

    if (stopPropagation) {
      event.stopPropagation();
    }

    if (onClick && !disabled) {
      onClick(event);
    }
  };

  render() {
    const {
      children,
      disabled,
      icon,
      innerRef,
      onClick,
      preventDefault,
      stopPropagation,
      to,
      type = 'button',
      ...htmlProps
    } = this.props;

    const props = {
      ...htmlProps,
      'aria-disabled': disabled,
      disabled,
      type,
    };

    if (to) {
      return (
        <BaseButtonLink {...props} onClick={onClick} to={to}>
          {icon}
          {children}
        </BaseButtonLink>
      );
    }

    return (
      <BaseButton {...props} onClick={this.handleClick} ref={innerRef}>
        {icon}
        {children}
      </BaseButton>
    );
  }
}

export const buttonStyle = (props: ThemedProps) => css`
  box-sizing: border-box;
  text-decoration: none;
  outline: none;
  border: var(--border);
  color: var(--color);
  background-color: var(--background);
  transition:
    background-color 0.2s ease,
    outline 0.2s ease;

  ${tw`sw-inline-flex sw-items-center`}
  ${tw`sw-h-control`}
  ${tw`sw-body-sm-highlight`}
  ${tw`sw-py-2 sw-px-4`}
  ${tw`sw-rounded-2`}
  ${tw`sw-cursor-pointer`}

  &:hover, &:active {
    color: var(--color);
    background-color: var(--backgroundHover);
  }

  &:focus,
  &:active {
    color: var(--color);
    outline: ${themeBorder('focus', 'var(--focus)')(props)};
  }

  &:disabled,
  &:disabled:hover {
    color: ${themeContrast('buttonDisabled')(props)};
    background-color: ${themeColor('buttonDisabled')(props)};
    border: ${themeBorder('default', 'buttonDisabledBorder')(props)};

    ${tw`sw-cursor-not-allowed`}
  }

  & > svg {
    ${tw`sw-mr-1`}
  }
`;

const BaseButtonLink = styled(BaseLink)`
  ${buttonStyle}
`;

const BaseButton = styled.button`
  ${buttonStyle}

  /*
   Workaround for tooltips issue with onMouseLeave in disabled buttons: 
   https://github.com/facebook/react/issues/4251 
  */
  & [disabled] {
    ${tw`sw-pointer-events-none`};
  }
`;
