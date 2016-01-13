/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
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
import React from 'react';
import md5 from 'blueimp-md5';

export default React.createClass({
  propTypes: {
    email: React.PropTypes.string,
    size: React.PropTypes.number.isRequired
  },

  render() {
    const shouldShowAvatar = window.SS && window.SS.lf && window.SS.lf.enableGravatar;
    if (!shouldShowAvatar) {
      return null;
    }
    const emailHash = md5.md5(this.props.email || '').trim();
    const url = ('' + window.SS.lf.gravatarServerUrl)
            .replace('{EMAIL_MD5}', emailHash)
            .replace('{SIZE}', this.props.size * 2);
    return <img className="rounded"
                src={url}
                width={this.props.size}
                height={this.props.size}
                alt={this.props.email}/>;
  }
});
