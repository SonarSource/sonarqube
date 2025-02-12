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

import { Heading, Link, LinkHighlight } from '@sonarsource/echoes-react';
import { FormattedMessage } from 'react-intl';
import { DocLink } from '../../../helpers/doc-links';
import { useDocUrl } from '../../../helpers/docs';
import { translate } from '../../../helpers/l10n';

export interface AppHeaderProps {
  canAdmin: boolean;
}

export default function AppHeader(props: AppHeaderProps) {
  const { canAdmin } = props;
  const toUrl = useDocUrl(DocLink.NewCodeDefinition);

  return (
    <header className="sw-mt-8 sw-mb-4">
      <Heading as="h1" className="sw-mb-4">
        {translate('project_baseline.page')}
      </Heading>
      <p className="sw-mb-2">{translate('project_baseline.page.description')}</p>
      <p className="sw-mb-2">{translate('settings.new_code_period.description1')}</p>
      <p className="sw-mb-2">
        {canAdmin && (
          <FormattedMessage
            id="project_baseline.page.description2"
            values={{
              link: (
                <Link
                  highlight={LinkHighlight.CurrentColor}
                  to="/admin/settings?category=new_code_period"
                >
                  {translate('project_baseline.page.description2.link')}
                </Link>
              ),
            }}
          />
        )}
      </p>
      <p className="sw-mb-2">
        <FormattedMessage
          id="settings.new_code_period.description3"
          values={{
            link: (
              <Link highlight={LinkHighlight.CurrentColor} to={toUrl} shouldOpenInNewTab={true}>
                {translate('settings.new_code_period.description3.link')}
              </Link>
            ),
          }}
        />
      </p>
    </header>
  );
}
