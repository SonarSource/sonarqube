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
import MeasuresIcon from '../../components/icons/MeasuresIcon';
import { translate } from '../../helpers/l10n';
import { getComponentDrilldownUrl } from '../../helpers/urls';
import { BranchLike } from '../../types/branch-like';
import Link from './Link';
import './MeasuresLink.css';

export interface MeasuresLinkProps {
  branchLike?: BranchLike;
  className?: string;
  component: string;
  label?: string;
  metric: string;
}

export default function MeasuresLink(props: MeasuresLinkProps) {
  const { branchLike, className, component, label, metric } = props;
  return (
    <Link
      className={classNames('measures-link', className)}
      to={getComponentDrilldownUrl({ branchLike, componentKey: component, metric })}
    >
      <MeasuresIcon className="little-spacer-right" size={14} />
      <span>{label || translate('portfolio.measures_link')}</span>
    </Link>
  );
}
