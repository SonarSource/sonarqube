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
import { FlagMessage, Link } from 'design-system';
import * as React from 'react';
import { useDocUrl } from '../../helpers/docs';
import { translate } from '../../helpers/l10n';

export default function NewCodeDefinitionAnalysisWarning() {
  const toStatic = useDocUrl(
    '/project-administration/clean-as-you-code-settings/defining-new-code/',
  );
  return (
    <FlagMessage variant="warning" className="sw-mb-4 sw-max-w-[800px]">
      <div>
        <p className="sw-mb-2 sw-font-bold">
          {translate('baseline.specific_analysis.compliance_warning.title')}
        </p>
        <p className="sw-mb-2">
          {translate('baseline.specific_analysis.compliance_warning.explanation')}
        </p>
        <p>
          {translate('learn_more')}:
          <Link className="sw-ml-2" to={toStatic}>
            {translate('learn_more')}
          </Link>
        </p>
      </div>
    </FlagMessage>
  );
}
