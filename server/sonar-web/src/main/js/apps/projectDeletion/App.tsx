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
import { Helmet } from 'react-helmet-async';
import { CenteredLayout, PageContentFontWrapper } from '~design-system';
import { ComponentContext } from '../../app/components/componentContext/ComponentContext';
import { translate } from '../../helpers/l10n';
import Form from './Form';
import Header from './Header';

export default function App() {
  const { component } = React.useContext(ComponentContext);

  if (component === undefined) {
    return null;
  }

  return (
    <CenteredLayout>
      <Helmet defer={false} title={translate('deletion.page')} />
      <PageContentFontWrapper className="sw-my-8 sw-typo-default">
        <Header component={component} />
        <Form component={component} />
      </PageContentFontWrapper>
    </CenteredLayout>
  );
}
