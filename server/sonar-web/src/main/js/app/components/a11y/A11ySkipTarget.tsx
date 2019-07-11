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
import { translate } from 'sonar-ui-common/helpers/l10n';
import { A11yContext } from './A11yContext';

interface Props {
  anchor: string;
  label?: string;
  weight?: number;
}

export default function A11ySkipTarget(props: Props) {
  return (
    <A11yContext.Consumer>
      {({ addA11ySkipLink, removeA11ySkipLink }) => (
        <A11ySkipTargetInner
          addA11ySkipLink={addA11ySkipLink}
          removeA11ySkipLink={removeA11ySkipLink}
          {...props}
        />
      )}
    </A11yContext.Consumer>
  );
}

interface InnerProps {
  addA11ySkipLink: (link: T.A11ySkipLink) => void;
  removeA11ySkipLink: (link: T.A11ySkipLink) => void;
}

export class A11ySkipTargetInner extends React.PureComponent<Props & InnerProps> {
  componentDidMount() {
    this.props.addA11ySkipLink(this.getLink());
  }

  componentWillUnmount() {
    this.props.removeA11ySkipLink(this.getLink());
  }

  getLink = (): T.A11ySkipLink => {
    const { anchor: key, label = translate('skip_to_content'), weight } = this.props;
    return { key, label, weight };
  };

  render() {
    const { anchor } = this.props;
    return <span id={`a11y_target__${anchor}`} />;
  }
}
