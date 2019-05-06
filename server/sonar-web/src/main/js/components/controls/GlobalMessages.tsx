/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { ClearButton } from '../ui/buttons';
import { cutLongWords } from '../../helpers/path';
import './GlobalMessages.css';

interface Message {
  id: string;
  level: 'ERROR' | 'SUCCESS';
  message: string;
}

export interface Props {
  closeGlobalMessage: (id: string) => void;
  messages: Message[];
}

export default function GlobalMessages({ closeGlobalMessage, messages }: Props) {
  if (messages.length === 0) {
    return null;
  }

  return (
    <div className="processes-container">
      {messages.map(message => (
        <GlobalMessage closeGlobalMessage={closeGlobalMessage} key={message.id} message={message} />
      ))}
    </div>
  );
}

export class GlobalMessage extends React.PureComponent<{
  closeGlobalMessage: (id: string) => void;
  message: Message;
}> {
  handleClose = () => {
    this.props.closeGlobalMessage(this.props.message.id);
  };

  render() {
    const { message } = this.props;
    return (
      <div
        className={classNames('process-spinner', 'shown', {
          'process-spinner-failed': message.level === 'ERROR',
          'process-spinner-success': message.level === 'SUCCESS'
        })}
        key={message.id}>
        {cutLongWords(message.message)}
        <ClearButton
          className="button-small process-spinner-close"
          color="#fff"
          onClick={this.handleClose}
        />
      </div>
    );
  }
}
