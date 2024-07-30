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

import {
  IMarkdownCell,
  IOutput,
  isDisplayData,
  isExecuteResult,
  isStream,
} from '@jupyterlab/nbformat';
import classNames from 'classnames';
import { CodeSnippet } from 'design-system';
import { isArray } from 'lodash';
import React from 'react';
import Markdown from 'react-markdown';
import { translate } from '../../../helpers/l10n';
import { Image } from '../common/Image';

export function JupyterMarkdownCell({ cell }: Readonly<{ cell: IMarkdownCell }>) {
  const markdown = isArray(cell.source) ? cell.source.join('') : cell.source;
  return (
    <div className="sw-m-4 sw-ml-0">
      <Markdown>{markdown}</Markdown>
    </div>
  );
}

function CellOutput({ output }: Readonly<{ output: IOutput }>) {
  if (isExecuteResult(output) || isDisplayData(output)) {
    const components = Object.entries(output.data).map(([mimeType, dataValue], index) => {
      if (mimeType === 'image/png') {
        return (
          <Image
            src={`data:image/png;base64,${dataValue}`}
            alt={translate('source_viewer.jupyter.output.image')}
            key={`${mimeType}_${index}`}
          />
        );
      } else if (mimeType === 'text/plain') {
        const bundle = isArray(dataValue) ? dataValue.join('') : dataValue;

        return (
          <pre key={`${mimeType}_${index}`}>
            {typeof bundle === 'string' ? bundle : JSON.stringify(bundle)}
          </pre>
        );
      }
      return null;
    });
    return <>{components}</>;
  } else if (isStream(output)) {
    const text = isArray(output.text) ? output.text.join('') : output.text;
    return <pre>{text}</pre>;
  }
  return null;
}

export function JupyterCodeCell(
  props: Readonly<{ className?: string; outputs?: IOutput[]; source: string[] }>,
) {
  const { source, outputs, className } = props;

  return (
    <div className={classNames('sw-m-4 sw-ml-0', className)}>
      <div>
        <CodeSnippet language="python" noCopy snippet={source.join('')} wrap className="sw-p-4" />
      </div>
      <div>
        {outputs?.map((output: IOutput, outputIndex: number) => (
          <CellOutput key={`${output.output_type}-output-${outputIndex}`} output={output} />
        ))}
      </div>
    </div>
  );
}
