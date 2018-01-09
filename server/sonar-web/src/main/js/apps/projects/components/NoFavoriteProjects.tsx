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
import { Link } from 'react-router';
import * as classNames from 'classnames';
import { connect } from 'react-redux';
import * as PropTypes from 'prop-types';
import { sortBy } from 'lodash';
import { Organization } from '../../../app/types';
import DropdownIcon from '../../../components/icons-components/DropdownIcon';
import Dropdown from '../../../components/controls/Dropdown';
import { getMyOrganizations } from '../../../store/rootReducer';
import OrganizationListItem from '../../../components/ui/OrganizationListItem';
import { translate } from '../../../helpers/l10n';

interface StateProps {
  organizations: Organization[];
}

interface Props extends StateProps {
  onSonarCloud: boolean;
}

export class NoFavoriteProjects extends React.PureComponent<Props> {
  static contextTypes = {
    openOnboardingTutorial: PropTypes.func
  };

  onAnalyzeProjectClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.context.openOnboardingTutorial();
  };

  render() {
    const { onSonarCloud, organizations } = this.props;
    return (
      <div className="projects-empty-list">
        <h3>{translate('projects.no_favorite_projects')}</h3>
        {onSonarCloud ? (
          <div className="spacer-top">
            <p>{translate('projects.no_favorite_projects.how_to_add_projects')}</p>
            <div className="huge-spacer-top">
              <a className="button" href="#" onClick={this.onAnalyzeProjectClick}>
                {translate('my_account.analyze_new_project')}
              </a>
              <Dropdown>
                {({ onToggleClick, open }) => (
                  <div
                    className={classNames('display-inline-block', 'big-spacer-left', 'dropdown', {
                      open
                    })}>
                    <a className="button" href="#" onClick={onToggleClick}>
                      {translate('projects.no_favorite_projects.favorite_projects_from_orgs')}
                      <DropdownIcon className="little-spacer-left" />
                    </a>
                    <ul className="dropdown-menu">
                      {sortBy(organizations, org => org.name.toLowerCase()).map(organization => (
                        <OrganizationListItem key={organization.key} organization={organization} />
                      ))}
                    </ul>
                  </div>
                )}
              </Dropdown>
              <Link className="button big-spacer-left" to="/explore/projects">
                {translate('projects.no_favorite_projects.favorite_public_projects')}
              </Link>
            </div>
          </div>
        ) : (
          <div>
            <p className="big-spacer-top">
              {translate('projects.no_favorite_projects.engagement')}
            </p>
            <p className="big-spacer-top">
              <Link to="/projects/all" className="button">
                {translate('projects.explore_projects')}
              </Link>
            </p>
          </div>
        )}
      </div>
    );
  }
}

const mapStateToProps = (state: any): StateProps => ({
  organizations: getMyOrganizations(state)
});

export default connect(mapStateToProps)(NoFavoriteProjects);
