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

import { Helmet } from 'react-helmet-async';
import { Card, CenteredLayout, Link, PageContentFontWrapper } from '~design-system';
import { getCookie } from '../../../helpers/cookies';
import { translate } from '../../../helpers/l10n';

export default function Unauthorized() {
  const message = decodeURIComponent(getCookie('AUTHENTICATION-ERROR') || '');
  return (
    <CenteredLayout id="bd">
      <Helmet defer={false} title={translate('unauthorized.page')} />
      <PageContentFontWrapper className="sw-typo-lg sw-flex sw-justify-center" id="nonav">
        <Card className="sw-w-abs-500 sw-my-14 sw-text-center">
          <p id="unauthorized">{translate('unauthorized.message')}</p>

          {Boolean(message) && (
            <p className="sw-mt-4">
              {translate('unauthorized.reason')}
              <br /> {message}
            </p>
          )}

          <div className="sw-mt-8">
            <Link to="/">{translate('layout.home')}</Link>
          </div>
        </Card>
      </PageContentFontWrapper>
    </CenteredLayout>
  );
}
