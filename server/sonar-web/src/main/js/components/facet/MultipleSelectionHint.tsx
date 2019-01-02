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
import { translate } from '../../helpers/l10n';
import './MultipleSelectionHint.css';

interface Props {
  options: number;
  values: number;
}

export default function MultipleSelectionHint({ options, values }: Props) {
  // do not render if nothing is selected or there are less than 2 possible options
  if (values === 0 || options < 2) {
    return null;
  }

  return (
    <div className="multiple-selection-hint">
      <div className="multiple-selection-hint-inner">
        {translate(
          isOnMac()
            ? 'shortcuts.section.global.facets.multiselection.mac'
            : 'shortcuts.section.global.facets.multiselection'
        )}
      </div>
    </div>
  );
}

function isOnMac() {
  return navigator.userAgent.indexOf('Mac OS X') !== -1;
}
