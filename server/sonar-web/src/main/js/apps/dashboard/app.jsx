export default {
  start(options) {
    let widgets = window.widgets || [];
    widgets.forEach(widget => {
      require([`widgets/${widget.name}/widget`], Widget => {
        new Widget(widget.options);
      });
    });
  }
};
