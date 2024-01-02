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
import { To } from 'react-router-dom';
import Link from '../common/Link';
import DropdownIcon from '../icons/DropdownIcon';
import SettingsIcon from '../icons/SettingsIcon';
import { PopupPlacement } from '../ui/popups';
import Dropdown from './Dropdown';
import Tooltip, { Placement } from './Tooltip';
import { Button, ButtonPlain } from './buttons';

export interface ActionsDropdownProps {
  className?: string;
  children: React.ReactNode;
  label?: string;
  onOpen?: () => void;
  overlayPlacement?: PopupPlacement;
  small?: boolean;
  toggleClassName?: string;
  disabled?: boolean;
}

export default function ActionsDropdown(props: ActionsDropdownProps) {
  const { children, className, label, overlayPlacement, small, toggleClassName } = props;
  return (
    <Dropdown
      className={className}
      onOpen={props.onOpen}
      overlay={<ul className="menu">{children}</ul>}
      overlayPlacement={overlayPlacement}
    >
      <Button
        aria-label={label}
        className={classNames('dropdown-toggle', toggleClassName, {
          'button-small': small,
        })}
        disabled={props.disabled}
      >
        <SettingsIcon size={small ? 12 : 14} />
        <DropdownIcon className="little-spacer-left" />
      </Button>
    </Dropdown>
  );
}

interface ItemProps {
  className?: string;
  children: React.ReactNode;
  destructive?: boolean;
  disabled?: boolean;
  label?: string;
  tooltipOverlay?: React.ReactNode;
  tooltipPlacement?: Placement;
  /** used to pass a name of downloaded file */
  download?: string;
  id?: string;
  onClick?: () => void;
  to?: To;
}

export class ActionsDropdownItem extends React.PureComponent<ItemProps> {
  handleClick = (event?: React.SyntheticEvent<HTMLAnchorElement>) => {
    if (event) {
      event.preventDefault();
      event.currentTarget.blur();
    }
    if (this.props.onClick) {
      this.props.onClick();
    }
  };

  render() {
    const className = classNames(this.props.className, { 'text-danger': this.props.destructive });
    let { children } = this.props;
    const { tooltipOverlay, tooltipPlacement, label } = this.props;

    if (this.props.download && typeof this.props.to === 'string') {
      children = (
        <a
          className={className}
          aria-label={label}
          download={this.props.download}
          href={this.props.to}
          id={this.props.id}
        >
          {children}
        </a>
      );
    } else if (this.props.to) {
      children = (
        <Link className={className} id={this.props.id} to={this.props.to} aria-label={label}>
          {children}
        </Link>
      );
    } else {
      children = (
        <ButtonPlain
          className={className}
          disabled={this.props.disabled}
          preventDefault
          id={this.props.id}
          onClick={this.handleClick}
          aria-label={label}
        >
          {children}
        </ButtonPlain>
      );
    }

    if (tooltipOverlay !== undefined) {
      return (
        <Tooltip overlay={tooltipOverlay} placement={tooltipPlacement}>
          <li>{children}</li>
        </Tooltip>
      );
    }

    return <li>{children}</li>;
  }
}

export function ActionsDropdownDivider() {
  return <li className="divider" />;
}
