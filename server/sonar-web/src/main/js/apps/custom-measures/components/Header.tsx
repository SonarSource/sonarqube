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
import * as React from 'react';
import CreateButton from './CreateButton';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import { translate } from '../../../helpers/l10n';

interface Props {
  loading: boolean;
  onCreate: (data: { description: string; metricKey: string; value: string }) => Promise<void>;
  skipMetrics: string[] | undefined;
}

export default function Header({ loading, onCreate, skipMetrics }: Props) {
  return (
    <header className="page-header" id="custom-measures-header">
      <h1 className="page-title">{translate('custom_measures.page')}</h1>
      <DeferredSpinner loading={loading} />
      <div className="page-actions">
        <CreateButton onCreate={onCreate} skipMetrics={skipMetrics} />
      </div>
      <p className="page-description">{translate('custom_measures.page.description')}</p>
    </header>
  );
}
