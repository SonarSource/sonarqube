/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import { connect } from 'react-redux';
import md5 from 'blueimp-md5';
import classNames from 'classnames';
import { getSettingValue } from '../../store/rootReducer';

class Avatar extends React.Component {
  static propTypes = {
    enableGravatar: React.PropTypes.bool.isRequired,
    gravatarServerUrl: React.PropTypes.string.isRequired,
    email: React.PropTypes.string,
    hash: React.PropTypes.string,
    size: React.PropTypes.number.isRequired,
    className: React.PropTypes.string
  };

  render() {
    if (!this.props.enableGravatar) {
      return null;
    }

    const emailHash = this.props.hash || md5.md5((this.props.email || '').toLowerCase()).trim();
    const url = this.props.gravatarServerUrl
      .replace('{EMAIL_MD5}', emailHash)
      .replace('{SIZE}', this.props.size * 2);

    const className = classNames(this.props.className, 'rounded');

    return (
      <img
        className={className}
        src={url}
        width={this.props.size}
        height={this.props.size}
        alt={this.props.email}
      />
    );
  }
}

const mapStateToProps = state => ({
  enableGravatar: (getSettingValue(state, 'sonar.lf.enableGravatar') || {}).value === 'true',
  gravatarServerUrl: (getSettingValue(state, 'sonar.lf.gravatarServerUrl') || {}).value
});

export default connect(mapStateToProps)(Avatar);

export const unconnectedAvatar = Avatar;
