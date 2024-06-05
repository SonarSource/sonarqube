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

import { ButtonSecondary, FlagMessage, Spinner, Variant } from 'design-system';
import React from 'react';
import { translate } from '../../../../helpers/l10n';

const intlPrefix = 'settings.authentication.configuration';

interface Props {
  flagMessageContent: React.ReactNode;
  flagMessageTitle: string;
  flagMessageVariant: Variant;
  loading: boolean;
  onTestConf: () => void;
}

export default function GitLabConfigurationValidity(props: Readonly<Props>) {
  const { loading, flagMessageContent, flagMessageTitle, flagMessageVariant, onTestConf } = props;

  return (
    <>
      <div className="sw-flex sw-items-center">
        <Spinner className="sw-mr-2 sw-my-2" loading={loading} />
        {loading && <p>{translate(`${intlPrefix}.validity_check_loading`)}</p>}
      </div>
      <FlagMessage
        className="sw-w-full"
        title={flagMessageTitle}
        variant={flagMessageVariant}
        aria-live="polite"
        role="status"
        aria-atomic
        aria-busy={loading}
      >
        {loading ? undefined : flagMessageContent}
      </FlagMessage>
      <ButtonSecondary
        onClick={onTestConf}
        disabled={loading}
        className="sw-whitespace-nowrap sw-text-center sw-my-4"
      >
        {translate(`${intlPrefix}.test`)}
      </ButtonSecondary>
    </>
  );
}
