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
import State from '../../../components/navigator/models/state';

export default State.extend({
  defaults: {
    page: 1,
    maxResultsReached: false,
    query: {},
    facets: ['languages', 'tags'],
    allFacets: [
      'q', 'rule_key', 'languages', 'tags', 'repositories', 'debt_characteristics', 'severities',
      'statuses', 'available_since', 'is_template', 'qprofile', 'inheritance', 'active_severities'
    ],
    facetsFromServer: [
      'languages', 'repositories', 'tags', 'severities', 'statuses', 'debt_characteristics',
      'active_severities'
    ],
    transform: {
      'has_debt_characteristic': 'debt_characteristics'
    }
  }
});
