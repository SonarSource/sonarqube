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
import { translate } from '../../../helpers/l10n';

/*::
type Props = { onTutorialSelect: () => void };
*/

export default function TutorialsHelp({ onTutorialSelect } /*: Props */) {
  const handleClick = (event /*: Event */) => {
    event.preventDefault();
    onTutorialSelect();
  };

  return (
    <div>
      <h2 className="spacer-top spacer-bottom">{translate('help.section.tutorials')}</h2>
      <a href="#" onClick={handleClick}>
        {translate('tutorials.onboarding')}
      </a>
    </div>
  );
}
