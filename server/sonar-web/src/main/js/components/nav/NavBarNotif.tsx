/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import CloseIcon from '../icons-components/CloseIcon';

interface Props {
  children?: React.ReactNode;
  className?: string;
  onCancel?: () => {};
}

export default class NavBarNotif extends React.PureComponent<Props> {
  handleCancel = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    if (this.props.onCancel) {
      this.props.onCancel();
    }
  };

  render() {
    if (!this.props.children) {
      return null;
    }
    return (
      <div className={classNames('navbar-notif', this.props.className)}>
        <div className="navbar-limited clearfix">
          <div className={classNames({ 'navbar-notif-cancelable': !!this.props.onCancel })}>
            {this.props.children}
            {this.props.onCancel && (
              <a className="button-link text-danger" href="#" onClick={this.handleCancel}>
                <CloseIcon />
              </a>
            )}
          </div>
        </div>
      </div>
    );
  }
}
