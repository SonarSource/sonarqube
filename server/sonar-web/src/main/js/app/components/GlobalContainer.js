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
// @flow
import React from 'react';
import GlobalNav from './nav/global/GlobalNav';
import GlobalFooterContainer from './GlobalFooterContainer';
import GlobalMessagesContainer from './GlobalMessagesContainer';

export default function GlobalContainer(props: Object) {
  // it is important to pass `location` down to `GlobalNav` to trigger render on url change

  return (
    <div className="global-container">
      <div className="page-wrapper page-wrapper-global" id="container">
        <div className="page-container">
          <GlobalNav location={props.location} />
          <GlobalMessagesContainer />
          {props.children}
        </div>
      </div>
      <GlobalFooterContainer />
    </div>
  );
}
