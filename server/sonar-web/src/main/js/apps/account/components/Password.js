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
import React, { Component } from 'react';

import ChangePasswordView from '../change-password-view';

export default class Password extends Component {
  handleChangePassword () {
    new ChangePasswordView().render();
  }

  render () {
    return (
        <section>
          <h2 className="spacer-bottom">Password</h2>
          <button
              id="change-password"
              onClick={this.handleChangePassword.bind(this)}
              type="submit">
            Change Password
          </button>
        </section>
    );
  }
}
