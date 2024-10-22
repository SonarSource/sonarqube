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
import { LargeCenteredLayout, PageContentFontWrapper, Title } from '~design-system';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { Router } from '~sonar-aligned/types/router';
import { changeKey } from '../../api/components';
import RecentHistory from '../../app/components/RecentHistory';
import withComponentContext from '../../app/components/componentContext/withComponentContext';
import { translate } from '../../helpers/l10n';
import { Component } from '../../types/types';
import UpdateForm from './UpdateForm';

interface Props {
  component: Component;
  router: Router;
}

function ProjectKeyApp({ component, router }: Props) {
  const handleChangeKey = (newKey: string) => {
    return changeKey({ from: component.key, to: newKey }).then(() => {
      RecentHistory.remove(component.key);
      router.replace({ pathname: '/project/key', query: { id: newKey } });
    });
  };

  return (
    <LargeCenteredLayout id="project-key">
      <Helmet defer={false} title={translate('update_key.page')} />
      <PageContentFontWrapper className="sw-my-8 sw-typo-default">
        <header className="sw-mt-8 sw-mb-4">
          <Title className="sw-mb-4">{translate('update_key.page')}</Title>
          <div className="sw-mb-2">{translate('update_key.page.description')}</div>
        </header>
        <UpdateForm component={component} onKeyChange={handleChangeKey} />
      </PageContentFontWrapper>
    </LargeCenteredLayout>
  );
}

export default withComponentContext(withRouter(ProjectKeyApp));
