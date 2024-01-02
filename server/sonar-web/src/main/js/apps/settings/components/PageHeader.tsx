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
import { Title } from 'design-system';
import * as React from 'react';
import InstanceMessage from '../../../components/common/InstanceMessage';
import { translate } from '../../../helpers/l10n';
import { ExtendedSettingDefinition } from '../../../types/settings';
import { Component } from '../../../types/types';
import SettingsSearch from './SettingsSearch';

export interface PageHeaderProps {
  component?: Component;
  definitions: ExtendedSettingDefinition[];
}

export default function PageHeader({ component, definitions }: Readonly<PageHeaderProps>) {
  const title = component ? translate('project_settings.page') : translate('settings.page');

  const description = component ? (
    translate('project_settings.page.description')
  ) : (
    <InstanceMessage message={translate('settings.page.description')} />
  );

  return (
    <header className="sw-mb-5">
      <Title className="sw-mb-4">{title}</Title>
      <p className="sw-mb-4">{description}</p>
      <SettingsSearch component={component} definitions={definitions} />
    </header>
  );
}
