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
import classNames from 'classnames';
import { ERROR, SUCCESS } from '../store/globalMessages';

export default class GlobalMessages extends React.Component {
  static propTypes = {
    messages: React.PropTypes.arrayOf(React.PropTypes.shape({
      id: React.PropTypes.string.isRequired,
      message: React.PropTypes.string.isRequired,
      level: React.PropTypes.oneOf([ERROR, SUCCESS])
    }))
  };

  renderMessage (message) {
    const className = classNames('alert', {
      'alert-danger': message.level === ERROR,
      'alert-success': message.level === SUCCESS
    });
    return <div key={message.id} className={className}>{message.message}</div>;
  }

  render () {
    const { messages } = this.props;

    if (messages.length === 0) {
      return null;
    }

    return (
        <div>{messages.map(this.renderMessage)}</div>
    );
  }
}
