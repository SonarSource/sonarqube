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

import { LargeCenteredLayout, PageContentFontWrapper } from '~design-system';
import withComponentContext from '../../../app/components/componentContext/withComponentContext';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import TutorialSelection from '../../../components/tutorials/TutorialSelection';
import handleRequiredAuthentication from '../../../helpers/handleRequiredAuthentication';
import { Component } from '../../../types/types';
import { CurrentUser, isLoggedIn } from '../../../types/users';

export interface TutorialsAppProps {
  component: Component;
  currentUser: CurrentUser;
}

export function TutorialsApp(props: TutorialsAppProps) {
  const { component, currentUser } = props;

  if (!isLoggedIn(currentUser)) {
    handleRequiredAuthentication();
    return null;
  }

  return (
    <LargeCenteredLayout className="sw-pt-8">
      <PageContentFontWrapper>
        <TutorialSelection component={component} currentUser={currentUser} />
      </PageContentFontWrapper>
    </LargeCenteredLayout>
  );
}

export default withComponentContext(withCurrentUserContext(TutorialsApp));
