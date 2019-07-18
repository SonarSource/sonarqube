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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import OnboardingAddMembersIcon from 'sonar-ui-common/components/icons/OnboardingAddMembersIcon';
import OnboardingProjectIcon from 'sonar-ui-common/components/icons/OnboardingProjectIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { OnboardingContextShape } from '../../../app/components/OnboardingContext';
import { Router, withRouter } from '../../../components/hoc/withRouter';
import '../../tutorials/styles.css';
import './OrganizationEmpty.css';

interface Props {
  openProjectOnboarding: OnboardingContextShape;
  organization: T.Organization;
  router: Pick<Router, 'push'>;
}

export class OrganizationEmpty extends React.PureComponent<Props> {
  handleNewProjectClick = () => {
    this.props.openProjectOnboarding(this.props.organization);
  };

  handleAddMembersClick = () => {
    const { organization } = this.props;
    this.props.router.push(`/organizations/${organization.key}/members`);
  };

  render() {
    const { organization } = this.props;
    const memberSyncActivated = organization.alm && organization.alm.membersSync;

    return (
      <div className="organization-empty">
        <h3 className="text-center">{translate('onboarding.create_organization.ready')}</h3>
        <div className="display-flex-space-around huge-spacer-top">
          <Button className="button-huge" onClick={this.handleNewProjectClick}>
            <OnboardingProjectIcon className="big-spacer-bottom" />
            <p className="medium spacer-top">{translate('provisioning.analyze_new_project')}</p>
          </Button>
          {!memberSyncActivated && (
            <Button className="button-huge" onClick={this.handleAddMembersClick}>
              <OnboardingAddMembersIcon />
              <p className="medium spacer-top">{translate('organization.members.add.multiple')}</p>
            </Button>
          )}
        </div>
      </div>
    );
  }
}

export default withRouter(OrganizationEmpty);
