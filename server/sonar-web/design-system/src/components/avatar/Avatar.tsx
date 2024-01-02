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
import styled from '@emotion/styled';
import { ReactEventHandler, useState } from 'react';
import tw from 'twin.macro';
import { themeBorder, themeColor } from '../../helpers/theme';
import { GenericAvatar } from './GenericAvatar';
import { Size, sizeMap } from './utils';

interface AvatarProps {
  border?: boolean;
  className?: string;
  enableGravatar?: boolean;
  gravatarServerUrl?: string;
  hash?: string;
  name?: string;
  organizationAvatar?: string;
  organizationName?: string;
  size?: Size;
}

/**
 * (!) Do not use directly. it requires the gravatar settings to properly fetch the avatars.
 * This is injected by the `Avatar` component in `components/ui` in sonar-web
 */
export function Avatar({
  className,
  enableGravatar,
  gravatarServerUrl,
  hash,
  name,
  organizationAvatar,
  organizationName,
  size = 'sm',
  border,
}: AvatarProps) {
  const [imgError, setImgError] = useState(false);
  const numberSize = sizeMap[size];
  const resolvedName = organizationName ?? name;

  const handleImgError: ReactEventHandler<HTMLImageElement> = () => {
    setImgError(true);
  };

  if (!imgError) {
    if (enableGravatar && gravatarServerUrl && hash) {
      const url = gravatarServerUrl
        .replace('{EMAIL_MD5}', hash)
        .replace('{SIZE}', String(numberSize * 2));

      return (
        <StyledAvatar
          alt={resolvedName}
          border={border}
          className={className}
          height={numberSize}
          onError={handleImgError}
          role="img"
          src={url}
          width={numberSize}
        />
      );
    }

    if (resolvedName && organizationAvatar) {
      return (
        <StyledAvatar
          alt={resolvedName}
          border={border}
          className={className}
          height={numberSize}
          onError={handleImgError}
          role="img"
          src={organizationAvatar}
          width={numberSize}
        />
      );
    }
  }

  if (!resolvedName) {
    return <input className="sw-appearance-none" />;
  }

  return <GenericAvatar className={className} name={resolvedName} size={size} />;
}

const StyledAvatar = styled.img<{ border?: boolean }>`
  ${tw`sw-inline-flex`};
  ${tw`sw-items-center`};
  ${tw`sw-justify-center`};
  ${tw`sw-align-top`};
  ${tw`sw-rounded-1`};
  border: ${({ border }) => (border ? themeBorder('default', 'avatarBorder') : '')};
  background: ${themeColor('avatarBackground')};
`;
