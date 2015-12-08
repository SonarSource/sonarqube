import ReactDOM from 'react-dom';

export const ResizeMixin = {
  componentDidMount () {
    if (this.isResizable()) {
      this.handleResize();
      window.addEventListener('resize', this.handleResize);
    }
  },

  componentWillUnmount () {
    if (this.isResizable()) {
      window.removeEventListener('resize', this.handleResize);
    }
  },

  handleResize () {
    let boundingClientRect = ReactDOM.findDOMNode(this).parentNode.getBoundingClientRect();
    let newWidth = this.props.width || boundingClientRect.width;
    let newHeight = this.props.height || boundingClientRect.height;
    this.setState({ width: newWidth, height: newHeight });
  },

  isResizable() {
    return !this.props.width || !this.props.height;
  }
};
