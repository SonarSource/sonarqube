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
import { Button } from '@sonarsource/echoes-react';
import { DiscreetInteractiveIcon, HomeFillIcon, HomeIcon } from 'design-system';
import React from 'react';
import { useIntl } from 'react-intl';
import { setHomePage } from '../../api/users';
import { CurrentUserContextInterface } from '../../app/components/current-user/CurrentUserContext';
import withCurrentUserContext from '../../app/components/current-user/withCurrentUserContext';
import { isSameHomePage } from '../../helpers/users';
import { HomePage, isLoggedIn } from '../../types/users';
import Tooltip from './Tooltip';

interface Props
  extends Pick<CurrentUserContextInterface, 'currentUser' | 'updateCurrentUserHomepage'> {
  className?: string;
  currentPage: HomePage;
  type?: 'button' | 'icon';
}

export const DEFAULT_HOMEPAGE: HomePage = { type: 'PROJECTS' };

export function HomePageSelect(props: Readonly<Props>) {
  const { currentPage, className, type = 'icon', currentUser, updateCurrentUserHomepage } = props;
  const intl = useIntl();

  if (!isLoggedIn(currentUser)) {
    return null;
  }

  const isChecked =
    currentUser.homepage !== undefined && isSameHomePage(currentUser.homepage, currentPage);
  const isDefault = isChecked && isSameHomePage(currentPage, DEFAULT_HOMEPAGE);

  const setCurrentUserHomepage = async (homepage: HomePage) => {
    if (isLoggedIn(currentUser)) {
      await setHomePage(homepage);

      updateCurrentUserHomepage(homepage);
    }
  };

  const tooltip = isChecked
    ? intl.formatMessage({ id: isDefault ? 'homepage.current.is_default' : 'homepage.current' })
    : intl.formatMessage({ id: 'homepage.check' });

  const handleClick = () => setCurrentUserHomepage?.(isChecked ? DEFAULT_HOMEPAGE : currentPage);

  const Icon = isChecked ? HomeFillIcon : HomeIcon;

  return (
    <Tooltip content={tooltip}>
      {type === 'icon' ? (
        <DiscreetInteractiveIcon
          aria-label={tooltip}
          className={className}
          disabled={isDefault}
          Icon={Icon}
          onClick={handleClick}
        />
      ) : (
        <Button
          aria-label={tooltip}
          prefix={<Icon />}
          className={className}
          isDisabled={isDefault}
          onClick={handleClick}
        >
          {intl.formatMessage({ id: 'overview.set_as_homepage' })}
        </Button>
      )}
    </Tooltip>
  );
}

export default withCurrentUserContext(HomePageSelect);
