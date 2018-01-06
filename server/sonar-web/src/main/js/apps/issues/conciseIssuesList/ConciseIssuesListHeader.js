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
import BackButton from './BackButton';
import ReloadButton from '../components/ReloadButton';
import IssuesCounter from '../components/IssuesCounter';
/*:: import type { Paging } from '../utils'; */

/*::
type Props = {|
  loading: boolean,
  onBackClick: () => void,
  onReload: () => void,
  paging?: Paging,
  selectedIndex: ?number
|};
*/

export default function ConciseIssuesListHeader(props /*: Props */) {
  const { paging, selectedIndex } = props;

  return (
    <header className="layout-page-header-panel concise-issues-list-header">
      <div className="layout-page-header-panel-inner concise-issues-list-header-inner">
        <BackButton className="pull-left" onClick={props.onBackClick} />
        {props.loading ? (
          <i className="spinner pull-right" />
        ) : (
          <ReloadButton className="pull-right" onClick={props.onReload} />
        )}
        {paging != null && <IssuesCounter current={selectedIndex} total={paging.total} />}
      </div>
    </header>
  );
}
