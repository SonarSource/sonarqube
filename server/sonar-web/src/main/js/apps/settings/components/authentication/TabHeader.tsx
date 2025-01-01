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

import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import { ReactElement } from 'react';
import { BasicSeparator, SubHeading } from '~design-system';
import { translate } from '../../../../helpers/l10n';

interface Props {
  configurationValidity?: ReactElement;
  onCreate: () => void;
  showCreate: boolean;
  title: string;
}

export default function TabHeader({
  title,
  showCreate,
  onCreate,
  configurationValidity,
}: Readonly<Props>) {
  return (
    <>
      <div className="sw-mb-4">
        <SubHeading as="h4">{title}</SubHeading>
        {showCreate && (
          <Button className="sw-mt-2" onClick={onCreate} variety={ButtonVariety.Primary}>
            {translate('settings.authentication.form.create')}
          </Button>
        )}
      </div>
      {configurationValidity}
      <BasicSeparator className="sw-my-6" />
    </>
  );
}
