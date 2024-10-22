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

import { useTheme } from '@emotion/react';
import styled from '@emotion/styled';
import { throttle } from 'lodash';
import React from 'react';
import tw from 'twin.macro';
import {
  LAYOUT_GLOBAL_NAV_HEIGHT,
  LAYOUT_LOGO_MARGIN_RIGHT,
  LAYOUT_LOGO_MAX_HEIGHT,
  LAYOUT_LOGO_MAX_WIDTH,
  LAYOUT_VIEWPORT_MIN_WIDTH,
  THROTTLE_SCROLL_DELAY,
} from '../helpers/constants';
import { themeBorder, themeColor, themeContrast, themeShadow } from '../helpers/theme';
import { BaseLink } from './Link';

const MainAppBarHeader = styled.header`
  ${tw`sw-flex`};
  ${tw`sw-items-center`};
  ${tw`sw-px-6`};
  ${tw`sw-w-full`};
  ${tw`sw-box-border`};

  background: ${themeColor('mainBar')};
  border-bottom: ${themeBorder('default')};
  color: ${themeContrast('mainBar')};
  height: ${LAYOUT_GLOBAL_NAV_HEIGHT}px;
  min-width: ${LAYOUT_VIEWPORT_MIN_WIDTH}px;
`;

const MainAppBarNavLogoDiv = styled.div`
  margin-right: ${LAYOUT_LOGO_MARGIN_RIGHT}px;

  img,
  svg {
    ${tw`sw-object-contain`};

    max-height: ${LAYOUT_LOGO_MAX_HEIGHT}px;
    max-width: ${LAYOUT_LOGO_MAX_WIDTH}px;
  }
`;

const MainAppBarNavLogoLink = styled(BaseLink)`
  border: none;
`;

const MainAppBarNavRightDiv = styled.div`
  flex-grow: 2;
  height: 100%;
`;

export function MainAppBar({
  children,
  Logo,
}: React.PropsWithChildren<{ Logo: React.ElementType }>) {
  const theme = useTheme();
  const [boxShadow, setBoxShadow] = React.useState('none');

  React.useEffect(() => {
    const handleScroll = throttle(() => {
      setBoxShadow(document.documentElement?.scrollTop > 0 ? themeShadow('md')({ theme }) : 'none');
    }, THROTTLE_SCROLL_DELAY);

    document.addEventListener('scroll', handleScroll);
    return () => {
      document.removeEventListener('scroll', handleScroll);
    };
  }, [theme]);

  return (
    <MainAppBarHeader style={{ boxShadow }}>
      <MainAppBarNavLogoDiv>
        <MainAppBarNavLogoLink to="/">
          <Logo />
        </MainAppBarNavLogoLink>
      </MainAppBarNavLogoDiv>
      <MainAppBarNavRightDiv>{children}</MainAppBarNavRightDiv>
    </MainAppBarHeader>
  );
}
