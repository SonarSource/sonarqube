import React from 'react';

const headerHeight = 48;

const containerCss = {
  minWidth: 320,
  maxWidth: 800,
  marginLeft: 'auto',
  marginRight: 'auto',
  paddingLeft: 16,
  paddingRight: 16
};

export default function Layout(props) {
  return (
    <div>
      <header css={{ height: headerHeight, backgroundColor: '#262626' }}>
        <div
          css={{
            display: 'flex',
            alignItems: 'center',
            alignContent: 'center',
            height: headerHeight,
            ...containerCss
          }}>
          <a href="/">
            <img
              alt="Continuous Code Quality"
              css={{ verticalAlign: 'top', margin: 0 }}
              height="30"
              src="https://next.sonarqube.com/sonarqube/images/logo.svg?v=6.6"
              title="Continuous Code Quality"
              width="83"
            />
          </a>
        </div>
      </header>

      <div css={containerCss}>{props.children()}</div>
    </div>
  );
}
