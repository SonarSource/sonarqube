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
import React, { MouseEvent, ReactNode, forwardRef, useCallback } from 'react';
import tw from 'twin.macro';
import { BaseLink, LinkProps } from '../../../components/Link';
import { themeBorder, themeColor, themeContrast } from '../../../helpers/theme';
import { ThemedProps } from '../../../types/theme';

type AllowedButtonAttributes = Pick<
  React.ButtonHTMLAttributes<HTMLButtonElement>,
  'aria-label' | 'autoFocus' | 'id' | 'name' | 'role' | 'style' | 'title' | 'type' | 'form'
>;

export interface ButtonProps extends AllowedButtonAttributes {
  children?: ReactNode;
  className?: string;
  disabled?: boolean;
  download?: string;
  icon?: ReactNode;
  isExternal?: LinkProps['isExternal'];
  onClick?: (event: MouseEvent<HTMLButtonElement | HTMLAnchorElement>) => unknown;

  preventDefault?: boolean;
  reloadDocument?: LinkProps['reloadDocument'];
  showExternalIcon?: boolean;
  stopPropagation?: boolean;
  target?: LinkProps['target'];
  to?: LinkProps['to'];
}

/**
 * @deprecated Use Button from Echoes instead.
 * Use the `variety` prop with the ButtonVariety enum to change the button's look and feel.
 *
 * Some of the props have changed or been renamed:
 * - `blurAfterClick` is now `shouldBlurAfterClick`
 * - `disabled` is now `isDisabled`, note that a Echoes Tooltip won't work
 * on a disabled button, use a text notice or ToggleTip next to the disabled button instead.
 * - `icon` is now replace by `prefix` which works the same way
 * - `preventDefault` is now `shouldPreventDefault`
 * - `stopPropagation` is now `shouldStopPropagation`
 *
 * The button can't be used as a link anymore, and all props related to links have been dropped.
 * Use a real Echoes Link instead.
 *
 * See the {@link https://xtranet-sonarsource.atlassian.net/wiki/spaces/Platform/pages/3382706231/Button | Migration Guide} for more information.
 */
export const Button = forwardRef<HTMLButtonElement, ButtonProps>((props, ref) => {
  const {
    children,
    disabled,
    icon,
    onClick,
    preventDefault = props.type !== 'submit',
    stopPropagation = false,
    to,
    type = 'button',
    ...htmlProps
  } = props;

  const handleClick = useCallback(
    (event: MouseEvent<HTMLButtonElement | HTMLAnchorElement>) => {
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
    [disabled, onClick, preventDefault, stopPropagation],
  );

  const buttonProps = {
    ...htmlProps,
    'aria-disabled': disabled,
    disabled,
    type,
  };

  if (to) {
    return (
      <BaseButtonLink {...buttonProps} onClick={onClick} to={to}>
        {icon}
        {children}
      </BaseButtonLink>
    );
  }

  return (
    <BaseButton {...buttonProps} onClick={handleClick} ref={ref}>
      {icon}
      {children}
    </BaseButton>
  );
});
Button.displayName = 'Button';

export const buttonStyle = (props: ThemedProps) => css`
  box-sizing: border-box;
  text-decoration: none;
  outline: none;
  border: var(--border);
  color: var(--color);
  background-color: var(--background);
  transition:
    background-color 0.2s ease,

  ${tw`sw-inline-flex sw-items-center`}
  ${tw`sw-h-control`}
  ${tw`sw-typo-semibold`}
  ${tw`sw-py-2 sw-px-4`}
  ${tw`sw-rounded-2`}
  ${tw`sw-cursor-pointer`}

  &:hover, &:active {
    color: var(--color);
    background-color: var(--backgroundHover);
  }

  &:focus,
  &:active,
  &:focus-visible {
    color: var(--color);
  }

  &:focus-visible {
    outline: var(--echoes-focus-border-width-default) solid var(--echoes-color-focus-default);
    outline-offset: var(--echoes-focus-border-offset-default);
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

  /*
    Workaround to apply disable style to button-link
    as link does not have disabled attribute, using props instead
  */

  ${({ disabled, theme }) =>
    disabled
      ? `&, &:hover, &:focus, &:active {
        color: ${themeContrast('buttonDisabled')({ theme })};
        background-color: ${themeColor('buttonDisabled')({ theme })};
        border: ${themeBorder('default', 'buttonDisabledBorder')({ theme })};
        cursor: not-allowed;
      }`
      : undefined};
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
