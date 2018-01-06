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
// @flow
import React from 'react';
import key from 'keymaster';
import SourceViewer from '../../../components/SourceViewer/SourceViewer';
/*:: import type { ComponentEnhanced, Paging, Period } from '../types'; */
/*:: import type { Metric } from '../../../store/metrics/actions'; */

/*:: type Props = {|
  branch?: string,
  component: ComponentEnhanced,
  components: Array<ComponentEnhanced>,
  leakPeriod?: Period,
  metric: Metric,
  selectedIdx: ?number,
  updateSelected: string => void,
|}; */

export default class CodeView extends React.PureComponent {
  /*:: props: Props; */

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
    ['j', 'k'].map(action => key.unbind(action, 'measures-files'));
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
    const { branch, component } = this.props;
    return <SourceViewer branch={branch} component={component.key} />;
  }
}
