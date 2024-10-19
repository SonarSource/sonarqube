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
import { differenceInDays } from 'date-fns';
import * as React from 'react';
import { showLicense } from '../../api/editions';
import { parseDate, toShortISO8601String } from '../../helpers/dates';
import { hasMessage } from '../../helpers/l10n';
import { get, save } from '../../helpers/storage';
import { AppState } from '../../types/appstate';
import { EditionKey } from '../../types/editions';
import { CurrentUser, isLoggedIn } from '../../types/users';
import LicensePromptModal from './LicensePromptModal';
import withAppStateContext from './app-state/withAppStateContext';
import withCurrentUserContext from './current-user/withCurrentUserContext';

interface Props {
  appState: AppState;
  currentUser: CurrentUser;
}

interface State {
  open?: boolean;
}

const LICENSE_PROMPT = 'sonarqube.license.prompt';

export class StartupModal extends React.PureComponent<React.PropsWithChildren<Props>, State> {
  state: State = {};

  componentDidMount() {
    this.tryAutoOpenLicense();
  }

  closeLicense = () => {
    this.setState({ open: false });
  };

  tryAutoOpenLicense = () => {
    const { appState, currentUser } = this.props;
    const hasLicenseManager = hasMessage('license.prompt.title');
    const hasLicensedEdition = appState.edition && appState.edition !== EditionKey.community;

    if (appState.canAdmin && hasLicensedEdition && isLoggedIn(currentUser) && hasLicenseManager) {
      const lastPrompt = get(LICENSE_PROMPT, currentUser.login);

      if (!lastPrompt || differenceInDays(new Date(), parseDate(lastPrompt)) >= 1) {
        showLicense()
          .then((license) => {
            if (!license || !license.isValidEdition) {
              save(LICENSE_PROMPT, toShortISO8601String(new Date()), currentUser.login);
              this.setState({ open: true });
            }
          })
          .catch(() => {});
      }
    }
  };

  render() {
    const { open } = this.state;
    return open ? <LicensePromptModal onClose={this.closeLicense} /> : null;
  }
}

export default withCurrentUserContext(withAppStateContext(StartupModal));
