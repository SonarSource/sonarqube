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
import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';
import { ERROR, SUCCESS } from '../../store/globalMessages/duck';

export default class GlobalMessages extends React.PureComponent {
  static propTypes = {
    messages: PropTypes.arrayOf(
      PropTypes.shape({
        id: PropTypes.string.isRequired,
        message: PropTypes.string.isRequired,
        level: PropTypes.oneOf([ERROR, SUCCESS])
      })
    ),
    closeGlobalMessage: PropTypes.func.isRequired
  };

  renderMessage = message => {
    const className = classNames('process-spinner', 'shown', {
      'process-spinner-failed': message.level === ERROR,
      'process-spinner-success': message.level === SUCCESS
    });
    return (
      <div key={message.id} className={className}>
        {message.message}
        <button
          className="process-spinner-close"
          type="button"
          onClick={() => this.props.closeGlobalMessage(message.id)}>
          <i className="icon-close" />
        </button>
      </div>
    );
  };

  render() {
    const { messages } = this.props;

    if (messages.length === 0) {
      return null;
    }

    return <div className="processes-container">{messages.map(this.renderMessage)}</div>;
  }
}
