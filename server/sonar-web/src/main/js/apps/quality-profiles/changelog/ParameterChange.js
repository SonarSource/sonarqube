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
import { translateWithParameters } from '../../../helpers/l10n';

/*::
type Props = {
  name: string,
  value: ?string
};
*/

export default class ParameterChange extends React.PureComponent {
  /*:: props: Props; */

  render() {
    const { name, value } = this.props;

    if (value == null) {
      return (
        <div style={{ whiteSpace: 'normal' }}>
          {translateWithParameters(
            'quality_profiles.changelog.parameter_reset_to_default_value',
            name
          )}
        </div>
      );
    }

    return (
      <div style={{ whiteSpace: 'normal' }}>
        {translateWithParameters('quality_profiles.parameter_set_to', name, value)}
      </div>
    );
  }
}
