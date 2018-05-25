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
import * as PropTypes from 'prop-types';
import { connect } from 'react-redux';
import OnboardingModal from '../../apps/tutorials/onboarding/OnboardingModal';
import LicensePromptModal from '../../apps/marketplace/components/LicensePromptModal';
import { showLicense } from '../../api/marketplace';
import { differenceInDays, parseDate, toShortNotSoISOString } from '../../helpers/dates';
import { hasMessage } from '../../helpers/l10n';
import { save, get } from '../../helpers/storage';
import { getCurrentUser, getAppState } from '../../store/rootReducer';
import { skipOnboarding } from '../../store/users/actions';
import { CurrentUser, isLoggedIn } from '../types';

interface StateProps {
  canAdmin: boolean;
  currentEdition: string;
  currentUser: CurrentUser;
}

interface DispatchProps {
  skipOnboarding: () => void;
}

interface OwnProps {
  children?: React.ReactNode;
}

type Props = StateProps & DispatchProps & OwnProps;

enum ModalKey {
  license,
  onboarding
}

interface State {
  modal?: ModalKey;
}

const LICENSE_PROMPT = 'sonarqube.license.prompt';

export class StartupModal extends React.PureComponent<Props, State> {
  static childContextTypes = {
    closeOnboardingTutorial: PropTypes.func,
    openOnboardingTutorial: PropTypes.func
  };

  state: State = {};

  getChildContext() {
    return {
      closeOnboardingTutorial: this.closeOnboarding,
      openOnboardingTutorial: this.openOnboarding
    };
  }

  componentDidMount() {
    this.tryAutoOpenLicense().catch(this.tryAutoOpenOnboarding);
  }

  closeOnboarding = () => {
    this.setState(state => ({
      modal: state.modal === ModalKey.onboarding ? undefined : state.modal
    }));
    this.props.skipOnboarding();
  };

  closeLicense = () => {
    this.setState(state => ({
      modal: state.modal === ModalKey.license ? undefined : state.modal
    }));
  };

  openOnboarding = () => {
    this.setState({ modal: ModalKey.onboarding });
  };

  tryAutoOpenLicense = () => {
    const { canAdmin, currentEdition, currentUser } = this.props;
    const hasLicenseManager = hasMessage('license.prompt.title');
    if (
      currentEdition !== 'community' &&
      isLoggedIn(currentUser) &&
      canAdmin &&
      hasLicenseManager
    ) {
      const lastPrompt = get(LICENSE_PROMPT, currentUser.login);
      if (!lastPrompt || differenceInDays(new Date(), parseDate(lastPrompt)) >= 1) {
        return showLicense().then(license => {
          if (!license || license.edition !== currentEdition) {
            save(LICENSE_PROMPT, toShortNotSoISOString(new Date()), currentUser.login);
            this.setState({ modal: ModalKey.license });
            return Promise.resolve();
          }
          return Promise.reject('License exists');
        });
      }
    }
    return Promise.reject('No license prompt');
  };

  tryAutoOpenOnboarding = () => {
    if (this.props.currentUser.showOnboardingTutorial) {
      this.openOnboarding();
    }
  };

  render() {
    const { modal } = this.state;
    return (
      <>
        {this.props.children}
        {modal === ModalKey.license && <LicensePromptModal onClose={this.closeLicense} />}
        {modal === ModalKey.onboarding && <OnboardingModal onFinish={this.closeOnboarding} />}
      </>
    );
  }
}

const mapStateToProps = (state: any): StateProps => ({
  canAdmin: getAppState(state).canAdmin,
  currentEdition: getAppState(state).edition,
  currentUser: getCurrentUser(state)
});

const mapDispatchToProps: DispatchProps = { skipOnboarding };

export default connect<StateProps, DispatchProps, OwnProps>(mapStateToProps, mapDispatchToProps)(
  StartupModal
);
