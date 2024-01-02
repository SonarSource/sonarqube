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
import classNames from 'classnames';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate } from '../../../helpers/l10n';
import DocLink from '../../common/DocLink';
import Link from '../../common/Link';
import { Alert } from '../../ui/Alert';

export interface ProjectTokenScopeInfoProps {
  className?: string;
}

export default function ProjectTokenScopeInfo({ className }: ProjectTokenScopeInfoProps) {
  return (
    <Alert variant="info" className={classNames('spacer-top', className)}>
      <FormattedMessage
        defaultMessage={translate('onboarding.token.warning_project_token_scope')}
        id="onboarding.token.warning_project_token_scope"
        values={{
          link: (
            <Link target="_blank" to="/account/security">
              {translate('onboarding.token.text.user_account')}
            </Link>
          ),
          doc_link: (
            <DocLink to="/user-guide/user-account/generating-and-using-tokens/">
              {translate('documentation')}
            </DocLink>
          ),
        }}
      />
    </Alert>
  );
}
