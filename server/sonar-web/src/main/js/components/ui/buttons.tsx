/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import ClearIcon from '../icons-components/ClearIcon';
import './buttons.css';
import EditIcon from '../icons-components/EditIcon';

interface ButtonIconProps {
  children: React.ReactNode;
  className?: string;
  color?: string;
  onClick?: () => void;
  [x: string]: any;
}

export class ButtonIcon extends React.PureComponent<ButtonIconProps> {
  handleClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    event.stopPropagation();
    if (this.props.onClick) {
      this.props.onClick();
    }
  };

  render() {
    const { children, className, color = theme.darkBlue, onClick, ...props } = this.props;
    return (
      <button
        className={classNames(className, 'button-icon')}
        onClick={this.handleClick}
        style={{ color }}
        {...props}>
        {children}
      </button>
    );
  }
}

interface ActionButtonProps {
  className?: string;
  onClick?: () => void;
  [x: string]: any;
}

export function DeleteButton(props: ActionButtonProps) {
  return (
    <ButtonIcon color={theme.red} {...props}>
      <ClearIcon />
    </ButtonIcon>
  );
}

export function EditButton(props: ActionButtonProps) {
  return (
    <ButtonIcon {...props}>
      <EditIcon />
    </ButtonIcon>
  );
}
