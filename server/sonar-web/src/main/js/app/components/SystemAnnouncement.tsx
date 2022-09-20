/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import { isEmpty, keyBy } from 'lodash';
import * as React from 'react';
import { getValues } from '../../api/settings';
import { Alert } from '../../components/ui/Alert';
import { GlobalSettingKeys, SettingValue } from '../../types/settings';
import './SystemAnnouncement.css';

interface State {
  displayMessage: boolean;
  message: string;
}

export default class SystemAnnouncement extends React.PureComponent<{}, State> {
  state: State = { displayMessage: false, message: '' };

  componentDidMount() {
    this.getSettings();
    document.addEventListener('visibilitychange', this.handleVisibilityChange);
  }

  componentWillUnmount() {
    document.removeEventListener('visibilitychange', this.handleVisibilityChange);
  }

  getSettings = async () => {
    const values: SettingValue[] = await getValues({
      keys: [GlobalSettingKeys.DisplaySystemMessage, GlobalSettingKeys.SystemMessage].join(',')
    });
    const settings = keyBy(values, 'key');

    this.setState({
      displayMessage: settings[GlobalSettingKeys.DisplaySystemMessage].value === 'true',
      message:
        (settings[GlobalSettingKeys.SystemMessage] &&
          settings[GlobalSettingKeys.SystemMessage].value) ||
        ''
    });
  };

  handleVisibilityChange = () => {
    if (document.visibilityState === 'visible') {
      this.getSettings();
    }
  };

  render() {
    const { displayMessage, message } = this.state;
    if (!displayMessage || isEmpty(message)) {
      return null;
    }

    return (
      <div className="system-announcement-wrapper">
        <Alert
          className="system-announcement-banner"
          title={message}
          display="banner"
          variant="warning">
          {message}
        </Alert>
      </div>
    );
  }
}
