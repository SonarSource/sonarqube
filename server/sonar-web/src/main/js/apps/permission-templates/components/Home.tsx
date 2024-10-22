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
import { LargeCenteredLayout, PageContentFontWrapper } from '~design-system';
import { translate } from '../../../helpers/l10n';
import { Permission, PermissionTemplate } from '../../../types/types';
import Header from './Header';
import List from './List';

interface Props {
  permissionTemplates: PermissionTemplate[];
  permissions: Permission[];
  ready: boolean;
  refresh: () => Promise<void>;
  topQualifiers: string[];
}

export default function Home(props: Props) {
  return (
    <LargeCenteredLayout id="users-page">
      <PageContentFontWrapper className="sw-my-8 sw-typo-default">
        <Helmet defer={false} title={translate('permission_templates.page')} />

        <Header ready={props.ready} refresh={props.refresh} />

        <main>
          <List
            permissionTemplates={props.permissionTemplates}
            permissions={props.permissions}
            refresh={props.refresh}
            topQualifiers={props.topQualifiers}
          />
        </main>
      </PageContentFontWrapper>
    </LargeCenteredLayout>
  );
}
