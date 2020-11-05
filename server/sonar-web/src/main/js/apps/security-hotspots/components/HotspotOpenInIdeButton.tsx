/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import addGlobalErrorMessage from '../../../app/utils/addGlobalErrorMessage';
import addGlobalSuccessMessage from '../../../app/utils/addGlobalSuccessMessage';
import { openHotspot, probeSonarLintServers } from '../../../helpers/sonarlint';

interface Props {
  projectKey: string;
  hotspotKey: string;
}

interface State {
  inDiscovery: boolean;
}

export default class HotspotOpenInIdeButton extends React.PureComponent<Props, State> {
  state = {
    inDiscovery: false
  };

  handleOnClick = () => {
    const { projectKey, hotspotKey } = this.props;
    this.setState({ inDiscovery: true });
    return probeSonarLintServers()
      .then(ides => {
        if (ides.length > 0) {
          const calledPort = ides[0].port;
          return openHotspot(calledPort, projectKey, hotspotKey);
        } else {
          return Promise.reject();
        }
      })
      .then(() => {
        addGlobalSuccessMessage(translate('hotspots.open_in_ide.success'));
      })
      .catch(() => {
        addGlobalErrorMessage(translate('hotspots.open_in_ide.failure'));
      })
      .finally(() => {
        this.setState({ inDiscovery: false });
      });
  };

  render() {
    return (
      <Button onClick={this.handleOnClick}>
        {translate('hotspots.open_in_ide.open')}
        <DeferredSpinner loading={this.state.inDiscovery} className="spacer-left" />
      </Button>
    );
  }
}
