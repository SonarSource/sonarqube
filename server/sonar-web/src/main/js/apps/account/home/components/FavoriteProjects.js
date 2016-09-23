/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React from 'react';
import { connect } from 'react-redux';
import sortBy from 'lodash/sortBy';
import Favorite from '../../../../components/controls/Favorite';
import Level from '../../../../components/ui/Level';
import { TooltipsContainer } from '../../../../components/mixins/tooltips-mixin';
import { getFavorites, getComponentMeasure } from '../../../../app/store/rootReducer';
import { getComponentUrl, getProjectsUrl } from '../../../../helpers/urls';
import { fetchFavoriteProjects } from '../store/actions';
import { translate } from '../../../../helpers/l10n';

class FavoriteProjects extends React.Component {
  static propTypes = {
    favorites: React.PropTypes.array,
    fetchFavoriteProjects: React.PropTypes.func.isRequired
  };

  componentDidMount () {
    this.props.fetchFavoriteProjects();
  }

  renderList () {
    const { favorites } = this.props;

    if (!favorites) {
      return null;
    }

    if (favorites.length === 0) {
      return (
          <div id="no-favorite-projects" className="boxed-group boxed-group-inner markdown text-center">
            <p className="note">{translate('my_activity.no_favorite_projects')}</p>
            <p>{translate('my_activity.no_favorite_projects.engagement')}</p>
          </div>
      );
    }

    const sorted = sortBy(favorites, project => project.name.toLowerCase());

    return (
        <ul id="favorite-projects">
          {sorted.map(project => (
              <li key={project.key}>
                <div className="pull-left" style={{ padding: '15px 15px 15px 10px' }}>
                  <Favorite favorite={true} component={project.key}/>
                </div>

                <a href={getComponentUrl(project.key)}>
                  {project.qualityGate != null && (
                      <span className="pull-right">
                        <Level level={project.qualityGate}/>
                      </span>
                  )}
                  <strong>{project.name}</strong>
                </a>
              </li>
          ))}
        </ul>
    );
  }

  renderQualityGateTitle () {
    const { favorites } = this.props;

    const shouldBeRendered = favorites != null && favorites.some(f => f.qualityGate != null);

    if (!shouldBeRendered) {
      return null;
    }

    return (
        <TooltipsContainer>
          <div className="pull-right note">
            {translate('overview.quality_gate')}
            <i className="little-spacer-left icon-help"
               title={translate('quality_gates.intro.1')}
               data-toggle="tooltip"/>
          </div>
        </TooltipsContainer>
    );
  }

  render () {
    const { favorites } = this.props;

    return (
        <div className="my-activity-projects">
          <div className="my-activity-projects-header">
            {this.renderQualityGateTitle()}
            <h2>{translate('my_activity.my_favorite_projects')}</h2>
          </div>

          {favorites == null && (
              <div className="text-center">
                <i className="spinner"/>
              </div>
          )}

          {this.renderList()}

          <div className="more">
            <a className="button" href={getProjectsUrl()}>
              {translate('my_activity.explore_projects')}
            </a>
          </div>
        </div>
    );
  }
}

const mapStateToProps = state => {
  const fromState = getFavorites(state);
  const favorites = fromState == null ? null : fromState
      .filter(component => component.qualifier === 'TRK')
      .map(component => ({
        ...component,
        qualityGate: getComponentMeasure(state, component.key, 'alert_status')
      }));

  return { favorites };
};

export default connect(
    mapStateToProps,
    { fetchFavoriteProjects }
)(FavoriteProjects);
