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

import { LinkHighlight, LinkStandalone } from '@sonarsource/echoes-react';
import * as React from 'react';
import Favorite from '../../../../components/controls/Favorite';
import { getComponentOverviewUrl } from '../../../../helpers/urls';
import { Component } from '../../../../types/types';
import { CurrentUser, isLoggedIn } from '../../../../types/users';

export interface BreadcrumbProps {
  component: Component;
  currentUser: CurrentUser;
}

export function Breadcrumb(props: Readonly<BreadcrumbProps>) {
  const { component, currentUser } = props;

  return (
    <div className="sw-text-sm sw-flex sw-justify-center">
      {component.breadcrumbs.map((breadcrumbElement, i) => {
        const isNotLast = i < component.breadcrumbs.length - 1;
        const isLast = !isNotLast;

        return (
          <div key={breadcrumbElement.key} className="sw-flex sw-items-center">
            {isLast && isLoggedIn(currentUser) && (
              <Favorite
                className="sw-mr-2"
                component={component.key}
                favorite={Boolean(component.isFavorite)}
                qualifier={component.qualifier}
              />
            )}

            <LinkStandalone
              highlight={LinkHighlight.Subdued}
              className="js-project-link"
              key={breadcrumbElement.name}
              shouldBlurAfterClick
              title={breadcrumbElement.name}
              to={getComponentOverviewUrl(breadcrumbElement.key, breadcrumbElement.qualifier)}
            >
              {breadcrumbElement.name}
            </LinkStandalone>

            {isNotLast && <span className="slash-separator sw-mx-2" />}
          </div>
        );
      })}
    </div>
  );
}

export default React.memo(Breadcrumb);
