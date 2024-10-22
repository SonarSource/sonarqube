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

import { Link, LinkHighlight } from '@sonarsource/echoes-react';
import { isOfficial } from '../../helpers/system';

export default function GlobalFooterBranding() {
  const official = isOfficial();

  return official ? (
    <div>
      SonarQube&trade; technology is powered by{' '}
      <Link highlight={LinkHighlight.CurrentColor} to="https://www.sonarsource.com">
        SonarSource SA
      </Link>
    </div>
  ) : (
    <div>
      This application is based on{' '}
      <Link
        highlight={LinkHighlight.CurrentColor}
        to="https://www.sonarsource.com/products/sonarqube/?referrer=sonarqube"
        title="SonarQube™"
      >
        SonarQube™
      </Link>{' '}
      but is <strong>not</strong> an official version provided by{' '}
      <Link
        highlight={LinkHighlight.CurrentColor}
        to="https://www.sonarsource.com"
        title="SonarSource SA"
      >
        SonarSource SA
      </Link>
      .
    </div>
  );
}
