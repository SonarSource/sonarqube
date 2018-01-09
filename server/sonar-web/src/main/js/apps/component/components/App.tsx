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
import SourceViewer from '../../../components/SourceViewer/SourceViewer';

interface Props {
  location: {
    query: {
      branch?: string;
      id: string;
      line?: string;
    };
  };
}

export default class App extends React.PureComponent<Props> {
  scrollToLine = () => {
    const { line } = this.props.location.query;
    if (line) {
      const row = document.querySelector(`.source-line[data-line-number="${line}"]`);
      if (row) {
        const rect = row.getBoundingClientRect();
        const topOffset = window.innerHeight / 2 - 60;
        const goal = rect.top - topOffset;
        window.scrollTo(0, goal);
      }
    }
  };

  render() {
    const { branch, id, line } = this.props.location.query;

    const finalLine = line != null ? Number(line) : null;

    return (
      <div className="page page-limited">
        <SourceViewer
          aroundLine={finalLine}
          branch={branch}
          component={id}
          highlightedLine={finalLine}
          onLoaded={this.scrollToLine}
        />
      </div>
    );
  }
}
