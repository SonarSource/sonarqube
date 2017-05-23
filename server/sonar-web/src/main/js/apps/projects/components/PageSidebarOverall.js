/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
//@flow
import React from 'react';
import CoverageFilter from '../filters/CoverageFilter';
import DuplicationsFilter from '../filters/DuplicationsFilter';
import SizeFilter from '../filters/SizeFilter';
import ReliabilityFilter from '../filters/ReliabilityFilter';
import SecurityFilter from '../filters/SecurityFilter';
import MaintainabilityFilter from '../filters/MaintainabilityFilter';

type Props = {
  isFavorite: boolean,
  organization?: { key: string },
  query: { [string]: string }
};

export default function PageSidebarOverall({ query, isFavorite, organization }: Props) {
  return (
    <div>
      <ReliabilityFilter query={query} isFavorite={isFavorite} organization={organization} />
      <SecurityFilter query={query} isFavorite={isFavorite} organization={organization} />
      <MaintainabilityFilter query={query} isFavorite={isFavorite} organization={organization} />
      <CoverageFilter query={query} isFavorite={isFavorite} organization={organization} />
      <DuplicationsFilter query={query} isFavorite={isFavorite} organization={organization} />
      <SizeFilter query={query} isFavorite={isFavorite} organization={organization} />
    </div>
  );
}
