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
import DocMarkdownBlock from './DocMarkdownBlock';
import HelpTooltip from '../controls/HelpTooltip';

interface Props {
  className?: string;
  children?: React.ReactNode;
  /** Key of the documentation chunk */
  doc: string;
}

interface State {
  content?: string;
  loading: boolean;
  open: boolean;
}

export default class DocTooltip extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: false, open: false };

  componentDidMount() {
    this.mounted = true;
    document.addEventListener('scroll', this.close, true);
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.doc !== this.props.doc) {
      this.setState({ content: undefined, loading: false, open: false });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    document.removeEventListener('scroll', this.close, true);
  }

  fetchContent = () => {
    this.setState({ loading: true });
    import(`Docs/tooltips/${this.props.doc}.md`).then(
      ({ default: content }) => {
        if (this.mounted) {
          this.setState({ content, loading: false });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  close = () => {
    this.setState({ open: false });
  };

  renderOverlay() {
    if (this.state.loading) {
      return (
        <div className="abs-width-300">
          <i className="spinner" />
        </div>
      );
    }

    return <DocMarkdownBlock className="cut-margins abs-width-300" content={this.state.content} />;
  }

  render() {
    return (
      <HelpTooltip
        className={this.props.className}
        onShow={this.fetchContent}
        overlay={this.renderOverlay()}>
        {this.props.children}
      </HelpTooltip>
    );
  }
}
