/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { Button } from '../../../components/controls/buttons';
import { DropdownOverlay } from '../../../components/controls/Dropdown';
import Toggler from '../../../components/controls/Toggler';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { addGlobalErrorMessage, addGlobalSuccessMessage } from '../../../helpers/globalMessages';
import { translate } from '../../../helpers/l10n';
import { openHotspot, probeSonarLintServers } from '../../../helpers/sonarlint';
import { Ide } from '../../../types/sonarlint';
import { HotspotOpenInIdeOverlay } from './HotspotOpenInIdeOverlay';

interface Props {
  projectKey: string;
  hotspotKey: string;
}

interface State {
  loading: boolean;
  ides: Array<Ide>;
}

export default class HotspotOpenInIdeButton extends React.PureComponent<Props, State> {
  mounted = false;

  state = {
    loading: false,
    ides: [],
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleOnClick = async () => {
    this.setState({ loading: true, ides: [] });
    const ides = await probeSonarLintServers();
    if (ides.length === 0) {
      if (this.mounted) {
        this.setState({ loading: false });
      }
      this.showError();
    } else if (ides.length === 1) {
      this.openHotspot(ides[0]);
    } else if (this.mounted) {
      this.setState({ loading: false, ides });
    }
  };

  openHotspot = (ide: Ide) => {
    this.setState({ loading: true, ides: [] });
    const { projectKey, hotspotKey } = this.props;
    return openHotspot(ide.port, projectKey, hotspotKey)
      .then(this.showSuccess)
      .catch(this.showError)
      .finally(this.cleanState);
  };

  showError = () => addGlobalErrorMessage(translate('hotspots.open_in_ide.failure'));

  showSuccess = () => addGlobalSuccessMessage(translate('hotspots.open_in_ide.success'));

  cleanState = () => {
    if (this.mounted) {
      this.setState({ loading: false, ides: [] });
    }
  };

  render() {
    return (
      <Toggler
        open={this.state.ides.length > 1}
        onRequestClose={this.cleanState}
        overlay={
          <DropdownOverlay>
            <HotspotOpenInIdeOverlay ides={this.state.ides} onIdeSelected={this.openHotspot} />
          </DropdownOverlay>
        }
      >
        <Button onClick={this.handleOnClick}>
          {translate('hotspots.open_in_ide.open')}
          <DeferredSpinner loading={this.state.loading} className="spacer-left" />
        </Button>
      </Toggler>
    );
  }
}
