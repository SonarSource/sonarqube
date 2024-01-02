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
import * as React from 'react';
import DocLink from '../../../components/common/DocLink';
import { translate } from '../../../helpers/l10n';

export default function CaycBadgeTooltip() {
  return (
    <div>
      <p className="spacer-bottom padded-bottom bordered-bottom-cayc">
        {translate('quality_gates.cayc.tooltip.message')}
      </p>
      <DocLink to="/user-guide/clean-as-you-code/">
        {translate('quality_gates.cayc.badge.tooltip.learn_more')}
      </DocLink>
    </div>
  );
}
