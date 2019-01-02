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
import { Link } from 'react-router';
import Workers from './Workers';
import { translate } from '../../../helpers/l10n';

interface Props {
  component?: any;
}

export default function Header(props: Props) {
  return (
    <header className="page-header">
      <h1 className="page-title">{translate('background_tasks.page')}</h1>
      {!props.component && (
        <div className="page-actions">
          <Workers />
        </div>
      )}
      <p className="page-description">
        {translate('background_tasks.page.description')}
        <Link
          className="spacer-left"
          target="_blank"
          to={{ pathname: '/documentation/analysis/background-tasks/' }}>
          {translate('learn_more')}
        </Link>
      </p>
    </header>
  );
}
