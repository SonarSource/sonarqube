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

interface Props {
  category: React.ReactElement;
  rating: React.ReactElement | null;
}

export default function MeasuresPanelCard(
  props: React.PropsWithChildren<Props & React.HTMLAttributes<HTMLDivElement>>,
) {
  const { category, children, rating, ...attributes } = props;

  return (
    <div className="sw-flex sw-justify-between sw-items-center" {...attributes}>
      <div className="sw-flex sw-flex-col sw-justify-between">
        <div className="sw-body-sm-highlight sw-flex sw-items-center">{category}</div>

        <div className="sw-mt-3">{children}</div>
      </div>

      <div>{rating}</div>
    </div>
  );
}
