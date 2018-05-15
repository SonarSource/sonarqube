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
import { Button } from '../ui/buttons';

interface Message {
  id: string;
  level: 'ERROR' | 'SUCCESS';
  message: string;
}

interface Props {
  closeGlobalMessage: (id: string) => void;
  messages: Message[];
}

export default class GlobalMessages extends React.PureComponent<Props> {
  renderMessage = (message: Message) => {
    const className = classNames('process-spinner', 'shown', {
      'process-spinner-failed': message.level === 'ERROR',
      'process-spinner-success': message.level === 'SUCCESS'
    });
    return (
      <div className={className} key={message.id}>
        {message.message}
        <Button
          className="process-spinner-close"
          onClick={() => this.props.closeGlobalMessage(message.id)}>
          <i className="icon-close" />
        </Button>
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
