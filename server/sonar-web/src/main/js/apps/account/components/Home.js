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

import Favorites from './Favorites';
import FavoriteIssueFilters from './FavoriteIssueFilters';
import FavoriteMeasureFilters from './FavoriteMeasureFilters';
import { translate } from '../../../helpers/l10n';

const Home = ({ user, favorites, issueFilters, measureFilters }) => (
    <div className="page page-limited">
      <div className="columns">
        <div className="column-third">
          <Favorites favorites={favorites}/>
          {issueFilters && <FavoriteIssueFilters issueFilters={issueFilters}/>}
          {measureFilters && <FavoriteMeasureFilters measureFilters={measureFilters}/>}
        </div>

        <div className="column-third">
          <section>
            <h2 className="spacer-bottom">{translate('issues.page')}</h2>
            <p>Some cool issue widgets go here...</p>
          </section>
        </div>

        <div className="column-third">
          <section>
            <h2 className="spacer-bottom">{translate('my_profile.groups')}</h2>
            <ul id="groups">
              {user.groups.map(group => (
                  <li
                      key={group}
                      className="little-spacer-bottom text-ellipsis"
                      title={group}>
                    {group}
                  </li>
              ))}
            </ul>
          </section>

          <section className="huge-spacer-top">
            <h2 className="spacer-bottom">{translate('my_profile.scm_accounts')}</h2>
            <ul id="scm-accounts">
              {user.scmAccounts.map(scmAccount => (
                  <li
                      key={scmAccount}
                      className="little-spacer-bottom text-ellipsis"
                      title={scmAccount}>
                    {scmAccount}
                  </li>
              ))}
            </ul>
          </section>
        </div>
      </div>
    </div>
);

export default Home;
