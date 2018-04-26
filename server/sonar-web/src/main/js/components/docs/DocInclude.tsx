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

interface Props {
  path: string;
}

interface State {
  content?: string;
}

export default class DocInclude extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {};

  componentDidMount() {
    this.mounted = true;
    this.fetchContent();
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.path !== this.props.path) {
      this.setState({ content: undefined });
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.path !== this.props.path) {
      this.fetchContent();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchContent = () => {
    import(`Docs/${this.props.path}.md`).then(
      ({ default: content }) => {
        if (this.mounted) {
          this.setState({ content });
        }
      },
      () => {}
    );
  };

  render() {
    return <DocMarkdownBlock content={this.state.content} />;
  }
}
