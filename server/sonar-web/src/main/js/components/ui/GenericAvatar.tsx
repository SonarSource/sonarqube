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

interface Props {
  className?: string;
  name: string;
  size: number;
}

export default function GenericAvatar({ className, name, size }: Props) {
  const color = stringToColor(name);

  let text = '';
  const words = name.split(/\s+/).filter(word => word.length > 0);
  if (words.length >= 2) {
    text = words[0][0] + words[1][0];
  } else if (name.length > 0) {
    text = name[0];
  }

  return (
    <div
      className={classNames(className, 'rounded')}
      style={{
        backgroundColor: color,
        color: getTextColor(color),
        display: 'inline-block',
        fontSize: Math.min(size / 2, 14),
        fontWeight: 'normal',
        height: size,
        lineHeight: `${size}px`,
        textAlign: 'center',
        verticalAlign: 'top',
        width: size
      }}>
      {text.toUpperCase()}
    </div>
  );
}

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
