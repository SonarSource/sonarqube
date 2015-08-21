import React from 'react';

export default React.createClass({
  renderLogo() {
    const url = this.props.logoUrl || `${window.baseUrl}/images/logo.svg`;
    const width = this.props.logoWidth || null;
    const title = window.t('layout.sonar.slogan');
    return <img src={url} width={width} height="30" alt={title} title={title}/>
  },

  render() {
    const homeUrl = window.baseUrl + '/';
    const homeLinkClassName = 'navbar-brand' + (this.props.logoUrl ? ' navbar-brand-custom' : '');
    return (
        <div className="navbar-header">
          <a className={homeLinkClassName} href={homeUrl}>{this.renderLogo()}</a>
        </div>
    );
  }
});
