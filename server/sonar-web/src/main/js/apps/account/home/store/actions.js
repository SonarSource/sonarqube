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
import { getIssuesCount } from '../../../../api/issues';
import { getFavorites } from '../../../../api/favorites';
import { receiveFavorites } from '../../../../app/store/favorites/actions';
import { getMeasures } from '../../../../api/measures';
import { receiveComponentMeasure } from '../../../../app/store/measures/actions';

export const RECEIVE_ISSUES_ACTIVITY = 'myActivity/RECEIVE_ISSUES_ACTIVITY';

const receiveIssuesActivity = (recent, all) => ({
  type: RECEIVE_ISSUES_ACTIVITY,
  recent,
  all
});

export const fetchIssuesActivity = () => dispatch => {
  const query = { resolved: 'false', assignees: '__me__' };
  Promise.all([
    getIssuesCount(query),
    getIssuesCount({ ...query, createdInLast: '1w' })
  ]).then(responses => dispatch(receiveIssuesActivity(responses[1].issues, responses[0].issues)));
};

export const fetchFavoriteProjects = () => dispatch => {
  getFavorites().then(favorites => {
    dispatch(receiveFavorites(favorites));

    const projects = favorites.filter(component => component.qualifier === 'TRK');
    Promise.all(projects.map(project => getMeasures(project.key, ['alert_status'])))
        .then(responses => {
          responses.forEach((measures, index) => {
            measures.forEach(measure => {
              dispatch(receiveComponentMeasure(projects[index].key, measure.metric, measure.value));
            });
          });
        });
  });
};
