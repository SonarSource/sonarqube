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
import { connect } from 'react-redux';
import handleRequiredAuthentication from '../../app/utils/handleRequiredAuthentication';
import Modal from '../../components/controls/Modal';
import { ResetButtonLink, Button } from '../../components/ui/buttons';
import { translate } from '../../helpers/l10n';
import { CurrentUser, isLoggedIn } from '../../app/types';
import { getCurrentUser } from '../../store/rootReducer';
import './styles.css';

interface OwnProps {
  onFinish: () => void;
  onOpenOrganizationOnboarding: () => void;
  onOpenProjectOnboarding: () => void;
  onOpenTeamOnboarding: () => void;
}

interface StateProps {
  currentUser: CurrentUser;
}

type Props = OwnProps & StateProps;

export class Onboarding extends React.PureComponent<Props> {
  componentDidMount() {
    if (!isLoggedIn(this.props.currentUser)) {
      handleRequiredAuthentication();
    }
  }

  render() {
    if (!isLoggedIn(this.props.currentUser)) {
      return null;
    }

    const header = translate('onboarding.header');
    return (
      <Modal
        contentLabel={header}
        medium={true}
        onRequestClose={this.props.onFinish}
        shouldCloseOnOverlayClick={false}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>
        <div className="modal-body">
          <p className="spacer-top big-spacer-bottom">
            {translate('onboarding.header.description')}
          </p>
          <ul className="onboarding-choices">
            <li className="text-center">
              <p className="big-spacer-bottom">{translate('onboarding.analyze_public_code')}</p>
              <Button onClick={this.props.onOpenProjectOnboarding}>
                {translate('onboarding.analyze_public_code.button')}
              </Button>
            </li>
            <li className="text-center">
              <p className="big-spacer-bottom">{translate('onboarding.analyze_private_code')}</p>
              <Button onClick={this.props.onOpenOrganizationOnboarding}>
                {translate('onboarding.analyze_private_code.button')}
              </Button>
            </li>
            <li className="text-center">
              <p className="big-spacer-bottom">
                {translate('onboarding.contribute_existing_project')}
              </p>
              <Button onClick={this.props.onOpenTeamOnboarding}>
                {translate('onboarding.contribute_existing_project.button')}
              </Button>
            </li>
          </ul>
        </div>
        <footer className="modal-foot">
          <ResetButtonLink onClick={this.props.onFinish}>{translate('close')}</ResetButtonLink>
        </footer>
      </Modal>
    );
  }
}

const mapStateToProps = (state: any): StateProps => ({ currentUser: getCurrentUser(state) });

export default connect<StateProps, {}, OwnProps>(mapStateToProps)(Onboarding);
