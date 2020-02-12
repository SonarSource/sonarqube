/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { scrollToElement } from 'sonar-ui-common/helpers/scrolling';
import { getSecurityHotspotDetails } from '../../../api/security-hotspots';
import { BranchLike } from '../../../types/branch-like';
import { Hotspot } from '../../../types/security-hotspots';
import HotspotViewerRenderer from './HotspotViewerRenderer';

interface Props {
  branchLike?: BranchLike;
  component: T.Component;
  hotspotKey: string;
  onUpdateHotspot: (hotspotKey: string) => Promise<void>;
  securityCategories: T.StandardSecurityCategories;
}

interface State {
  hotspot?: Hotspot;
  loading: boolean;
  commentVisible: boolean;
}

export default class HotspotViewer extends React.PureComponent<Props, State> {
  mounted = false;
  state: State;
  commentTextRef: React.RefObject<HTMLTextAreaElement>;
  parentScrollRef: React.RefObject<HTMLDivElement>;

  constructor(props: Props) {
    super(props);
    this.commentTextRef = React.createRef<HTMLTextAreaElement>();
    this.parentScrollRef = React.createRef<HTMLDivElement>();
    this.state = { loading: false, commentVisible: false };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchHotspot();
  }

  componentDidUpdate(prevProps: Props, prevState: State) {
    if (prevProps.hotspotKey !== this.props.hotspotKey) {
      this.fetchHotspot();
    }
    if (this.commentTextRef.current && !prevState.commentVisible && this.state.commentVisible) {
      this.commentTextRef.current.focus({ preventScroll: true });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchHotspot = () => {
    this.setState({ loading: true });
    return getSecurityHotspotDetails(this.props.hotspotKey)
      .then(hotspot => {
        if (this.mounted) {
          this.setState({ hotspot, loading: false });
        }
        return hotspot;
      })
      .catch(() => this.mounted && this.setState({ loading: false }));
  };

  handleHotspotUpdate = () => {
    return this.fetchHotspot().then((hotspot?: Hotspot) => {
      if (hotspot) {
        return this.props.onUpdateHotspot(hotspot.key);
      }
    });
  };

  handleOpenComment = () => {
    this.setState({ commentVisible: true });
    if (this.commentTextRef.current) {
      // Edge case when the comment is already open and unfocus.
      this.commentTextRef.current.focus({ preventScroll: true });
    }
    if (this.commentTextRef.current && this.parentScrollRef.current) {
      scrollToElement(this.commentTextRef.current, {
        parent: this.parentScrollRef.current,
        bottomOffset: 100
      });
    }
  };

  handleCloseComment = () => {
    this.setState({ commentVisible: false });
  };

  render() {
    const { branchLike, component, securityCategories } = this.props;
    const { hotspot, loading, commentVisible } = this.state;

    return (
      <HotspotViewerRenderer
        branchLike={branchLike}
        component={component}
        commentTextRef={this.commentTextRef}
        commentVisible={commentVisible}
        hotspot={hotspot}
        loading={loading}
        onCloseComment={this.handleCloseComment}
        onOpenComment={this.handleOpenComment}
        onUpdateHotspot={this.handleHotspotUpdate}
        parentScrollRef={this.parentScrollRef}
        securityCategories={securityCategories}
      />
    );
  }
}
