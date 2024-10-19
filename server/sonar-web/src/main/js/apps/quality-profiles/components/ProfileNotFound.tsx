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
import { Link } from 'design-system';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { PROFILE_PATH } from '../constants';

export default function ProfileNotFound() {
  const intl = useIntl();

  return (
    <div className="sw-text-center sw-mt-4">
      <h1 className="sw-typo-lg-semibold sw-mb-4">
        {intl.formatMessage({ id: 'quality_profiles.not_found' })}
      </h1>
      <Link className="sw-typo-semibold" to={PROFILE_PATH}>
        {intl.formatMessage({ id: 'quality_profiles.back_to_list' })}
      </Link>
    </div>
  );
}
