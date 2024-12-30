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

import { Link } from '@sonarsource/echoes-react';
import { Helmet } from 'react-helmet-async';
import { Card, CenteredLayout, SubHeading } from '~design-system';
import { translate } from '../../helpers/l10n';

export interface ComponentContainerNotFoundProps {
  isPortfolioLike: boolean;
}

export default function ComponentContainerNotFound({
  isPortfolioLike,
}: Readonly<ComponentContainerNotFoundProps>) {
  const componentType = isPortfolioLike ? 'portfolio' : 'project';

  return (
    <CenteredLayout className="sw-flex sw-justify-around" id="bd">
      <Helmet defaultTitle={translate('404_not_found')} defer={false} />
      <Card className="sw-mt-24" id="nonav">
        <SubHeading>{translate('dashboard', componentType, 'not_found')}</SubHeading>
        <p className="sw-mb-2">{translate('dashboard', componentType, 'not_found.2')}</p>
        <p>
          <Link to="/">{translate('go_back_to_homepage')}</Link>
        </p>
      </Card>
    </CenteredLayout>
  );
}
