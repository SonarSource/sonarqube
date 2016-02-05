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

import { translate } from '../../../helpers/l10n';

const FavoriteMeasureFilters = ({ measureFilters }) => (
    <section className="huge-spacer-top">
      <h2 className="spacer-bottom">
        {translate('my_account.favorite_measure_filters')}
      </h2>

      {!measureFilters.length && (
          <p className="note">
            {translate('my_account.no_favorite_measure_filters')}
          </p>
      )}

      <table id="favorite-measure-filters" className="data">
        <tbody>
          {measureFilters.map(f => (
              <tr key={f.name}>
                <td className="thin">
                  <i className="icon-favorite"/>
                </td>
                <td>
                  <a href={`${window.baseUrl}/measures/filter/${f.id}`}>
                    {f.name}
                  </a>
                </td>
              </tr>
          ))}
        </tbody>
      </table>

      <div className="spacer-top small">
        <a href={`${window.baseUrl}/measures/manage`}>{translate('see_all')}</a>
      </div>

    </section>
);

export default FavoriteMeasureFilters;
