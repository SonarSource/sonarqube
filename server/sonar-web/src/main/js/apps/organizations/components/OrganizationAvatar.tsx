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
import { Avatar } from '~design-system';
import * as React from 'react';
import { OrganizationBase } from '../../../types/types';
import './OrganizationAvatar.css';

interface Props {
  className?: string;
  organization: Pick<OrganizationBase, 'avatar' | 'name'>;
  small?: boolean;
}

interface State {
  imgLoadError: boolean;
}

export default class OrganizationAvatar extends React.PureComponent<Props, State> {
  state = { imgLoadError: false };

  handleImgError = () => {
    this.setState({ imgLoadError: true });
  };

  render() {
    const { className, organization, small } = this.props;
    const { imgLoadError } = this.state;
    return organization.avatar && !imgLoadError ? (
      <div
        className={classNames(
          'navbar-context-avatar',
          'rounded',
          { 'no-border': !organization.avatar, 'is-small': small },
          className,
        )}
      >
        <img
          alt={organization.name}
          className="rounded"
          onError={this.handleImgError}
          src={organization.avatar}
        />
      </div>
    ) : (
      <Avatar name={organization.name} size={small ? 'xs' : 'md'} />
    );
  }
}
