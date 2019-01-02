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
import { translateWithParameters } from '../../../helpers/l10n';

interface Props {
  name: string;
  value: string | null;
}

export default function ParameterChange({ name, value }: Props) {
  return (
    <div className="quality-profile-changelog-parameter">
      {value == null
        ? translateWithParameters(
            'quality_profiles.changelog.parameter_reset_to_default_value',
            name
          )
        : translateWithParameters('quality_profiles.parameter_set_to', name, value)}
    </div>
  );
}
