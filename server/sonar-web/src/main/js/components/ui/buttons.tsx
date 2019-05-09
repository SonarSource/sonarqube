/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as React from 'react';
import * as classNames from 'classnames';
import * as theme from '../../app/theme';
import ChevronRightIcon from '../icons-components/ChevronRightcon';
import ClearIcon from '../icons-components/ClearIcon';
import DeleteIcon from '../icons-components/DeleteIcon';
import EditIcon from '../icons-components/EditIcon';
import Tooltip from '../controls/Tooltip';
import { IconProps } from '../icons-components/Icon';
import './buttons.css';

type AllowedButtonAttributes = Pick<
  React.ButtonHTMLAttributes<HTMLButtonElement>,
  'className' | 'disabled' | 'id' | 'style' | 'title'
>;

interface ButtonProps extends AllowedButtonAttributes {
  autoFocus?: boolean;
  children?: React.ReactNode;
  innerRef?: (node: HTMLElement | null) => void;
  name?: string;
  onClick?: () => void;
  preventDefault?: boolean;
  stopPropagation?: boolean;
  type?: string;
}

export class Button extends React.PureComponent<ButtonProps> {
  handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    const { onClick, preventDefault = true, stopPropagation = false } = this.props;

    event.currentTarget.blur();
    if (preventDefault) event.preventDefault();
    if (stopPropagation) event.stopPropagation();
    if (onClick) onClick();
  };

  render() {
    const {
      className,
      innerRef,
      onClick,
      preventDefault,
      stopPropagation,
      type = 'button',
      ...props
    } = this.props;
    return (
      // eslint-disable-next-line react/button-has-type
      <button
        {...props}
        className={classNames('button', className)}
        disabled={this.props.disabled}
        id={this.props.id}
        onClick={this.handleClick}
        ref={this.props.innerRef}
        type={type}
      />
    );
  }
}

export function ButtonLink({ className, ...props }: ButtonProps) {
  return <Button {...props} className={classNames('button-link', className)} />;
}

export function SubmitButton(props: T.Omit<ButtonProps, 'type'>) {
  // do not prevent default to actually submit a form
  return <Button {...props} preventDefault={false} type="submit" />;
}

export function ResetButtonLink(props: T.Omit<ButtonProps, 'type'>) {
  return <ButtonLink {...props} type="reset" />;
}

interface ButtonIconProps extends ButtonProps {
  className?: string;
  color?: string;
  onClick?: () => void;
  tooltip?: string;
}

export function ButtonIcon(props: ButtonIconProps) {
  const { className, color = theme.darkBlue, tooltip, ...other } = props;
  const buttonComponent = (
    <Button
      className={classNames(className, 'button-icon')}
      stopPropagation={true}
      style={{ color }}
      {...other}
    />
  );
  if (tooltip) {
    return (
      <Tooltip mouseEnterDelay={0.4} overlay={tooltip}>
        {buttonComponent}
      </Tooltip>
    );
  }
  return buttonComponent;
}

interface ActionButtonProps extends ButtonIconProps {
  className?: string;
  iconProps?: IconProps;
  onClick?: () => void;
}

export function ClearButton({ color = theme.gray60, iconProps = {}, ...props }: ActionButtonProps) {
  return (
    <ButtonIcon color={color} {...props}>
      <ClearIcon {...iconProps} />
    </ButtonIcon>
  );
}

export function DeleteButton({ iconProps = {}, ...props }: ActionButtonProps) {
  return (
    <ButtonIcon color={theme.red} {...props}>
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
