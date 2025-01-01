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

import React from 'react';
import { ToggleButton } from '~design-system';
import { GradleBuildDSL } from '../types';

interface Props {
  children: (build: GradleBuildDSL) => React.ReactNode;
  className?: string;
}

export default function GradleBuildSelection({ children, className }: Props) {
  const [build, setBuild] = React.useState<GradleBuildDSL>(GradleBuildDSL.Groovy);

  const buildOptions = Object.values(GradleBuildDSL).map((v: GradleBuildDSL) => ({
    label: v,
    value: v,
  }));

  return (
    <>
      <div className={className}>
        <ToggleButton
          options={buildOptions}
          value={build}
          onChange={(value: GradleBuildDSL) => setBuild(value)}
        />
      </div>
      {children(build)}
    </>
  );
}
