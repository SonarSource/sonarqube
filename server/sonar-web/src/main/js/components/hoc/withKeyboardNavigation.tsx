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
import { getWrappedDisplayName } from './utils';
import PageActions from '../ui/PageActions';

export interface WithKeyboardNavigationProps {
  components?: T.ComponentMeasure[];
  cycle?: boolean;
  isFile?: boolean;
  onEndOfList?: () => void;
  onGoToParent?: () => void;
  onHighlight?: (item: T.ComponentMeasure) => void;
  onSelect?: (item: T.ComponentMeasure) => void;
  selected?: T.ComponentMeasure;
}

const KEY_SCOPE = 'key_nav';

export default function withKeyboardNavigation<P>(
  WrappedComponent: React.ComponentClass<P & Partial<WithKeyboardNavigationProps>>
) {
  return class Wrapper extends React.Component<P & WithKeyboardNavigationProps> {
    static displayName = getWrappedDisplayName(WrappedComponent, 'withKeyboardNavigation');

    componentDidMount() {
      this.attachShortcuts();
    }

    componentWillUnmount() {
      this.detachShortcuts();
    }

    attachShortcuts = () => {
      key.setScope(KEY_SCOPE);
      key('up', KEY_SCOPE, () => {
        return this.skipIfFile(this.handleHighlightPrevious);
      });
      key('down', KEY_SCOPE, () => {
        return this.skipIfFile(this.handleHighlightNext);
      });
      key('right,enter', KEY_SCOPE, () => {
        return this.skipIfFile(this.handleSelectCurrent);
      });
      key('left', KEY_SCOPE, () => {
        this.handleSelectParent();
        return false; // always hijack left
      });
    };

    detachShortcuts = () => {
      key.deleteScope(KEY_SCOPE);
    };

    getCurrentIndex = () => {
      const { selected, components = [] } = this.props;
      return selected ? components.findIndex(component => component.key === selected.key) : -1;
    };

    skipIfFile = (handler: () => void) => {
      if (this.props.isFile) {
        return true;
      } else {
        handler();
        return false;
      }
    };

    handleHighlightNext = () => {
      if (this.props.onHighlight === undefined) {
        return;
      }

      const { components = [], cycle } = this.props;
      const index = this.getCurrentIndex();
      const first = cycle ? 0 : index;

      this.props.onHighlight(
        index < components.length - 1 ? components[index + 1] : components[first]
      );

      if (index + 1 === components.length - 1 && this.props.onEndOfList) {
        this.props.onEndOfList();
      }
    };

    handleHighlightPrevious = () => {
      if (this.props.onHighlight === undefined) {
        return;
      }
      const { components = [], cycle } = this.props;
      const index = this.getCurrentIndex();
      const last = cycle ? components.length - 1 : index;

      this.props.onHighlight(index > 0 ? components[index - 1] : components[last]);
    };

    handleSelectCurrent = () => {
      if (this.props.onSelect === undefined) {
        return;
      }

      const { selected } = this.props;
      if (selected !== undefined) {
        this.props.onSelect(selected as T.ComponentMeasure);
      }
    };

    handleSelectNext = () => {
      if (this.props.onSelect === undefined) {
        return;
      }

      const { components = [] } = this.props;
      const index = this.getCurrentIndex();

      if (index !== -1 && index < components.length - 1) {
        this.props.onSelect(components[index + 1]);
      }
    };

    handleSelectParent = () => {
      if (this.props.onGoToParent !== undefined) {
        this.props.onGoToParent();
      }
    };

    handleSelectPrevious = () => {
      if (this.props.onSelect === undefined) {
        return;
      }

      const { components = [] } = this.props;
      const index = this.getCurrentIndex();

      if (components.length && index > 0) {
        this.props.onSelect(components[index - 1]);
      }
    };

    render() {
      return (
        <>
          <PageActions showShortcuts={!this.props.isFile} />

          <WrappedComponent {...this.props} />
        </>
      );
    }
  };
}
