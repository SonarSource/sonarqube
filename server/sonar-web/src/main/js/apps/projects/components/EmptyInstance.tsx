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
import { translate } from '../../../helpers/l10n';
import { Button } from '../../../components/ui/buttons';
import { isSonarCloud } from '../../../helpers/system';
import { hasGlobalPermission, isLoggedIn } from '../../../helpers/users';
import { OnboardingContextShape } from '../../../app/components/OnboardingContext';

interface Props {
  currentUser: T.CurrentUser;
  openProjectOnboarding: OnboardingContextShape;
  organization?: T.Organization;
}

export default class EmptyInstance extends React.PureComponent<Props> {
  analyzeNewProject = () => {
    this.props.openProjectOnboarding(this.props.organization);
  };

  render() {
    const { currentUser, organization } = this.props;
    const showNewProjectButton = isSonarCloud()
      ? organization && organization.actions && organization.actions.provision
      : isLoggedIn(currentUser) && hasGlobalPermission(currentUser, 'provisioning');

    return (
      <div className="projects-empty-list">
        <h3>
          {showNewProjectButton
            ? translate('projects.no_projects.empty_instance.new_project')
            : translate('projects.no_projects.empty_instance')}
        </h3>
        {showNewProjectButton && (
          <div>
            <p className="big-spacer-top">
              {translate('projects.no_projects.empty_instance.how_to_add_projects')}
            </p>
            <p className="big-spacer-top">
              <Button onClick={this.analyzeNewProject}>
                {translate('my_account.create_new.TRK')}
              </Button>
            </p>
          </div>
        )}
      </div>
    );
  }
}
