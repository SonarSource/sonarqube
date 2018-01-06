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
// @flow
import React from 'react';
import ListFooter from '../../../components/controls/ListFooter';
/*:: import type { Paging } from '../types'; */

/*::
type Props = {
  analyses: Array<*>,
  fetchMoreActivity: () => void,
  paging?: Paging
};
*/

export default function ProjectActivityPageFooter(
  { analyses, fetchMoreActivity, paging } /*: Props */
) {
  if (!paging || analyses.length === 0) {
    return null;
  }
  return <ListFooter count={analyses.length} total={paging.total} loadMore={fetchMoreActivity} />;
}
