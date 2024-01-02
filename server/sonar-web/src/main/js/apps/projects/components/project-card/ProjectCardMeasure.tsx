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

export interface ProjectCardMeasureProps {
  label: string;
  metricKey: string;
  className?: string;
}

export default function ProjectCardMeasure(
  props: React.PropsWithChildren<ProjectCardMeasureProps>,
) {
  const { label, metricKey, children, className } = props;

  return (
    <div
      data-key={metricKey}
      className={classNames('it__project_card_measure sw-text-center', className)}
    >
      <div className="sw-flex sw-justify-center">{children}</div>
      <div className="sw-body-sm sw-mt-1 sw-whitespace-nowrap" title={label}>
        {label}
      </div>
    </div>
  );
}
