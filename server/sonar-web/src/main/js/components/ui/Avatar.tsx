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
import * as React from 'react';
import { connect } from 'react-redux';
import * as classNames from 'classnames';
import { getGlobalSettingValue } from '../../store/rootReducer';

interface Props {
  className?: string;
  enableGravatar: boolean;
  gravatarServerUrl: string;
  hash?: string;
  name: string;
  size: number;
}

class Avatar extends React.PureComponent<Props> {
  renderFallback() {
    const className = classNames(this.props.className, 'rounded');
    const color = stringToColor(this.props.name);

    let text = '';
    const words = this.props.name.split(/\s+/).filter(word => word.length > 0);
    if (words.length >= 2) {
      text = words[0][0] + words[1][0];
    } else if (this.props.name.length > 0) {
      text = this.props.name[0];
    }

    return (
      <div
        className={className}
        style={{
          backgroundColor: color,
          color: getTextColor(color),
          display: 'inline-block',
          fontSize: Math.min(this.props.size / 2, 14),
          fontWeight: 'normal',
          height: this.props.size,
          lineHeight: `${this.props.size}px`,
          textAlign: 'center',
          verticalAlign: 'top',
          width: this.props.size
        }}>
        {text.toUpperCase()}
      </div>
    );
  }

  render() {
    if (!this.props.enableGravatar || !this.props.hash) {
      return this.renderFallback();
    }

    const url = this.props.gravatarServerUrl
      .replace('{EMAIL_MD5}', this.props.hash)
      .replace('{SIZE}', String(this.props.size * 2));

    return (
      <img
        className={classNames(this.props.className, 'rounded')}
        src={url}
        width={this.props.size}
        height={this.props.size}
        alt={this.props.name}
      />
    );
  }
}

const mapStateToProps = (state: any) => ({
  enableGravatar: (getGlobalSettingValue(state, 'sonar.lf.enableGravatar') || {}).value === 'true',
  gravatarServerUrl: (getGlobalSettingValue(state, 'sonar.lf.gravatarServerUrl') || {}).value
});

export default connect(mapStateToProps)(Avatar);

export const unconnectedAvatar = Avatar;

/* eslint-disable no-bitwise, no-mixed-operators */
function stringToColor(str: string) {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash);
  }
  let color = '#';
  for (let i = 0; i < 3; i++) {
    const value = (hash >> (i * 8)) & 0xff;
    color += ('00' + value.toString(16)).substr(-2);
  }
  return color;
}

function getTextColor(background: string) {
  const rgb = parseInt(background.substr(1), 16);
  const r = (rgb >> 16) & 0xff;
  const g = (rgb >> 8) & 0xff;
  const b = (rgb >> 0) & 0xff;
  const luma = 0.2126 * r + 0.7152 * g + 0.0722 * b;
  return luma > 140 ? '#222' : '#fff';
}
