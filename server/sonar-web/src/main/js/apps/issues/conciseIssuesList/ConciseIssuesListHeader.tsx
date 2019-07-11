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
import BackButton from 'sonar-ui-common/components/controls/BackButton';
import ReloadButton from 'sonar-ui-common/components/controls/ReloadButton';
import IssuesCounter from '../components/IssuesCounter';

interface Props {
  displayBackButton?: boolean;
  loading: boolean;
  onBackClick: () => void;
  onReload: () => void;
  paging: T.Paging | undefined;
  selectedIndex: number | undefined;
}

export default function ConciseIssuesListHeader(props: Props) {
  const { displayBackButton = true, paging, selectedIndex } = props;

  return (
    <header className="layout-page-header-panel concise-issues-list-header">
      <div className="layout-page-header-panel-inner concise-issues-list-header-inner">
        {displayBackButton && (
          <BackButton className="pull-left" disabled={props.loading} onClick={props.onBackClick} />
        )}
        {props.loading ? (
          <i className="spinner pull-right" />
        ) : (
          <ReloadButton className="pull-right" onClick={props.onReload} />
        )}
        {paging && <IssuesCounter current={selectedIndex} total={paging.total} />}
      </div>
    </header>
  );
}
