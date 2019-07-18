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
import { sortBy } from 'lodash';
import * as React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import { Button } from 'sonar-ui-common/components/controls/buttons';
import Dropdown from 'sonar-ui-common/components/controls/Dropdown';
import DropdownIcon from 'sonar-ui-common/components/icons/DropdownIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { OnboardingContextShape } from '../../../app/components/OnboardingContext';
import OrganizationListItem from '../../../components/ui/OrganizationListItem';
import { isSonarCloud } from '../../../helpers/system';
import { getMyOrganizations, Store } from '../../../store/rootReducer';

interface OwnProps {
  openProjectOnboarding: OnboardingContextShape;
}

interface StateProps {
  organizations: T.Organization[];
}

export function NoFavoriteProjects(props: StateProps & OwnProps) {
  return (
    <div className="projects-empty-list">
      <h3>{translate('projects.no_favorite_projects')}</h3>
      {isSonarCloud() ? (
        <div className="spacer-top">
          <p>{translate('projects.no_favorite_projects.how_to_add_projects')}</p>
          <div className="huge-spacer-top">
            <Button onClick={props.openProjectOnboarding}>
              {translate('provisioning.analyze_new_project')}
            </Button>

            {props.organizations.length > 0 && (
              <Dropdown
                className="display-inline-block big-spacer-left"
                overlay={
                  <ul className="menu">
                    {sortBy(props.organizations, org => org.name.toLowerCase()).map(
                      organization => (
                        <OrganizationListItem key={organization.key} organization={organization} />
                      )
                    )}
                  </ul>
                }>
                <a className="button" href="#">
                  {translate('projects.no_favorite_projects.favorite_projects_from_orgs')}
                  <DropdownIcon className="little-spacer-left" />
                </a>
              </Dropdown>
            )}

            <Link className="button big-spacer-left" to="/explore/projects">
              {translate('projects.no_favorite_projects.favorite_public_projects')}
            </Link>
          </div>
        </div>
      ) : (
        <div>
          <p className="big-spacer-top">{translate('projects.no_favorite_projects.engagement')}</p>
          <p className="big-spacer-top">
            <Link className="button" to="/projects/all">
              {translate('projects.explore_projects')}
            </Link>
          </p>
        </div>
      )}
    </div>
  );
}

const mapStateToProps = (state: Store): StateProps => ({
  organizations: getMyOrganizations(state)
});

export default connect(mapStateToProps)(NoFavoriteProjects);
