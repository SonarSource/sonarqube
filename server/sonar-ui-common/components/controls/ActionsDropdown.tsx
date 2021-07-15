/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import * as classNames from 'classnames';
import { LocationDescriptor } from 'history';
import * as React from 'react';
import { Link } from 'react-router';
import { translate } from '../../helpers/l10n';
import DropdownIcon from '../icons/DropdownIcon';
import SettingsIcon from '../icons/SettingsIcon';
import { PopupPlacement } from '../ui/popups';
import { Button } from './buttons';
import { ClipboardBase } from './clipboard';
import Dropdown from './Dropdown';
import Tooltip from './Tooltip';

export interface ActionsDropdownProps {
  className?: string;
  children: React.ReactNode;
  onOpen?: () => void;
  overlayPlacement?: PopupPlacement;
  small?: boolean;
  toggleClassName?: string;
}

export default function ActionsDropdown(props: ActionsDropdownProps) {
  const { children, className, overlayPlacement, small, toggleClassName } = props;
  return (
    <Dropdown
      className={className}
      onOpen={props.onOpen}
      overlay={<ul className="menu">{children}</ul>}
      overlayPlacement={overlayPlacement}>
      <Button
        className={classNames('dropdown-toggle', toggleClassName, {
          'button-small': small,
        })}>
        <SettingsIcon size={small ? 12 : 14} />
        <DropdownIcon className="little-spacer-left" />
      </Button>
    </Dropdown>
  );
}

interface ItemProps {
  className?: string;
  children: React.ReactNode;
  /** used to pass a string to copy to clipboard */
  copyValue?: string;
  destructive?: boolean;
  /** used to pass a name of downloaded file */
  download?: string;
  id?: string;
  onClick?: () => void;
  to?: LocationDescriptor;
}

export class ActionsDropdownItem extends React.PureComponent<ItemProps> {
  handleClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    if (this.props.onClick) {
      this.props.onClick();
    }
  };

  render() {
    const className = classNames(this.props.className, { 'text-danger': this.props.destructive });

    if (this.props.download && typeof this.props.to === 'string') {
      return (
        <li>
          <a
            className={className}
            download={this.props.download}
            href={this.props.to}
            id={this.props.id}>
            {this.props.children}
          </a>
        </li>
      );
    }

    if (this.props.to) {
      return (
        <li>
          <Link className={className} id={this.props.id} to={this.props.to}>
            {this.props.children}
          </Link>
        </li>
      );
    }

    if (this.props.copyValue) {
      return (
        <ClipboardBase>
          {({ setCopyButton, copySuccess }) => (
            <Tooltip overlay={translate('copied_action')} visible={copySuccess}>
              <li data-clipboard-text={this.props.copyValue} ref={setCopyButton}>
                <a className={className} href="#" id={this.props.id} onClick={this.handleClick}>
                  {this.props.children}
                </a>
              </li>
            </Tooltip>
          )}
        </ClipboardBase>
      );
    }

    return (
      <li>
        <a className={className} href="#" id={this.props.id} onClick={this.handleClick}>
          {this.props.children}
        </a>
      </li>
    );
  }
}

export function ActionsDropdownDivider() {
  return <li className="divider" />;
}
