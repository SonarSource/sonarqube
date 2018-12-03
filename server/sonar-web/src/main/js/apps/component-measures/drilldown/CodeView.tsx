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
import * as key from 'keymaster';
import SourceViewer from '../../../components/SourceViewer/SourceViewer';

interface Props {
  branchLike?: T.BranchLike;
  component: T.ComponentMeasure;
  components: T.ComponentMeasureEnhanced[];
  leakPeriod?: T.Period;
  selectedIdx?: number;
  updateSelected: (component: string) => void;
}

export default class CodeView extends React.PureComponent<Props> {
  componentDidMount() {
    this.attachShortcuts();
  }

  componentWillUnmount() {
    this.detachShortcuts();
  }

  attachShortcuts() {
    key('j', 'measures-files', () => {
      this.selectNext();
      return false;
    });
    key('k', 'measures-files', () => {
      this.selectPrevious();
      return false;
    });
  }

  detachShortcuts() {
    ['j', 'k'].forEach(action => key.unbind(action, 'measures-files'));
  }

  selectPrevious = () => {
    const { selectedIdx } = this.props;
    if (selectedIdx != null && selectedIdx > 0) {
      const prevComponent = this.props.components[selectedIdx - 1];
      if (prevComponent) {
        this.props.updateSelected(prevComponent.key);
      }
    }
  };

  selectNext = () => {
    const { components, selectedIdx } = this.props;
    if (selectedIdx != null && selectedIdx < components.length - 1) {
      const nextComponent = components[selectedIdx + 1];
      if (nextComponent) {
        this.props.updateSelected(nextComponent.key);
      }
    }
  };

  render() {
    const { branchLike, component } = this.props;
    return <SourceViewer branchLike={branchLike} component={component.key} />;
  }
}
