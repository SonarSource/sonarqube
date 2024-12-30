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

import { LogoSize } from '@sonarsource/echoes-react';
import * as React from 'react';
import { MainAppBar } from '~design-system';
import { Image } from '~sonar-aligned/components/common/Image';
import { SonarQubeProductLogo } from '../../../../components/branding/SonarQubeProductLogo';
import { translate } from '../../../../helpers/l10n';

function LogoWithAriaText() {

  const customLogoUrl = `/images/sonarcloud-logo-wb.svg`;
  const title = customLogoUrl
    ? translate('layout.nav.home_logo_alt')
    : translate('layout.nav.home_sonarqube_logo_alt');

  return (
    <div aria-label={title} role="img">
      {customLogoUrl ? (
        <Image alt={title} src={customLogoUrl} width={120} />
      ) : (
        <SonarQubeProductLogo hasText size={LogoSize.Large} />
      )}
    </div>
  );
}

export default function MainSonarQubeBar({ children }: React.PropsWithChildren<object>) {
  return <MainAppBar Logo={LogoWithAriaText}>{children}</MainAppBar>;
}
