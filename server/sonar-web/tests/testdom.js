if (typeof document === 'undefined') {
  var jsdom = require('jsdom').jsdom;
  var markup = '<html><body></body></html>';
  global.document = jsdom(markup);
  global.window = document.parentWindow;
  global.navigator = { userAgent: 'node.js' };
}
