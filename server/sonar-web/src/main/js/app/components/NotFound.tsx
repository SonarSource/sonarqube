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
import { Card, CenteredLayout, Link, PageContentFontWrapper, Title } from '~design-system';
import { translate } from '../../helpers/l10n';

export default function NotFound() {
  return (
    <>
      <Helmet defaultTitle={translate('404_not_found')} defer={false} />
      <PageContentFontWrapper className="sw-typo-lg">
        <CenteredLayout className="sw-flex sw-flex-col sw-items-center">
          <Card className="sw-m-14 sw-w-abs-600">
            <Title>{translate('page_not_found')}</Title>
            <p className="sw-mb-2">{translate('address_mistyped_or_page_moved')}</p>
            <p>
              <Link to="/">{translate('go_back_to_homepage')}</Link>
            </p>
          </Card>
        </CenteredLayout>
      </PageContentFontWrapper>
    </>
  );
}
