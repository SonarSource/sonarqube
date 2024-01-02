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
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { Alert } from '../../../components/ui/Alert';
import { translate } from '../../../helpers/l10n';
import DocLink from '../../common/DocLink';

export interface CompilationInfoProps {
  className?: string;
}

export function CompilationInfo({ className = 'spacer-top spacer-bottom' }: CompilationInfoProps) {
  return (
    <Alert className={className} variant="info">
      <p className="spacer-bottom">
        <FormattedMessage
          id="onboarding.tutorial.cfamilly.compilation_database_info"
          defaultMessage={translate('onboarding.tutorial.cfamilly.compilation_database_info')}
          values={{
            link: (
              <DocLink to="/analyzing-source-code/languages/c-family/">
                {translate('onboarding.tutorial.cfamilly.compilation_database_info.link')}
              </DocLink>
            ),
          }}
        />
      </p>
      <p>
        <FormattedMessage
          id="onboarding.tutorial.cfamilly.speed_caching"
          defaultMessage={translate('onboarding.tutorial.cfamilly.speed_caching')}
          values={{
            link: (
              <DocLink to="/analyzing-source-code/languages/c-family/#analysis-cache">
                {translate('onboarding.tutorial.cfamilly.speed_caching.link')}
              </DocLink>
            ),
          }}
        />
      </p>
    </Alert>
  );
}
