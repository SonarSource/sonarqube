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

import { ICell } from '@jupyterlab/nbformat';
import { Spinner } from '@sonarsource/echoes-react';
import { FlagMessage } from 'design-system/lib';
import React from 'react';
import { JupyterCell } from '~sonar-aligned/components/SourceViewer/JupyterNotebookViewer';
import { getBranchLikeQuery } from '~sonar-aligned/helpers/branch-like';
import { translate } from '../../helpers/l10n';
import { useRawSourceQuery } from '../../queries/sources';
import { BranchLike } from '../../types/branch-like';

export interface Props {
  branchLike: BranchLike | undefined;
  component: string;
}

export default function SourceViewerPreview(props: Readonly<Props>) {
  const { component, branchLike } = props;

  const { data, isLoading } = useRawSourceQuery({
    key: component,
    ...getBranchLikeQuery(branchLike),
  });

  if (isLoading) {
    return <Spinner isLoading={isLoading} />;
  }

  if (typeof data !== 'string') {
    return (
      <FlagMessage className="sw-mt-2" variant="warning">
        {translate('component_viewer.no_component')}
      </FlagMessage>
    );
  }

  const jupyterFile: { cells: ICell[] } = JSON.parse(data);

  return (
    <>
      {jupyterFile.cells.map((cell: ICell, index: number) => (
        <JupyterCell cell={cell} key={`${cell.cell_type}-${index}`} />
      ))}
    </>
  );
}
