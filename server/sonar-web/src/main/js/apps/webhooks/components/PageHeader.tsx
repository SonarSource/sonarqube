/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { Link, Title } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { useDocUrl } from '../../../helpers/docs';
import { translate } from '../../../helpers/l10n';

interface Props {
  children?: React.ReactNode;
}

export default function PageHeader({ children }: Readonly<Props>) {
  const toUrl = useDocUrl('/project-administration/webhooks/');

  return (
    <header className="sw-mb-2 sw-flex sw-items-center sw-justify-between">
      <div>
        <Title>{translate('webhooks.page')}</Title>
        <p>{translate('webhooks.description0')}</p>
        <p>
          <FormattedMessage
            defaultMessage={translate('webhooks.description1')}
            id="webhooks.description"
            values={{
              url: <Link to={toUrl}>{translate('webhooks.documentation_link')}</Link>,
            }}
          />
        </p>
      </div>
      <div>{children}</div>
    </header>
  );
}
