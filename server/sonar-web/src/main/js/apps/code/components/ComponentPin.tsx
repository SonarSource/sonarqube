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
import { InteractiveIcon, PinIcon } from 'design-system';
import * as React from 'react';
import { WorkspaceContextShape } from '../../../components/workspace/context';
import { translateWithParameters } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { ComponentMeasure } from '../../../types/types';

interface Props {
  branchLike?: BranchLike;
  component: ComponentMeasure;
  openComponent: WorkspaceContextShape['openComponent'];
}

export default function ComponentPin(props: Props) {
  const { branchLike, component, openComponent } = props;

  const handleClick = React.useCallback(() => {
    openComponent({
      branchLike,
      key: component.key,
      name: component.path,
      qualifier: component.qualifier,
    });
  }, [branchLike, component, openComponent]);

  const label = translateWithParameters('component_viewer.open_in_workspace_X', component.name);

  return (
    <span title={label}>
      <InteractiveIcon aria-label={label} Icon={PinIcon} onClick={handleClick} />
    </span>
  );
}
