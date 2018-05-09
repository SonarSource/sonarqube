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
import MetaTagsSelector from './MetaTagsSelector';
import { setProjectTags } from '../../../api/components';
import { translate } from '../../../helpers/l10n';
import TagsList from '../../../components/tags/TagsList';
import { Component } from '../../../app/types';
import { Button } from '../../../components/ui/buttons';
import Dropdown from '../../../components/controls/Dropdown';
import { PopupPlacement } from '../../../components/ui/popups';

interface Props {
  component: Component;
  onComponentChange: (changes: {}) => void;
}

export default class MetaTags extends React.PureComponent<Props> {
  card?: HTMLDivElement | null;
  tagsList?: HTMLElement | null;
  tagsSelector?: HTMLDivElement | null;

  canUpdateTags = () => {
    const { configuration } = this.props.component;
    return configuration && configuration.showSettings;
  };

  getPopupPos = (eltPos: ClientRect, containerPos: ClientRect) => ({
    top: eltPos.height,
    right: containerPos.width - eltPos.width
  });

  handleSetProjectTags = (values: string[]) => {
    setProjectTags({
      project: this.props.component.key,
      tags: values.join(',')
    }).then(() => this.props.onComponentChange({ tags: values }), () => {});
  };

  render() {
    const { key } = this.props.component;
    const tags = this.props.component.tags || [];

    if (this.canUpdateTags()) {
      return (
        <div className="big-spacer-top overview-meta-tags" ref={card => (this.card = card)}>
          <Dropdown
            closeOnClick={false}
            closeOnClickOutside={true}
            overlay={
              <MetaTagsSelector
                project={key}
                selectedTags={tags}
                setProjectTags={this.handleSetProjectTags}
              />
            }
            overlayPlacement={PopupPlacement.BottomLeft}>
            <Button
              className="button-link"
              innerRef={tagsList => (this.tagsList = tagsList)}
              stopPropagation={true}>
              <TagsList allowUpdate={true} tags={tags.length ? tags : [translate('no_tags')]} />
            </Button>
          </Dropdown>
        </div>
      );
    } else {
      return (
        <div className="big-spacer-top overview-meta-tags">
          <TagsList
            allowUpdate={false}
            className="note"
            tags={tags.length ? tags : [translate('no_tags')]}
          />
        </div>
      );
    }
  }
}
