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
import * as classNames from 'classnames';
import DocMarkdownBlock from './DocMarkdownBlock';
import HelpIcon from '../icons-components/HelpIcon';
import Tooltip from '../controls/Tooltip';
import OutsideClickHandler from '../controls/OutsideClickHandler';
import * as theme from '../../app/theme';

interface Props {
  className?: string;
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
    import(`Docs/${this.props.doc}.md`).then(
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

  handleHelpClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    if (!this.state.open && !this.state.loading && this.state.content === undefined) {
      this.fetchContent();
    }

    if (this.state.open) {
      this.setState({ open: false });
    } else {
      // defer opening to not trigger OutsideClickHandler.onClickOutside callback
      setTimeout(() => {
        this.setState({ open: true });
      }, 0);
    }
  };

  renderOverlay() {
    if (this.state.loading) {
      return (
        <div className="abs-width-300">
          <i className="spinner" />
        </div>
      );
    }

    return (
      <OutsideClickHandler onClickOutside={this.close}>
        {({ ref }) => (
          <div ref={ref}>
            <DocMarkdownBlock className="cut-margins abs-width-300" content={this.state.content} />
          </div>
        )}
      </OutsideClickHandler>
    );
  }

  render() {
    return (
      <div className={classNames('display-flex-center', this.props.className)}>
        <Tooltip
          classNameSpace="popup"
          overlay={this.renderOverlay()}
          visible={this.state.content !== undefined && this.state.open}>
          <a
            className="display-flex-center link-no-underline"
            href="#"
            onClick={this.handleHelpClick}>
            <HelpIcon fill={theme.gray80} size={12} />
          </a>
        </Tooltip>
      </div>
    );
  }
}
