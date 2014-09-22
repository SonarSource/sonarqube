var express = require('express'),
    path = require('path'),
    errorhandler = require('errorhandler'),
    serveStatic = require('serve-static'),
    app = express();

app.set('views', __dirname + '/views');
app.set('view engine', 'jade');

app.use(errorhandler({ dumpExceptions: true, showStack: true }));

// Serve static files
app.use('/', serveStatic(path.join(__dirname, '../../../webapp')));

app.get('/pages/:page', function (req, res) {
  res.render(req.param('page'));
});

// Get the port from environment variables
var port = process.env.PORT || 8000;

app.listen(port);

console.log('Server running on port %d', port);
