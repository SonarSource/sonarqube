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

import { TextSubdued } from 'design-system';
import * as React from 'react';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { NewCodeDefinition, NewCodeDefinitionType } from '../../types/new-code-definition';

interface Props {
  globalNcd: NewCodeDefinition;
}

export default function GlobalNewCodeDefinitionDescription({ globalNcd }: Readonly<Props>) {
  let setting: string;
  let description: string;
  let useCase: string;

  if (globalNcd.type === NewCodeDefinitionType.NumberOfDays) {
    setting = `${translate('new_code_definition.number_days')} (${translateWithParameters(
      'duration.days',
      globalNcd.value ?? '?',
    )})`;

    description = translate('new_code_definition.number_days.description');
    useCase = translate('new_code_definition.number_days.usecase');
  } else {
    setting = translate('new_code_definition.previous_version');
    description = translate('new_code_definition.previous_version.description');
    useCase = translate('new_code_definition.previous_version.usecase');
  }

  return (
    <div className="sw-flex sw-flex-col sw-gap-2 sw-max-w-[800px]">
      <TextSubdued>
        <strong className="sw-font-bold">{setting}</strong>
      </TextSubdued>

      <TextSubdued>
        <span>{description}</span>
      </TextSubdued>

      <TextSubdued>
        <span>{useCase}</span>
      </TextSubdued>
    </div>
  );
}
