/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
//@flow
import React from 'react';
import ReactDOM from 'react-dom';
import debounce from 'lodash/debounce';
import { translate } from '../../../helpers/l10n';
import TagsList from '../../../components/tags/TagsList';
import TagsSelector from '../../../components/tags/TagsSelector';

type Props = {
  component: {
    id: string,
    key: string,
    qualifier: string,
    tags: Array<string>,
    configuration?: {
      showSettings?: boolean
    }
  }
};

type State = {
  popupOpen: boolean,
  popupPosition: { top: number, right: number },
  searchResult: Array<string>
};

export default class MetaTags extends React.PureComponent {
  tagsSelector: HTMLElement;
  props: Props;
  state: State = {
    popupOpen: false,
    popupPosition: {
      top: 0,
      right: 0
    },
    searchResult: []
  };

  constructor(props: Props) {
    super(props);
    this.onSearch = debounce(this.onSearch, 250);
  }

  componentDidMount() {
    const buttonPos = this.refs.tagslist.getBoundingClientRect();
    const cardPos = this.refs.card.getBoundingClientRect();
    this.setState({ popupPosition: this.getPopupPos(buttonPos, cardPos) });

    window.addEventListener('keydown', this.handleKey, false);
    window.addEventListener('click', this.handleOutsideClick, false);
  }

  componentWillUnmount() {
    window.removeEventListener('keydown', this.handleKey);
    window.removeEventListener('click', this.handleOutsideClick);
  }

  handleKey = (evt: SyntheticInputEvent) => {
    // Escape key
    if (evt.keyCode === 27) {
      this.setState({ popupOpen: false });
    }
  };

  handleOutsideClick = (evt: SyntheticInputEvent) => {
    const domNode = ReactDOM.findDOMNode(this.refs.tagsSelector);
    if (!domNode || !domNode.contains(evt.target)) {
      this.setState({ popupOpen: false });
    }
  };

  handleClick = (evt: MouseEvent) => {
    evt.stopPropagation();
    this.setState(state => ({ popupOpen: !state.popupOpen }));
  };

  onSearch = (query: string) => {
    console.log('search', query);// eslint-disable-line
    this.setState({ searchResult: [query] });
  };

  onSelect = (tag: string) => {
    console.log(`select`, tag); // eslint-disable-line
  };

  onUnselect = (tag: string) => {
    console.log('unselect', tag);// eslint-disable-line
  };

  getPopupPos(eltPos: { height: number, width: number }, containerPos: { width: number }) {
    return {
      top: eltPos.height,
      right: containerPos.width - eltPos.width
    };
  }

  render() {
    const { tags, configuration } = this.props.component;
    const { popupOpen, popupPosition } = this.state;

    return (
      <div className="overview-meta-card" style={{ position: 'relative' }} ref="card">
        <button className="button-link" onClick={this.handleClick} ref="tagslist">
          <TagsList
            tags={tags.length ? tags : [translate('no_tags')]}
            allowUpdate={configuration && configuration.showSettings}
            allowMultiLine={true}
          />
        </button>
        <TagsSelector
          ref="tagsSelector"
          open={popupOpen}
          position={popupPosition}
          tags={this.state.searchResult}
          selectedTags={tags}
          onSearch={this.onSearch}
          onSelect={this.onSelect}
          onUnselect={this.onUnselect}
          popupCustomClass="bubble-popup-bottom-right"
        />
      </div>
    );
  }
}
