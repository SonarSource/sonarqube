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
import classNames from 'classnames';
import * as React from 'react';
import { colors } from '../../app/theme';
import ChevronRightIcon from '../icons/ChevronRightIcon';
import ClearIcon, { ClearIconProps } from '../icons/ClearIcon';
import DeleteIcon from '../icons/DeleteIcon';
import EditIcon from '../icons/EditIcon';
import { IconProps } from '../icons/Icon';
import './buttons.css';
import Tooltip, { TooltipProps } from './Tooltip';

type AllowedButtonAttributes = Pick<
  React.ButtonHTMLAttributes<HTMLButtonElement>,
  | 'aria-label'
  | 'className'
  | 'disabled'
  | 'id'
  | 'style'
  | 'title'
  | 'onFocus'
  | 'onBlur'
  | 'onMouseOver'
  | 'onMouseLeave'
  | 'tabIndex'
  | 'role'
>;

interface ButtonProps extends AllowedButtonAttributes {
  autoFocus?: boolean;
  children?: React.ReactNode;
  innerRef?: React.Ref<HTMLButtonElement>;
  name?: string;
  onClick?: () => void;
  preventDefault?: boolean;
  stopPropagation?: boolean;
  type?: 'button' | 'submit' | 'reset';
}

export class Button extends React.PureComponent<ButtonProps> {
  handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    const { disabled, onClick, preventDefault = true, stopPropagation = false } = this.props;

    if (preventDefault || disabled) {
      event.preventDefault();
    }
    if (stopPropagation) {
      event.stopPropagation();
    }

    if (onClick && !disabled) {
      onClick();
    }
  };

  render() {
    const {
      className,
      disabled,
      innerRef,
      onClick,
      preventDefault,
      stopPropagation,
      type = 'button',
      ...props
    } = this.props;

    // Instead of undoing button style we simply not apply the class.
    const isPlain = className && className.indexOf('button-plain') !== -1;
    return (
      <button
        {...props}
        aria-disabled={disabled}
        disabled={disabled}
        className={classNames(isPlain ? '' : 'button', className, { disabled })}
        id={this.props.id}
        onClick={this.handleClick}
        ref={this.props.innerRef}
        // eslint-disable-next-line react/button-has-type
        type={type}
      />
    );
  }
}

export function ButtonLink({ className, ...props }: ButtonProps) {
  return <Button {...props} className={classNames('button-link', className)} />;
}

export function ButtonPlain({ className, ...props }: ButtonProps) {
  return <Button {...props} className={classNames('button-plain', className)} />;
}

export function SubmitButton(props: Omit<ButtonProps, 'type'>) {
  // do not prevent default to actually submit a form
  return <Button {...props} preventDefault={false} type="submit" />;
}

export function ResetButtonLink(props: Omit<ButtonProps, 'type'>) {
  return <ButtonLink {...props} type="reset" />;
}

export interface ButtonIconProps extends ButtonProps {
  'aria-label'?: string;
  'aria-labelledby'?: string;
  className?: string;
  color?: string;
  onClick?: () => void;
  tooltip?: React.ReactNode;
  tooltipProps?: Partial<TooltipProps>;
}

export function ButtonIcon(props: ButtonIconProps) {
  const { className, color, tooltip, tooltipProps, ...other } = props;
  return (
    <Tooltip mouseEnterDelay={0.4} overlay={tooltip} {...tooltipProps}>
      <Button
        className={classNames(className, 'button-icon')}
        stopPropagation
        style={{ color: color || colors.darkBlue }}
        {...other}
      />
    </Tooltip>
  );
}

interface ClearButtonProps extends ButtonIconProps {
  className?: string;
  iconProps?: ClearIconProps;
  onClick?: () => void;
}

export function ClearButton({ color, iconProps = {}, ...props }: ClearButtonProps) {
  return (
    <ButtonIcon color={color || colors.gray60} {...props}>
      <ClearIcon {...iconProps} />
    </ButtonIcon>
  );
}

interface ActionButtonProps extends ButtonIconProps {
  className?: string;
  iconProps?: IconProps;
  onClick?: () => void;
}

export function DeleteButton({ iconProps = {}, ...props }: ActionButtonProps) {
  return (
    <ButtonIcon color={colors.red} {...props}>
      <DeleteIcon {...iconProps} />
    </ButtonIcon>
  );
}

export function EditButton({ iconProps = {}, ...props }: ActionButtonProps) {
  return (
    <ButtonIcon {...props}>
      <EditIcon {...iconProps} />
    </ButtonIcon>
  );
}

export function ListButton({ className, children, ...props }: ButtonProps) {
  return (
    <Button className={classNames('button-list', className)} {...props}>
      {children}
      <ChevronRightIcon />
    </Button>
  );
}
