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
import { setLogLevel } from '../../api/system';
import { translate } from '../../helpers/l10n';

const LOG_LEVELS = ['INFO', 'DEBUG', 'TRACE'];

export default class ItemLogLevel extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = { level: props.value };
  }

  onChange = () => {
    const newValue = this.refs.select.value;
    setLogLevel(newValue).then(() => {
      this.setState({ level: newValue });
    });
  };

  render() {
    const options = LOG_LEVELS.map(level =>
      <option key={level} value={level}>
        {level}
      </option>
    );
    const warning =
      this.state.level !== 'INFO'
        ? <div className="alert alert-danger spacer-top" style={{ wordBreak: 'normal' }}>
            {translate('system.log_level.warning')}
          </div>
        : null;
    return (
      <div>
        <select ref="select" onChange={this.onChange} value={this.state.level}>
          {options}
        </select>
        {warning}
      </div>
    );
  }
}
