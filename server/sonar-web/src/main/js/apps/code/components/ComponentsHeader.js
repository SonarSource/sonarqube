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
import React from 'react';

import { translate } from '../../../helpers/l10n';


const ComponentsHeader = ({ baseComponent }) => (
    <thead>
      <tr>
        <th className="thin nowrap">&nbsp;</th>
        <th>&nbsp;</th>
        <th className="thin nowrap text-right code-components-cell">
          {baseComponent && translate('metric.ncloc.name')}
        </th>
        <th className="thin nowrap text-right code-components-cell">
          {baseComponent && translate('metric.code_smells.name')}
        </th>
        <th className="thin nowrap text-right code-components-cell">
          {baseComponent && translate('metric.bugs.name')}
        </th>
        <th className="thin nowrap text-right code-components-cell">
          {baseComponent && translate('metric.vulnerabilities.name')}
        </th>
        <th className="thin nowrap text-right code-components-cell">
          {baseComponent && translate('metric.coverage.name')}
        </th>
        <th className="thin nowrap text-right code-components-cell">
          {baseComponent && translate('metric.duplicated_lines_density.short_name')}
        </th>
      </tr>
    </thead>
);


export default ComponentsHeader;
