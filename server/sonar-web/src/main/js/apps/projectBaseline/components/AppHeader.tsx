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
import DocLink from '../../../components/common/DocLink';
import Link from '../../../components/common/Link';
import { translate } from '../../../helpers/l10n';

export interface AppHeaderProps {
  canAdmin: boolean;
}

export default function AppHeader(props: AppHeaderProps) {
  const { canAdmin } = props;

  return (
    <header className="page-header">
      <h1 className="page-title">{translate('project_baseline.page')}</h1>
      <p className="page-description">
        <FormattedMessage
          defaultMessage={translate('project_baseline.page.description')}
          id="project_baseline.page.description"
          values={{
            link: (
              <DocLink to="/project-administration/defining-new-code/">
                {translate('project_baseline.page.description.link')}
              </DocLink>
            ),
          }}
        />
        <br />
        {canAdmin && (
          <FormattedMessage
            defaultMessage={translate('project_baseline.page.description2')}
            id="project_baseline.page.description2"
            values={{
              link: (
                <Link to="/admin/settings?category=new_code_period">
                  {translate('project_baseline.page.description2.link')}
                </Link>
              ),
            }}
          />
        )}
      </p>
    </header>
  );
}
