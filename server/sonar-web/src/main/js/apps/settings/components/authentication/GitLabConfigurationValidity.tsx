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
import React from 'react';
import { Button } from '../../../../components/controls/buttons';
import { Alert } from '../../../../components/ui/Alert';
import { translate } from '../../../../helpers/l10n';
import { GitlabConfiguration } from '../../../../types/provisioning';

const intlPrefix = 'settings.authentication.gitlab.configuration';

interface Props {
  configuration: GitlabConfiguration | undefined;
  loading: boolean;
  onRecheck: () => void;
}

export default function GitLabConfigurationValidity(props: Readonly<Props>) {
  const { configuration, loading } = props;
  const message = loading
    ? translate(`${intlPrefix}.validity_check_loading`)
    : configuration?.errorMessage ??
      translate(`${intlPrefix}.valid.${configuration?.provisioningType}`);
  const variant = configuration?.errorMessage ? 'error' : 'success';

  return (
    <Alert
      title={message}
      variant={loading ? 'loading' : variant}
      aria-live="polite"
      role="status"
      aria-atomic
      aria-busy={loading}
    >
      <div className="sw-flex sw-justify-between sw-items-center">
        <div>{message}</div>
        <div className="sw-flex">
          <Button
            onClick={props.onRecheck}
            disabled={loading}
            className="sw-whitespace-nowrap sw-text-center"
          >
            {translate(`${intlPrefix}.test`)}
          </Button>
        </div>
      </div>
    </Alert>
  );
}
