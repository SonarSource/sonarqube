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
import IssuesFilter from './IssuesFilter';
import { Facet } from '../types';
import BugIcon from '../../../components/icons-components/BugIcon';
import { translate } from '../../../helpers/l10n';
import { RawQuery } from '../../../helpers/query';

interface Props {
  className?: string;
  facet?: Facet;
  headerDetail?: React.ReactNode;
  maxFacetValue?: number;
  onQueryChange: (change: RawQuery) => void;
  organization?: { key: string };
  query: T.Dict<any>;
  value?: any;
}

export default function ReliabilityFilter(props: Props) {
  return (
    <IssuesFilter
      {...props}
      headerDetail={
        <span className="note little-spacer-left">
          {'('}
          <BugIcon className="little-spacer-right" />
          {translate('metric.bugs.name')}
          {' )'}
        </span>
      }
      name="Reliability"
      property="reliability"
    />
  );
}
