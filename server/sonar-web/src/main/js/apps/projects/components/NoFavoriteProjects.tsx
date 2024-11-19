/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import { Link, Text, TextSize } from '@sonarsource/echoes-react';
import { translate } from '../../../helpers/l10n';

export default function NoFavoriteProjects() {
  return (
    <div className="sw-flex sw-flex-col sw-items-center sw-py-8">
      <Text isHighlighted size={TextSize.Large} className="sw-mb-2">
        {translate('projects.no_favorite_projects')}
      </Text>
      <p className="sw-mt-2 sw-typo-default">
        {translate('projects.no_favorite_projects.engagement')}
      </p>
      <p className="sw-mt-6">
        <Link className="sw-mt-6 sw-typo-semibold" to="/projects/all">
          {translate('projects.explore_projects')}
        </Link>
      </p>
    </div>
  );
}
