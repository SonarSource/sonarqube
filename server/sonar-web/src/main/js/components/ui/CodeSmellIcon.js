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
// @flow
import React from 'react';

export default class CodeSmellIcon extends React.Component {
  render () {
    /* eslint-disable max-len */
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 14 14" width="14" height="14" style={{ position: 'relative', top: -1, verticalAlign: 'middle' }}>
          <path style={{ fill: 'currentColor' }} d="M6.5 0C2.9 0 0 2.9 0 6.5S2.9 13 6.5 13 13 10.1 13 6.5 10.1 0 6.5 0zM6 6h1v1H6V6zm-4.1.2c-.1 0-.2-.1-.2-.2 0-.4.2-1.3.7-2.1.5-1 1.3-1.5 1.6-1.7.1-.1.2 0 .3.1l1.4 2.5c0 .1 0 .2-.1.3-.2.1-.3.3-.4.4-.1.2-.2.4-.2.6 0 .1-.1.2-.2.2l-2.9-.1zm6.7 4.7c-.3.2-1.2.5-2.2.5-1 0-1.8-.4-2.2-.5-.1-.1-.1-.2-.1-.3l1.4-2.5c.1-.1.2-.1.3-.1.2.1.4.1.6.1.2 0 .4 0 .6-.1.1 0 .2 0 .3.1l1.4 2.5c0 .1 0 .2-.1.3zm2.6-4.5l-2.8.1c-.1 0-.2-.1-.2-.2 0-.2-.1-.4-.2-.6l-.4-.4c-.1 0-.2-.2-.1-.2l1.4-2.5c.1-.1.2-.1.3-.1.3.2 1 .7 1.6 1.6.5.9.6 1.8.7 2.1-.1.1-.1.2-.3.2z"/>
        </svg>
    );
  }
}
