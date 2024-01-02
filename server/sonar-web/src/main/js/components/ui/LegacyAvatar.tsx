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
import classNames from 'classnames';
import * as React from 'react';
import withAppStateContext from '../../app/components/app-state/withAppStateContext';
import { AppState } from '../../types/appstate';
import { GlobalSettingKeys } from '../../types/settings';
import GenericAvatar from './GenericAvatar';

const GRAVATAR_SIZE_MULTIPLIER = 2;

interface Props {
  appState: AppState;
  className?: string;
  hash?: string;
  name?: string;
  size: number;
}

/**
 * @deprecated Use Avatar instead
 */
export function LegacyAvatar(props: Props) {
  const {
    appState: { settings },
    className,
    hash,
    name,
    size,
  } = props;

  const enableGravatar = settings[GlobalSettingKeys.EnableGravatar] === 'true';

  if (!enableGravatar || !hash) {
    if (!name) {
      return null;
    }
    return <GenericAvatar className={className} name={name} size={size} />;
  }

  const gravatarServerUrl = settings[GlobalSettingKeys.GravatarServerUrl] ?? '';
  const url = gravatarServerUrl
    .replace('{EMAIL_MD5}', hash)
    .replace('{SIZE}', String(size * GRAVATAR_SIZE_MULTIPLIER));

  return (
    <img
      alt={name}
      className={classNames(className, 'rounded')}
      height={size}
      src={url}
      width={size}
    />
  );
}

export default withAppStateContext(LegacyAvatar);
