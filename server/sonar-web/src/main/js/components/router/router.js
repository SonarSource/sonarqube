let listener;


export const RouterMixin = {
  getDefaultProps() {
    return { urlRoot: '/' };
  },

  getInitialState() {
    return { route: this.getRoute() };
  },

  getRoute() {
    let path = window.location.pathname;
    if (path.indexOf(this.props.urlRoot) === 0) {
      return path.substr(this.props.urlRoot.length);
    } else {
      return null;
    }
  },

  componentDidMount () {
    listener = this;
    window.addEventListener('popstate', this.handleRouteChange);
  },

  componentWillUnmount() {
    window.removeEventListener('popstate', this.handleRouteChange);
  },

  handleRouteChange() {
    let route = this.getRoute();
    this.setState({ route });
  },

  navigate (route) {
    let url = this.props.urlRoot + route + window.location.search + window.location.hash;
    window.history.pushState({ route }, document.title, url);
    this.setState({ route });
  }
};


export function navigate (route) {
  if (listener) {
    listener.navigate(route);
  }
}
