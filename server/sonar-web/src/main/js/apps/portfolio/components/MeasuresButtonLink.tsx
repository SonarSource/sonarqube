/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { Link } from 'react-router';
import MeasuresIcon from 'sonar-ui-common/components/icons/MeasuresIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getComponentDrilldownUrl } from '../../../helpers/urls';

interface Props {
  component: string;
  label?: string;
  metric: string;
}

export default function MeasuresButtonLink({ component, label, metric }: Props) {
  return (
    <Link to={getComponentDrilldownUrl({ componentKey: component, metric })}>
      <MeasuresIcon className="little-spacer-right" size={14} />
      <span>{label || translate('portfolio.measures_link')}</span>
    </Link>
  );
}
