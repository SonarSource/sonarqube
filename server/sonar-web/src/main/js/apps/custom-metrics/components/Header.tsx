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
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import CreateButton from './CreateButton';
import { MetricProps } from './Form';

interface Props {
  domains: string[] | undefined;
  loading: boolean;
  onCreate: (data: MetricProps) => Promise<void>;
  types: string[] | undefined;
}

export default function Header({ domains, loading, onCreate, types }: Props) {
  return (
    <header className="page-header" id="custom-metrics-header">
      <h1 className="page-title">{translate('custom_metrics.page')}</h1>
      <DeferredSpinner loading={loading} />
      <div className="page-actions">
        {domains && types && <CreateButton domains={domains} onCreate={onCreate} types={types} />}
      </div>
      <div className="page-description">
        <Alert display="inline" variant="error">
          {translate('custom_metrics.deprecated')}
        </Alert>
        <p>{translate('custom_metrics.page.description')}</p>
      </div>
    </header>
  );
}
