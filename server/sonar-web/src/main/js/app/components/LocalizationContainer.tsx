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
import { addLocaleData, IntlProvider, Locale } from 'react-intl';
import GlobalLoading from './GlobalLoading';
import { DEFAULT_LANGUAGE, requestMessages } from '../../helpers/l10n';

interface Props {
  children?: any;
}

interface State {
  loading: boolean;
  lang?: string;
}

export default class LocalizationContainer extends React.PureComponent<Props, State> {
  mounted = false;

  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    requestMessages().then(this.bundleLoaded, this.bundleLoaded);
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  bundleLoaded = (lang: string) => {
    const langToLoad = lang || DEFAULT_LANGUAGE;

    // No need to load english bundle, it's coming wiht react-intl, use english if it fails
    if (langToLoad !== 'en') {
      import('react-intl/locale-data/' + langToLoad).then(
        i => this.updateLang(langToLoad, i),
        () => {}
      );
    } else {
      this.setState({ loading: false, lang: langToLoad });
    }
  };

  updateLang = (lang: string, intlBundle: Locale[]) => {
    if (this.mounted) {
      addLocaleData(intlBundle);
      this.setState({ loading: false, lang });
    }
  };

  render() {
    if (this.state.loading) {
      return <GlobalLoading />;
    }
    return (
      <IntlProvider
        locale={this.state.lang || DEFAULT_LANGUAGE}
        defaultLocale={this.state.lang || DEFAULT_LANGUAGE}>
        {this.props.children}
      </IntlProvider>
    );
  }
}
