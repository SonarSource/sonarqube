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

export const mockIpynbFile = JSON.stringify({
  cells: [
    {
      cell_type: 'markdown',
      metadata: {},
      source: ['# Learning a cosine with keras'],
    },
    {
      cell_type: 'code',
      execution_count: 2,
      metadata: {
        collapsed: false,
        jupyter: {
          outputs_hidden: false,
        },
      },
      outputs: [
        {
          name: 'stdout',
          output_type: 'stream',
          text: ['(7500,)\n', '(2500,)\n'],
        },
      ],
      source: [
        'import numpy as np\n',
        'import sklearn.cross_validation as skcv\n',
        '#x = np.linspace(0, 5*np.pi, num=10000, dtype=np.float32)\n',
        'x = np.linspace(0, 4*np.pi, num=10000, dtype=np.float32)\n',
        'y = np.cos(x)\n',
        '\n',
        'train, test = skcv.train_test_split(np.arange(x.shape[0]))\n',
        'print train.shape\n',
        'print test.shape',
      ],
    },
    {
      cell_type: 'code',
      execution_count: 3,
      metadata: {
        collapsed: false,
        jupyter: {
          outputs_hidden: false,
        },
      },
      outputs: [
        {
          data: {
            'text/plain': ['[<matplotlib.lines.Line2D at 0x7fb588176b90>]'],
          },
          execution_count: 3,
          metadata: {},
          output_type: 'execute_result',
        },
        {
          data: {
            'image/png':
              'iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAG0lEQVR4nGIJn1mo28/GzPDiV+yTNYAAAAD//yPBBfrGshAGAAAAAElFTkSuQmCC',
            'text/plain': ['<matplotlib.figure.Figure at 0x7fb58e57c850>'],
          },
          metadata: {},
          output_type: 'display_data',
        },
      ],
      source: ['import pylab as pl\n', '%matplotlib inline\n', 'pl.plot(x, y)'],
    },
    {
      cell_type: 'markdown',
      metadata: {},
      source: '# markdown as a string',
    },
  ],
});
