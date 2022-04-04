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
import * as differenceInDays from 'date-fns/difference_in_days';
import * as React from 'react';
import { connect } from 'react-redux';
import { lazyLoadComponent } from 'sonar-ui-common/components/lazyLoadComponent';
import { parseDate, toShortNotSoISOString } from 'sonar-ui-common/helpers/dates';
import { hasMessage } from 'sonar-ui-common/helpers/l10n';
import { get, save } from 'sonar-ui-common/helpers/storage';
import { showLicense } from '../../api/marketplace';
import { Location, Router, withRouter } from '../../components/hoc/withRouter';
import { isLoggedIn } from '../../helpers/users';
import { getAppState, getCurrentUser, Store } from '../../store/rootReducer';
import { EditionKey } from '../../types/editions';

const LicensePromptModal = lazyLoadComponent(
  () => import('../../apps/marketplace/components/LicensePromptModal'),
  'LicensePromptModal'
);

interface StateProps {
  canAdmin?: boolean;
  currentEdition?: EditionKey;
  currentUser: T.CurrentUser;
}

interface OwnProps {
  children?: React.ReactNode;
}

interface WithRouterProps {
  location: Pick<Location, 'pathname'>;
  router: Pick<Router, 'push'>;
}

type Props = StateProps & OwnProps & WithRouterProps;

interface State {
  open?: boolean;
}

const LICENSE_PROMPT = 'sonarqube.license.prompt';

export class StartupModal extends React.PureComponent<Props, State> {
  state: State = {};

  componentDidMount() {
    this.tryAutoOpenLicense();
  }

  closeLicense = () => {
    this.setState({ open: false });
  };

  tryAutoOpenLicense = () => {
    const { canAdmin, currentEdition, currentUser } = this.props;
    const hasLicenseManager = hasMessage('license.prompt.title');
    const hasLicensedEdition = currentEdition && currentEdition !== EditionKey.community;

    if (canAdmin && hasLicensedEdition && isLoggedIn(currentUser) && hasLicenseManager) {
      const lastPrompt = get(LICENSE_PROMPT, currentUser.login);

      if (!lastPrompt || differenceInDays(new Date(), parseDate(lastPrompt)) >= 1) {
        showLicense()
          .then(license => {
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

const mapStateToProps = (state: Store): StateProps => ({
  canAdmin: getAppState(state).canAdmin,
  currentEdition: getAppState(state).edition as EditionKey, // TODO: Fix once AppState is no longer ambiant.
  currentUser: getCurrentUser(state)
});

export default connect(mapStateToProps)(withRouter(StartupModal));
