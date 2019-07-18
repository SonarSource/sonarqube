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
import * as differenceInDays from 'date-fns/difference_in_days';
import * as React from 'react';
import { connect } from 'react-redux';
import { lazyLoad } from 'sonar-ui-common/components/lazyLoad';
import { parseDate, toShortNotSoISOString } from 'sonar-ui-common/helpers/dates';
import { hasMessage } from 'sonar-ui-common/helpers/l10n';
import { get, save } from 'sonar-ui-common/helpers/storage';
import { showLicense } from '../../api/marketplace';
import { Location, Router, withRouter } from '../../components/hoc/withRouter';
import { isSonarCloud } from '../../helpers/system';
import { isLoggedIn } from '../../helpers/users';
import { getAppState, getCurrentUser, Store } from '../../store/rootReducer';
import { skipOnboarding } from '../../store/users';
import { OnboardingContext } from './OnboardingContext';

const OnboardingModal = lazyLoad(() => import('../../apps/tutorials/onboarding/OnboardingModal'));
const LicensePromptModal = lazyLoad(
  () => import('../../apps/marketplace/components/LicensePromptModal'),
  'LicensePromptModal'
);

interface StateProps {
  canAdmin?: boolean;
  currentEdition?: T.EditionKey;
  currentUser: T.CurrentUser;
}

interface DispatchProps {
  skipOnboarding: () => void;
}

interface OwnProps {
  children?: React.ReactNode;
}

interface WithRouterProps {
  location: Pick<Location, 'pathname'>;
  router: Pick<Router, 'push'>;
}

type Props = StateProps & DispatchProps & OwnProps & WithRouterProps;

export enum ModalKey {
  license,
  onboarding
}

interface State {
  modal?: ModalKey;
}

const LICENSE_PROMPT = 'sonarqube.license.prompt';

export class StartupModal extends React.PureComponent<Props, State> {
  state: State = {};

  componentDidMount() {
    this.tryAutoOpenLicense().catch(this.tryAutoOpenOnboarding);
  }

  closeOnboarding = () => {
    this.setState(state => {
      if (state.modal !== ModalKey.license) {
        this.props.skipOnboarding();
        return { modal: undefined };
      }
      return null;
    });
  };

  closeLicense = () => {
    this.setState(state => {
      if (state.modal === ModalKey.license) {
        return { modal: undefined };
      }
      return null;
    });
  };

  openOnboarding = () => {
    this.setState({ modal: ModalKey.onboarding });
  };

  openProjectOnboarding = (organization?: T.Organization) => {
    this.setState({ modal: undefined });
    const state: { organization?: string; tab?: string } = {};
    if (organization) {
      state.organization = organization.key;
      state.tab = organization.alm ? 'auto' : 'manual';
    }
    this.props.router.push({ pathname: `/projects/create`, state });
  };

  tryAutoOpenLicense = () => {
    const { canAdmin, currentEdition, currentUser } = this.props;
    const hasLicenseManager = hasMessage('license.prompt.title');
    const hasLicensedEdition = currentEdition && currentEdition !== 'community';

    if (canAdmin && hasLicensedEdition && isLoggedIn(currentUser) && hasLicenseManager) {
      const lastPrompt = get(LICENSE_PROMPT, currentUser.login);

      if (!lastPrompt || differenceInDays(new Date(), parseDate(lastPrompt)) >= 1) {
        return showLicense().then(license => {
          if (!license || !license.isValidEdition) {
            save(LICENSE_PROMPT, toShortNotSoISOString(new Date()), currentUser.login);
            this.setState({ modal: ModalKey.license });
            return Promise.resolve();
          }
          return Promise.reject();
        });
      }
    }
    return Promise.reject();
  };

  tryAutoOpenOnboarding = () => {
    if (
      isSonarCloud() &&
      this.props.currentUser.showOnboardingTutorial &&
      !['/about', '/documentation', '/onboarding', '/projects/create', '/create-organization'].some(
        path => this.props.location.pathname.startsWith(path)
      )
    ) {
      this.openOnboarding();
    }
  };

  render() {
    const { modal } = this.state;
    return (
      <OnboardingContext.Provider value={this.openProjectOnboarding}>
        {this.props.children}
        {modal === ModalKey.license && <LicensePromptModal onClose={this.closeLicense} />}
        {modal === ModalKey.onboarding && (
          <OnboardingModal
            onClose={this.closeOnboarding}
            onOpenProjectOnboarding={this.openProjectOnboarding}
          />
        )}
      </OnboardingContext.Provider>
    );
  }
}

const mapStateToProps = (state: Store): StateProps => ({
  canAdmin: getAppState(state).canAdmin,
  currentEdition: getAppState(state).edition,
  currentUser: getCurrentUser(state)
});

const mapDispatchToProps: DispatchProps = { skipOnboarding };

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(withRouter(StartupModal));
