(function() {

  var defaultActions = ['comment', 'assign', 'assign_to_me', 'plan', 'set_severity'];

  Handlebars.registerHelper('capitalize', function(string) {
    return string.charAt(0).toUpperCase() + string.slice(1).toLowerCase();
  });

  Handlebars.registerHelper('severityIcon', function(severity) {
    return new Handlebars.SafeString(
        '<i class="icon-severity-' + severity.toLowerCase() + '"></i>'
    );
  });

  Handlebars.registerHelper('statusIcon', function(status) {
    return new Handlebars.SafeString(
        '<i class="icon-status-' + status.toLowerCase() + '"></i>'
    );
  });

  Handlebars.registerHelper('resolutionIcon', function(resolution) {
    return new Handlebars.SafeString(
        '<i class="icon-resolution-' + resolution.toLowerCase() + '"></i>'
    );
  });

  Handlebars.registerHelper('eq', function(v1, v2, options) {
    return v1 == v2 ? options.fn(this) : options.inverse(this);
  });

  Handlebars.registerHelper('notEq', function(v1, v2, options) {
    return v1 != v2 ? options.fn(this) : options.inverse(this);
  });

  Handlebars.registerHelper('all', function() {
    var args = Array.prototype.slice.call(arguments, 0, -1),
        options = arguments[arguments.length - 1],
        all = args.reduce(function(prev, current) {
          return prev && current;
        }, true);
    return all ? options.fn(this) : options.inverse(this);
  });

  Handlebars.registerHelper('any', function() {
    var args = Array.prototype.slice.call(arguments, 0, -1),
        options = arguments[arguments.length - 1],
        all = args.reduce(function(prev, current) {
          return prev || current;
        }, true);
    return all ? options.fn(this) : options.inverse(this);
  });

  Handlebars.registerHelper('inArray', function(array, element, options) {
    if (array.indexOf(element) !== -1) {
      return options.fn(this);
    } else {
      return options.inverse(this);
    }
  });

  Handlebars.registerHelper('ifNotEmpty', function() {
    var args = Array.prototype.slice.call(arguments, 0, -1),
        options = arguments[arguments.length - 1],
        notEmpty = args.reduce(function(prev, current) {
          return prev || (current && current.length > 0);
        }, false);
    return notEmpty ? options.fn(this) : '';
  });

  Handlebars.registerHelper('dashboardUrl', function(componentKey, componentQualifier) {
    var url = '/dashboard/index/' + decodeURIComponent(componentKey);
    if (componentQualifier === 'FIL' || componentQualifier === 'CLA') {
      url += '?metric=sqale_index';
    }
    return url;
  });

  Handlebars.registerHelper('translate', function(key, prefix) {
    var args = Array.prototype.slice.call(arguments, 0, -1),
        tokens = args.reduce(function(prev, current) {
          return prev.concat(current.split('.'));
        }, []),
        start = window.SS.phrases;

    return tokens.reduce(function(prev, current) {
      return current ? prev[current] : prev;
    }, start);
  });

  Handlebars.registerHelper('pluginActions', function(actions, options) {
    var pluginActions = _.difference(actions, defaultActions);
    return pluginActions.reduce(function(prev, current) {
      return prev + options.fn(current);
    }, '');
  });

  Handlebars.registerHelper('ifHasExtraTransitions', function(transitions, options) {
    if (transitions && transitions.length > 1) {
      return options.fn(this);
    } else {
      return '';
    }
  });

  Handlebars.registerHelper('ifHasExtraActions', function(actions, options) {
    var actionsLeft = _.difference(actions, _.without(defaultActions, 'set_severity'));
    if (actionsLeft.length > 0) {
      return options.fn(this);
    } else {
      return '';
    }
  });

  Handlebars.registerHelper('withFirst', function(list, options) {
    if (list && list.length > 0) {
      return options.fn(list[0]);
    } else {
      return '';
    }
  });

  Handlebars.registerHelper('withoutFirst', function(list, options) {
    if (list && list.length > 1) {
      return list.slice(1).reduce(function(prev, current) {
        return prev + options.fn(current);
      }, '');
    } else {
      return '';
    }
  });

  Handlebars.registerHelper('sources', function(source, scm, options) {
    var sources = _.map(source, function(code, line) {
      return {
        lineNumber: line,
        code: code,
        scm: (scm && scm[line]) ? { author: scm[line][0], date: scm[line][1] } : undefined
      }
    });

    return sources.reduce(function(prev, current, index) {
      return prev + options.fn(_.extend({ first: index === 0 }, current));
    }, '');
  });

})();
