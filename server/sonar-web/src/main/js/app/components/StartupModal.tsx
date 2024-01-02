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
import LicensePromptModal from '../../apps/marketplace/components/LicensePromptModal';
import { Location, Router, withRouter } from '../../components/hoc/withRouter';
import { parseDate, toShortNotSoISOString } from '../../helpers/dates';
import { hasMessage } from '../../helpers/l10n';
import { get, save } from '../../helpers/storage';
import { AppState } from '../../types/appstate';
import { EditionKey } from '../../types/editions';
import { CurrentUser, isLoggedIn } from '../../types/users';
import withAppStateContext from './app-state/withAppStateContext';
import withCurrentUserContext from './current-user/withCurrentUserContext';

interface StateProps {
  currentUser: CurrentUser;
}

type Props = {
  children?: React.ReactNode;
  location: Location;
  router: Router;
  appState: AppState;
};

interface State {
  open?: boolean;
}

const LICENSE_PROMPT = 'sonarqube.license.prompt';

export class StartupModal extends React.PureComponent<Props & StateProps, State> {
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
              save(LICENSE_PROMPT, toShortNotSoISOString(new Date()), currentUser.login);
              this.setState({ open: true });
            }
          })
          .catch(() => {});
      }
    }
  };

  render() {
    const { open } = this.state;
    return (
      <>
        {this.props.children}
        {open && <LicensePromptModal onClose={this.closeLicense} />}
      </>
    );
  }
}

export default withCurrentUserContext(withRouter(withAppStateContext(StartupModal)));
