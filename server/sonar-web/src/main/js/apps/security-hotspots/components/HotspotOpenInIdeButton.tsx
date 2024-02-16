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

import {
  ButtonSecondary,
  DropdownMenu,
  DropdownToggler,
  ItemButton,
  PopupPlacement,
  PopupZLevel,
  Spinner,
  addGlobalErrorMessage,
  addGlobalSuccessMessage,
} from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { openHotspot, probeSonarLintServers } from '../../../helpers/sonarlint';
import { Ide } from '../../../types/sonarlint';

interface Props {
  projectKey: string;
  hotspotKey: string;
}

interface State {
  loading: boolean;
  ides: Ide[];
}

export default class HotspotOpenInIdeButton extends React.PureComponent<Props, State> {
  mounted = false;

  state = {
    loading: false,
    ides: [] as Ide[],
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
    this.setState({ loading: true, ides: [] as Ide[] });
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
    const { ides, loading } = this.state;

    return (
      <div>
        <DropdownToggler
          onRequestClose={() => this.cleanState()}
          allowResizing
          open={ides.length > 1}
          placement={PopupPlacement.BottomLeft}
          zLevel={PopupZLevel.Global}
          overlay={
            <DropdownMenu size="auto">
              {ides.map((ide) => {
                const { ideName, description } = ide;
                const label = ideName + (description ? ` - ${description}` : '');

                return (
                  <ItemButton
                    key={ide.port}
                    onClick={() => {
                      this.openHotspot(ide);
                    }}
                  >
                    {label}
                  </ItemButton>
                );
              })}
            </DropdownMenu>
          }
        >
          <ButtonSecondary onClick={this.handleOnClick}>
            {translate('open_in_ide')}
            <Spinner loading={loading} className="sw-ml-4" />
          </ButtonSecondary>
        </DropdownToggler>
      </div>
    );
  }
}
