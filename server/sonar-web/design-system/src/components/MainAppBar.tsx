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
import tw from 'twin.macro';
import {
  LAYOUT_GLOBAL_NAV_HEIGHT,
  LAYOUT_LOGO_MARGIN_RIGHT,
  LAYOUT_LOGO_MAX_HEIGHT,
  LAYOUT_LOGO_MAX_WIDTH,
  LAYOUT_VIEWPORT_MIN_WIDTH,
} from '../helpers/constants';
import { themeBorder, themeColor, themeContrast } from '../helpers/theme';
import { BaseLink } from './Link';

const MainAppBarDiv = styled.div`
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
  return (
    <MainAppBarDiv>
      <MainAppBarNavLogoDiv>
        <MainAppBarNavLogoLink to="/">
          <Logo />
        </MainAppBarNavLogoLink>
      </MainAppBarNavLogoDiv>
      <MainAppBarNavRightDiv>{children}</MainAppBarNavRightDiv>
    </MainAppBarDiv>
  );
}
