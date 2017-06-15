/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import SearchFilterContainer from '../filters/SearchFilterContainer';
import { translate } from '../../../helpers/l10n';

type Props = {
  isFavorite?: boolean,
  loading: boolean,
  onOpenOptionBar: () => void,
  optionBarOpen?: boolean,
  organization?: { key: string },
  query: { [string]: string },
  total?: number
};

export default function PageHeader(props: Props) {
  return (
    <header className="page-header">
      <SearchFilterContainer
        isFavorite={props.isFavorite}
        organization={props.organization}
        query={props.query}
      />
      <div className="page-actions projects-page-actions text-right">
        {!props.optionBarOpen &&
          <a
            className="button js-projects-topbar-open spacer-right"
            href="#"
            onClick={props.onOpenOptionBar}>
            {translate('projects.view_settings')}
          </a>}

        {!!props.loading && <i className="spinner spacer-right" />}

        {props.total != null &&
          <span>
            <strong id="projects-total">{props.total}</strong>
            {' '}
            {translate('projects._projects')}
          </span>}
      </div>
    </header>
  );
}
