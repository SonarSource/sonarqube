import React from 'react';

export default React.createClass({
  renderLogo() {
    let url = this.props.logoUrl || `${window.baseUrl}/images/logo.svg`;
    let width = this.props.logoWidth || 100;
    let height = 30;
    let title = window.t('layout.sonar.slogan');
    return <img src={url}
                width={width}
                height={height}
                alt={title}
                title={title}/>;
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
