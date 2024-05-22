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
import { Note, SubHeading } from 'design-system';
import * as React from 'react';
import Tooltip from '../../../components/controls/Tooltip';
import { translateWithParameters } from '../../../helpers/l10n';
import { sanitizeStringRestricted } from '../../../helpers/sanitize';
import { ExtendedSettingDefinition } from '../../../types/settings';
import { getPropertyDescription, getPropertyName } from '../utils';

interface Props {
  definition: ExtendedSettingDefinition;
}

export default function DefinitionDescription({ definition }: Readonly<Props>) {
  const propertyName = getPropertyName(definition);
  const description = getPropertyDescription(definition);

  return (
    <div className="sw-w-abs-300">
      <SubHeading title={propertyName}>{propertyName}</SubHeading>

      {description && (
        <div
          className="markdown sw-mt-1"
          // eslint-disable-next-line react/no-danger
          dangerouslySetInnerHTML={{ __html: sanitizeStringRestricted(description) }}
        />
      )}

      <Tooltip content={translateWithParameters('settings.key_x', definition.key)}>
        <Note as="div" className="sw-mt-4">
          {translateWithParameters('settings.key_x', definition.key)}
        </Note>
      </Tooltip>
    </div>
  );
}
