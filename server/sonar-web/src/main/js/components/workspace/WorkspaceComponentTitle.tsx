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

import { QualifierIcon } from '~design-system';
import { translate } from '../../helpers/l10n';
import { collapsePath } from '../../helpers/path';
import { isDefined } from '../../helpers/types';
import { ComponentDescriptor } from './context';

interface Props {
  component: ComponentDescriptor;
  limited?: boolean;
}

export default function WorkspaceComponentTitle({ component, limited }: Props) {
  const { name = '—' } = component;
  return (
    <>
      {isDefined(component.qualifier) && (
        <QualifierIcon
          aria-label={translate('qualifier', component.qualifier)}
          className="sw-mr-1"
          qualifier={component.qualifier}
        />
      )}
      {limited ? collapsePath(name, 15) : name}
    </>
  );
}
