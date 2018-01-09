/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import SimpleBubbleChart from './SimpleBubbleChart';
import { Project } from '../types';

interface Props {
  displayOrganizations: boolean;
  projects: Project[];
}

export default function Coverage(props: Props) {
  return (
    <SimpleBubbleChart
      {...props}
      xMetric={{ key: 'complexity', type: 'SHORT_INT' }}
      yMetric={{ key: 'coverage', type: 'PERCENT' }}
      yDomain={[100, 0]}
      sizeMetric={{ key: 'uncovered_lines', type: 'SHORT_INT' }}
    />
  );
}
