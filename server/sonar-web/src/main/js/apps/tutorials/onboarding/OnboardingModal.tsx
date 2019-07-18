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
import { Button, ResetButtonLink } from 'sonar-ui-common/components/controls/buttons';
import Modal from 'sonar-ui-common/components/controls/Modal';
import OnboardingProjectIcon from 'sonar-ui-common/components/icons/OnboardingProjectIcon';
import OnboardingTeamIcon from 'sonar-ui-common/components/icons/OnboardingTeamIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { whenLoggedIn } from '../../../components/hoc/whenLoggedIn';
import { withUserOrganizations } from '../../../components/hoc/withUserOrganizations';
import '../styles.css';
import OrganizationsShortList from './OrganizationsShortList';

export interface Props {
  onClose: VoidFunction;
  onOpenProjectOnboarding: VoidFunction;
  userOrganizations: T.Organization[];
}

export function OnboardingModal(props: Props) {
  const { onClose, onOpenProjectOnboarding, userOrganizations } = props;

  const header = translate('onboarding.header');
  return (
    <Modal
      contentLabel={header}
      onRequestClose={onClose}
      shouldCloseOnOverlayClick={false}
      size={userOrganizations.length > 0 ? 'medium' : 'small'}>
      <div className="modal-head">
        <h2>{translate('onboarding.header')}</h2>
        <p className="spacer-top">{translate('onboarding.header.description')}</p>
      </div>
      <div className="modal-body text-center display-flex-row huge-spacer-top huge-spacer-bottom">
        <div className="flex-1">
          <OnboardingProjectIcon className="big-spacer-bottom" />
          <h3 className="big-spacer-bottom">{translate('onboarding.analyze_your_code')}</h3>
          <Button onClick={onOpenProjectOnboarding}>
            {translate('onboarding.project.create')}
          </Button>
        </div>
        {userOrganizations.length > 0 && (
          <>
            <div className="vertical-pipe-separator">
              <div className="vertical-separator" />
            </div>
            <div className="flex-1">
              <OnboardingTeamIcon className="big-spacer-bottom" />
              <h3 className="big-spacer-bottom">
                {translate('onboarding.browse_your_organizations')}
              </h3>
              <OrganizationsShortList onClick={onClose} organizations={userOrganizations} />
            </div>
          </>
        )}
      </div>
      <div className="modal-foot text-right">
        <ResetButtonLink onClick={onClose}>{translate('not_now')}</ResetButtonLink>
      </div>
    </Modal>
  );
}

export default withUserOrganizations(whenLoggedIn(OnboardingModal));
