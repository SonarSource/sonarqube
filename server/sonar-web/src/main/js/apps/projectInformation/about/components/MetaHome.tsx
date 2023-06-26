/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { Checkbox, HelperHintIcon } from 'design-system';
import React, { useContext } from 'react';
import { setHomePage } from '../../../../api/users';
import { CurrentUserContext } from '../../../../app/components/current-user/CurrentUserContext';
import HelpTooltip from '../../../../components/controls/HelpTooltip';
import { DEFAULT_HOMEPAGE } from '../../../../components/controls/HomePageSelect';
import { translate } from '../../../../helpers/l10n';
import { isSameHomePage } from '../../../../helpers/users';
import { HomePage, LoggedInUser, isLoggedIn } from '../../../../types/users';

export interface MetaHomeProps {
  componentKey: string;
  currentUser: LoggedInUser;
  isApp?: boolean;
}

export default function MetaHome({ componentKey, currentUser, isApp }: MetaHomeProps) {
  const { updateCurrentUserHomepage } = useContext(CurrentUserContext);
  const currentPage: HomePage = {
    component: componentKey,
    type: isApp ? 'APPLICATION' : 'PROJECT',
    branch: undefined,
  };

  const setCurrentUserHomepage = async (homepage: HomePage) => {
    if (isLoggedIn(currentUser)) {
      await setHomePage(homepage);

      updateCurrentUserHomepage(homepage);
    }
  };

  const handleClick = (value: boolean) => {
    setCurrentUserHomepage(value ? currentPage : DEFAULT_HOMEPAGE);
  };

  return (
    <>
      <div className="sw-flex sw-items-center">
        <h3>{translate('project.info.make_home.title')}</h3>
        <HelpTooltip
          className="sw-ml-1"
          overlay={
            <p className="sw-max-w-abs-250">
              {translate(isApp ? 'application' : 'project', 'info.make_home.tooltip')}
            </p>
          }
        >
          <HelperHintIcon />
        </HelpTooltip>
      </div>
      <Checkbox
        checked={
          currentUser.homepage !== undefined && isSameHomePage(currentUser.homepage, currentPage)
        }
        onCheck={handleClick}
      >
        <span className="sw-ml-2">
          {translate(isApp ? 'application' : 'project', 'info.make_home.label')}
        </span>
      </Checkbox>
    </>
  );
}
