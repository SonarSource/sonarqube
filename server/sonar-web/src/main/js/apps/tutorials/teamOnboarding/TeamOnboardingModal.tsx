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
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import Modal from '../../../components/controls/Modal';
import { translate } from '../../../helpers/l10n';
import { ResetButtonLink } from '../../../components/ui/buttons';
import { Alert } from '../../../components/ui/Alert';

interface Props {
  onFinish: () => void;
}

export default class TeamOnboardingModal extends React.PureComponent<Props> {
  render() {
    const header = translate('onboarding.team.header');
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
          <Alert variant="info">{translate('onboarding.team.work_in_progress')}</Alert>
          <p className="spacer-top big-spacer-bottom">{translate('onboarding.team.first_step')}</p>
          <p className="spacer-top big-spacer-bottom">
            <FormattedMessage
              defaultMessage={translate('onboarding.team.how_to_join')}
              id="onboarding.team.how_to_join"
              values={{
                link: (
                  <Link
                    onClick={this.props.onFinish}
                    to="/documentation/organizations/manage-team/">
                    {translate('as_explained_here')}
                  </Link>
                )
              }}
            />
          </p>
        </div>
        <footer className="modal-foot">
          <ResetButtonLink onClick={this.props.onFinish}>{translate('close')}</ResetButtonLink>
        </footer>
      </Modal>
    );
  }
}
