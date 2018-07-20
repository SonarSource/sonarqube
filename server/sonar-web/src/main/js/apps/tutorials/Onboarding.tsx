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
import handleRequiredAuthentication from '../../app/utils/handleRequiredAuthentication';
import Modal from '../../components/controls/Modal';
import OnboardingPrivateIcon from '../../components/icons-components/OnboardingPrivateIcon';
import OnboardingProjectIcon from '../../components/icons-components/OnboardingProjectIcon';
import OnboardingTeamIcon from '../../components/icons-components/OnboardingTeamIcon';
import { Button, ResetButtonLink } from '../../components/ui/buttons';
import { translate } from '../../helpers/l10n';
import { CurrentUser, isLoggedIn } from '../../app/types';
import { getCurrentUser } from '../../store/rootReducer';
import './styles.css';

interface OwnProps {
  onClose: (doSkipOnboarding?: boolean) => void;
  onOpenOrganizationOnboarding: () => void;
  onOpenTeamOnboarding: () => void;
}

interface StateProps {
  currentUser: CurrentUser;
}

type Props = OwnProps & StateProps;

export class Onboarding extends React.PureComponent<Props> {
  static contextTypes = {
    router: PropTypes.object
  };

  componentDidMount() {
    if (!isLoggedIn(this.props.currentUser)) {
      handleRequiredAuthentication();
    }
  }

  openProjectOnboarding = () => {
    this.props.onClose(false);
    this.context.router.push('/onboarding');
  };

  onFinish = () => {
    this.props.onClose(true);
  };

  render() {
    if (!isLoggedIn(this.props.currentUser)) {
      return null;
    }

    const header = translate('onboarding.header');
    return (
      <Modal
        contentLabel={header}
        medium={true}
        onRequestClose={this.onFinish}
        shouldCloseOnOverlayClick={false}>
        <div className="modal-simple-head text-center">
          <h1>{translate('onboarding.header')}</h1>
          <p className="spacer-top">{translate('onboarding.header.description')}</p>
        </div>
        <div className="modal-simple-body text-center onboarding-choices">
          <Button className="onboarding-choice" onClick={this.openProjectOnboarding}>
            <OnboardingProjectIcon />
            <span>{translate('onboarding.analyze_public_code')}</span>
            <p className="note">{translate('onboarding.analyze_public_code.note')}</p>
          </Button>
          <Button className="onboarding-choice" onClick={this.props.onOpenOrganizationOnboarding}>
            <OnboardingPrivateIcon />
            <span>{translate('onboarding.analyze_private_code')}</span>
            <p className="note">{translate('onboarding.analyze_private_code.note')}</p>
          </Button>
          <Button className="onboarding-choice" onClick={this.props.onOpenTeamOnboarding}>
            <OnboardingTeamIcon />
            <span>{translate('onboarding.contribute_existing_project')}</span>
            <p className="note">{translate('onboarding.contribute_existing_project.note')}</p>
          </Button>
        </div>
        <div className="modal-simple-footer text-center">
          <ResetButtonLink className="spacer-bottom" onClick={this.onFinish}>
            {translate('not_now')}
          </ResetButtonLink>
          <p className="note">{translate('onboarding.footer')}</p>
        </div>
      </Modal>
    );
  }
}

const mapStateToProps = (state: any): StateProps => ({ currentUser: getCurrentUser(state) });

export default connect<StateProps, {}, OwnProps>(mapStateToProps)(Onboarding);
