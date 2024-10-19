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
import { SubnavigationItem } from 'design-system';
import React from 'react';
import { MeasureEnhanced } from '../../../types/types';
import SubnavigationMeasureValue from './SubnavigationMeasureValue';

interface Props {
  componentKey: string;
  measure: MeasureEnhanced;
  name: string;
  onChange: (metric: string) => void;
  selected: string;
}

export default function DomainSubnavigationItem({
  componentKey,
  measure,
  name,
  onChange,
  selected,
}: Readonly<Props>) {
  const { key } = measure.metric;
  return (
    <SubnavigationItem
      active={key === selected}
      ariaCurrent={key === selected}
      key={key}
      onClick={onChange}
      value={key}
      className="sw-pl-2 sw-w-full sw-flex sw-justify-between"
      id={`measure-${key}-name`}
    >
      {name}
      <SubnavigationMeasureValue measure={measure} componentKey={componentKey} />
    </SubnavigationItem>
  );
}
