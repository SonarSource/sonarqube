/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { Location } from 'history';
import SourceViewer from '../../../components/SourceViewer/SourceViewer';
import withKeyboardNavigation from '../../../components/hoc/withKeyboardNavigation';
import { scrollToElement } from '../../../helpers/scrolling';

interface Props {
  branchLike?: T.BranchLike;
  component: string;
  location: Pick<Location, 'query'>;
}

export class SourceViewerWrapper extends React.PureComponent<Props> {
  scrollToLine = () => {
    const { location } = this.props;
    const { line } = location.query;

    if (line) {
      const row = document.querySelector(`.source-line[data-line-number="${line}"]`);
      if (row) {
        scrollToElement(row, { smooth: false, bottomOffset: window.innerHeight / 2 - 60 });
      }
    }
  };

  render() {
    const { branchLike, component, location } = this.props;
    const { line } = location.query;
    const finalLine = line ? Number(line) : undefined;

    return (
      <SourceViewer
        aroundLine={finalLine}
        branchLike={branchLike}
        component={component}
        highlightedLine={finalLine}
        onLoaded={this.scrollToLine}
      />
    );
  }
}

export default withKeyboardNavigation(SourceViewerWrapper);
