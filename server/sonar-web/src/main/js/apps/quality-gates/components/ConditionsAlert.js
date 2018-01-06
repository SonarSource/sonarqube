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
import React, { Component } from 'react';
import { translate } from '../../../helpers/l10n';

export default class ConditionsAlert extends Component {
  state = {
    expanded: false
  };

  handleMoreClick(e) {
    e.preventDefault();
    this.setState({ expanded: true });
  }

  render() {
    const { expanded } = this.state;

    return (
      <div className="big-spacer-bottom">
        {translate('quality_gates.introduction')}
        {!expanded && (
          <a className="spacer-left" href="#" onClick={this.handleMoreClick.bind(this)}>
            {translate('more')}
          </a>
        )}
        {expanded && (
          <div className="spacer-top">
            {translate('quality_gates.health_icons')}
            <ul>
              <li className="little-spacer-top">
                <i className="icon-alert-ok" /> {translate('alerts.notes.ok')}
              </li>
              <li className="little-spacer-top">
                <i className="icon-alert-warn" /> {translate('alerts.notes.warn')}
              </li>
              <li className="little-spacer-top">
                <i className="icon-alert-error" /> {translate('alerts.notes.error')}
              </li>
            </ul>
          </div>
        )}
      </div>
    );
  }
}
