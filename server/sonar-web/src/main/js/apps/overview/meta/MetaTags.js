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
import { translate } from '../../../helpers/l10n';
import TagsList from '../../../components/tags/TagsList';
import ProjectTagsSelectorContainer from '../../projects/components/ProjectTagsSelectorContainer';

type Props = {
  component: {
    key: string,
    tags: Array<string>,
    configuration?: {
      showSettings?: boolean
    }
  }
};

type State = {
  popupOpen: boolean,
  popupPosition: { top: number, right: number }
};

export default class MetaTags extends React.PureComponent {
  card: HTMLElement;
  tagsList: HTMLElement;
  tagsSelector: HTMLElement;
  props: Props;
  state: State = {
    popupOpen: false,
    popupPosition: {
      top: 0,
      right: 0
    }
  };

  componentDidMount() {
    if (this.canUpdateTags()) {
      const buttonPos = this.tagsList.getBoundingClientRect();
      const cardPos = this.card.getBoundingClientRect();
      this.setState({ popupPosition: this.getPopupPos(buttonPos, cardPos) });

      window.addEventListener('keydown', this.handleKey, false);
      window.addEventListener('click', this.handleOutsideClick, false);
    }
  }

  componentWillUnmount() {
    window.removeEventListener('keydown', this.handleKey);
    window.removeEventListener('click', this.handleOutsideClick);
  }

  handleKey = (evt: KeyboardEvent) => {
    // Escape key
    if (evt.keyCode === 27) {
      this.setState({ popupOpen: false });
    }
  };

  handleOutsideClick = (evt: SyntheticInputEvent) => {
    if (!this.tagsSelector || !this.tagsSelector.contains(evt.target)) {
      this.setState({ popupOpen: false });
    }
  };

  handleClick = (evt: MouseEvent) => {
    evt.stopPropagation();
    this.setState(state => ({ popupOpen: !state.popupOpen }));
  };

  canUpdateTags() {
    const { configuration } = this.props.component;
    return configuration && configuration.showSettings;
  }

  getPopupPos(eltPos: { height: number, width: number }, containerPos: { width: number }) {
    return {
      top: eltPos.height,
      right: containerPos.width - eltPos.width
    };
  }

  render() {
    const { tags, key } = this.props.component;
    const { popupOpen, popupPosition } = this.state;

    if (this.canUpdateTags()) {
      return (
        <div className="overview-meta-card overview-meta-tags" ref={card => this.card = card}>
          <button
            className="button-link"
            onClick={this.handleClick}
            ref={tagsList => this.tagsList = tagsList}>
            <TagsList tags={tags.length ? tags : [translate('no_tags')]} allowUpdate={true} />
          </button>
          {popupOpen &&
            <div ref={tagsSelector => this.tagsSelector = tagsSelector}>
              <ProjectTagsSelectorContainer
                position={popupPosition}
                project={key}
                selectedTags={tags}
              />
            </div>}
        </div>
      );
    } else {
      return (
        <div className="overview-meta-card overview-meta-tags">
          <TagsList
            tags={tags.length ? tags : [translate('no_tags')]}
            allowUpdate={false}
            allowMultiLine={true}
          />
        </div>
      );
    }
  }
}
