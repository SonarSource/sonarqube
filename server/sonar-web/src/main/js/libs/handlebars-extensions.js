(function () {
  Handlebars.registerHelper('log', function () {
    /* eslint no-console: 0 */
    var args = Array.prototype.slice.call(arguments, 0, -1);
    console.log.apply(console, args);
  });

  Handlebars.registerHelper('link', function () {
    var url = Array.prototype.slice.call(arguments, 0, -1).join('');
    return baseUrl + url;
  });

  Handlebars.registerHelper('componentPermalink', function (componentKey) {
    return baseUrl + '/dashboard/index?id=' + encodeURIComponent(componentKey);
  });

  Handlebars.registerHelper('componentDashboardPermalink', function (componentKey, dashboardKey) {
    var params = [
      { key: 'id', value: componentKey },
      { key: 'did', value: dashboardKey }
    ];

    var matchPeriod = window.location.search.match(/period=(\d+)/);
    if (matchPeriod) {
       // If we have a match for period, check that it is not project-specific
      var period = parseInt(matchPeriod[1], 10);
      if (period <= 3) {
        params.push({ key: 'period', value: period });
      }
    }

    var query = params.map(function (p) {
      return p.key + '=' + encodeURIComponent(p.value);
    }).join('&');
    return baseUrl + '/dashboard/index?' + query;
  });

  Handlebars.registerHelper('componentBrowsePermalink', function (componentKey) {
    return baseUrl + '/components/index?id=' + encodeURIComponent(componentKey);
  });

  Handlebars.registerHelper('componentIssuesPermalink', function (componentKey) {
    return baseUrl + '/component_issues/index?id=' + encodeURIComponent(componentKey);
  });

  Handlebars.registerHelper('rulePermalink', function (ruleKey) {
    return baseUrl + '/coding_rules#rule_key=' + encodeURIComponent(ruleKey);
  });

  Handlebars.registerHelper('isActiveLink', function () {
    var args = Array.prototype.slice.call(arguments, 0, -1),
        options = arguments[arguments.length - 1],
        prefix = args.join(''),
        path = window.location.pathname,
        match = path.indexOf(baseUrl + prefix) === 0;
    return match ? options.fn(this) : options.inverse(this);
  });

  Handlebars.registerHelper('capitalize', function (string) {
    return string.charAt(0).toUpperCase() + string.slice(1).toLowerCase();
  });

  Handlebars.registerHelper('severityIcon', function (severity) {
    return new Handlebars.SafeString(
        '<i class="icon-severity-' + severity.toLowerCase() + '"></i>'
    );
  });

  Handlebars.registerHelper('severityHelper', function (severity) {
    return new Handlebars.SafeString(
        '<i class="icon-severity-' + severity.toLowerCase() + '"></i>&nbsp;' + t('severity', severity)
    );
  });

  Handlebars.registerHelper('statusIcon', function (status) {
    return new Handlebars.SafeString(
        '<i class="icon-status-' + status.toLowerCase() + '"></i>'
    );
  });

  Handlebars.registerHelper('statusHelper', function (status, resolution) {
    var s = '<i class="icon-status-' + status.toLowerCase() + '"></i>&nbsp;' + t('issue.status', status);
    if (resolution != null) {
      s = s + '&nbsp;(' + t('issue.resolution', resolution) + ')';
    }
    return new Handlebars.SafeString(s);
  });

  Handlebars.registerHelper('testStatusIcon', function (status) {
    return new Handlebars.SafeString(
        '<i class="icon-test-status-' + status.toLowerCase() + '"></i>'
    );
  });

  Handlebars.registerHelper('testStatusIconClass', function (status) {
    return new Handlebars.SafeString('' +
        'icon-test-status-' + status.toLowerCase()
    );
  });

  Handlebars.registerHelper('alertIconClass', function (alert) {
    return new Handlebars.SafeString(
        'icon-alert-' + alert.toLowerCase()
    );
  });

  Handlebars.registerHelper('qualifierIcon', function (qualifier) {
    return new Handlebars.SafeString(
        qualifier ? '<i class="icon-qualifier-' + qualifier.toLowerCase() + '"></i>' : ''
    );
  });

  Handlebars.registerHelper('default', function () {
    var args = Array.prototype.slice.call(arguments, 0, -1);
    return args.reduce(function (prev, current) {
      return prev != null ? prev : current;
    }, null);
  });

  Handlebars.registerHelper('show', function () {
    var args = Array.prototype.slice.call(arguments),
        ret = null;
    args.forEach(function (arg) {
      if (typeof arg === 'string' && ret == null) {
        ret = arg;
      }
    });
    return ret || '';
  });

  Handlebars.registerHelper('percent', function (value, total) {
    if (total > 0) {
      return '' + ((value || 0) / total * 100) + '%';
    } else {
      return '0%';
    }
  });

  Handlebars.registerHelper('eachIndex', function (context, options) {
    var ret = '';
    context.forEach(function (d, i) {
      var c = _.extend({ index: i }, d);
      ret += options.fn(c);
    });
    return ret;
  });

  Handlebars.registerHelper('eachChanged', function (context, property, options) {
    var ret = '';
    context.forEach(function (d, i) {
      var changed = i > 0 ? d[property] !== context[i - 1][property] : true,
          c = _.extend({ changed: changed }, d);
      ret += options.fn(c);
    });
    return ret;
  });

  Handlebars.registerHelper('eachEven', function (context, options) {
    var ret = '';
    context.forEach(function (d, i) {
      if (i % 2 === 0) {
        ret += options.fn(d);
      }
    });
    return ret;
  });

  Handlebars.registerHelper('eachOdd', function (context, options) {
    var ret = '';
    context.forEach(function (d, i) {
      if (i % 2 === 1) {
        ret += options.fn(d);
      }
    });
    return ret;
  });

  Handlebars.registerHelper('eq', function (v1, v2, options) {
    /* eslint eqeqeq: 0 */
    return v1 == v2 ? options.fn(this) : options.inverse(this);
  });

  Handlebars.registerHelper('notEq', function (v1, v2, options) {
    /* eslint eqeqeq: 0 */
    return v1 != v2 ? options.fn(this) : options.inverse(this);
  });

  Handlebars.registerHelper('gt', function (v1, v2, options) {
    return v1 > v2 ? options.fn(this) : options.inverse(this);
  });

  Handlebars.registerHelper('lt', function (v1, v2, options) {
    return v1 < v2 ? options.fn(this) : options.inverse(this);
  });

  Handlebars.registerHelper('notNull', function (value, options) {
    return value != null ? options.fn(this) : options.inverse(this);
  });

  Handlebars.registerHelper('isNull', function (value, options) {
    return value == null ? options.fn(this) : options.inverse(this);
  });

  Handlebars.registerHelper('notEmpty', function (array, options) {
    var cond = _.isArray(array) && array.length > 0;
    return cond ? options.fn(this) : options.inverse(this);
  });

  Handlebars.registerHelper('empty', function (array, options) {
    var cond = _.isArray(array) && array.length > 0;
    return cond ? options.inverse(this) : options.fn(this);
  });

  Handlebars.registerHelper('all', function () {
    var args = Array.prototype.slice.call(arguments, 0, -1),
        options = arguments[arguments.length - 1],
        all = args.reduce(function (prev, current) {
          return prev && current;
        }, true);
    return all ? options.fn(this) : options.inverse(this);
  });

  Handlebars.registerHelper('any', function () {
    var args = Array.prototype.slice.call(arguments, 0, -1),
        options = arguments[arguments.length - 1],
        any = args.reduce(function (prev, current) {
          return prev || current;
        }, false);
    return any ? options.fn(this) : options.inverse(this);
  });

  Handlebars.registerHelper('inArray', function (array, element, options) {
    if (_.isArray(array)) {
      if (array.indexOf(element) !== -1) {
        return options.fn(this);
      } else {
        return options.inverse(this);
      }
    }
  });

  Handlebars.registerHelper('ifNotEmpty', function () {
    var args = Array.prototype.slice.call(arguments, 0, -1),
        options = arguments[arguments.length - 1],
        notEmpty = args.reduce(function (prev, current) {
          return prev || (current && current.length > 0);
        }, false);
    return notEmpty ? options.fn(this) : options.inverse(this);
  });

  Handlebars.registerHelper('join', function (array, separator) {
    return array.join(separator);
  });

  Handlebars.registerHelper('ifLength', function (array, len, options) {
    var cond = _.isArray(array) && array.length === +len;
    return cond ? options.fn(this) : options.inverse(this);
  });

  Handlebars.registerHelper('unlessLength', function (array, len, options) {
    var cond = _.isArray(array) && array.length === +len;
    return cond ? options.inverse(this) : options.fn(this);
  });

  Handlebars.registerHelper('eachReverse', function (array, options) {
    var ret = '';

    if (array && array.length > 0) {
      for (var i = array.length - 1; i >= 0; i--) {
        ret += options.fn(array[i]);
      }
    } else {
      ret = options.inverse(this);
    }

    return ret;
  });

  Handlebars.registerHelper('joinEach', function (array, separator, options) {
    var ret = '';

    if (array && array.length > 0) {
      for (var i = 0, n = array.length; i < n; i++) {
        ret += options.fn(array[i]);
        if (i < n - 1) {
          ret += separator;
        }
      }
    } else {
      ret = options.inverse(this);
    }

    return ret;
  });

  Handlebars.registerHelper('sum', function () {
    var args = Array.prototype.slice.call(arguments, 0, -1);
    return args.reduce(function (p, c) {
      return p + +c;
    }, 0);
  });

  Handlebars.registerHelper('dashboardUrl', function (componentKey, componentQualifier) {
    var url = baseUrl + '/dashboard/index?id=' + encodeURIComponent(componentKey);
    if (componentQualifier === 'FIL' || componentQualifier === 'CLA') {
      url += '&metric=sqale_index';
    }
    return url;
  });

  Handlebars.registerHelper('translate', function () {
    var args = Array.prototype.slice.call(arguments, 0, -1);
    return window.translate.apply(this, args);
  });

  Handlebars.registerHelper('t', function () {
    var args = Array.prototype.slice.call(arguments, 0, -1);
    return window.t.apply(this, args);
  });

  Handlebars.registerHelper('tp', function () {
    var args = Array.prototype.slice.call(arguments, 0, -1);
    return window.tp.apply(this, args);
  });

  Handlebars.registerHelper('d', function (date) {
    return moment(date).format('LL');
  });

  Handlebars.registerHelper('dt', function (date) {
    return moment(date).format('LLL');
  });

  Handlebars.registerHelper('ds', function (date) {
    return moment(date).format('YYYY-MM-DD');
  });

  Handlebars.registerHelper('fromNow', function (date) {
    return moment(date).fromNow();
  });

  Handlebars.registerHelper('durationFromNow', function (date, units) {
    return moment(new Date()).diff(date, units);
  });

  Handlebars.registerHelper('numberShort', function (number) {
    var format = '0,0';
    if (number >= 10000) {
      format = '0.[0]a';
    }
    if (number >= 100000) {
      format = '0a';
    }
    return numeral(number).format(format);
  });

  Handlebars.registerHelper('withFirst', function (list, options) {
    if (list && list.length > 0) {
      return options.fn(list[0]);
    } else {
      return '';
    }
  });

  Handlebars.registerHelper('withLast', function (list, options) {
    if (list && list.length > 0) {
      return options.fn(list[list.length - 1]);
    } else {
      return '';
    }
  });

  Handlebars.registerHelper('withoutFirst', function (list, options) {
    if (list && list.length > 1) {
      return list.slice(1).reduce(function (prev, current) {
        return prev + options.fn(current);
      }, '');
    } else {
      return '';
    }
  });

  var audaciousFn;
  Handlebars.registerHelper('recursive', function (children, options) {
    var out = '';

    if (options.fn !== undefined) {
      audaciousFn = options.fn;
    }

    children.forEach(function (child) {
      out = out + audaciousFn(child);
    });

    return out;
  });

  Handlebars.registerHelper('sources', function (source, scm, options) {
    if (options == null) {
      options = scm;
      scm = null;
    }

    var sources = _.map(source, function (code, line) {
      return {
        lineNumber: line,
        code: code,
        scm: (scm && scm[line]) ? { author: scm[line][0], date: scm[line][1] } : undefined
      };
    });

    return sources.reduce(function (prev, current, index) {
      return prev + options.fn(_.extend({ first: index === 0 }, current));
    }, '');
  });

  Handlebars.registerHelper('operators', function (options) {
    var ops = ['LT', 'GT', 'EQ', 'NE'];

    return ops.reduce(function (prev, current) {
      return prev + options.fn(current);
    }, '');
  });

  Handlebars.registerHelper('changelog', function (diff) {
    var message = '';
    if (diff.newValue != null) {
      message = tp('issue.changelog.changed_to', t('issue.changelog.field', diff.key), diff.newValue);
    } else {
      message = tp('issue.changelog.removed', t('issue.changelog.field', diff.key));
    }
    if (diff.oldValue != null) {
      message += ' (';
      message += tp('issue.changelog.was', diff.oldValue);
      message += ')';
    }
    return message;
  });

  Handlebars.registerHelper('ifMeasureShouldBeShown', function (measure, period, options) {
    if (measure != null || period != null) {
      return options.fn(this);
    } else {
      return options.inverse(this);
    }
  });

  Handlebars.registerHelper('ifSCMChanged', function (source, line, options) {
    var currentLine = _.findWhere(source, { lineNumber: line }),
        prevLine = _.findWhere(source, { lineNumber: line - 1 }),
        changed = true;
    if (currentLine && prevLine && currentLine.scm && prevLine.scm) {
      changed = (currentLine.scm.author !== prevLine.scm.author) ||
          (currentLine.scm.date !== prevLine.scm.date) || (!prevLine.show);
    }
    return changed ? options.fn(this) : options.inverse(this);
  });

  Handlebars.registerHelper('ifSCMChanged2', function (source, line, options) {
    var currentLine = _.findWhere(source, { line: line }),
        prevLine = _.findWhere(source, { line: line - 1 }),
        changed = true;
    if (currentLine && prevLine && currentLine.scmAuthor && prevLine.scmAuthor) {
      changed = (currentLine.scmAuthor !== prevLine.scmAuthor) || (currentLine.scmDate !== prevLine.scmDate);
    }
    return changed ? options.fn(this) : options.inverse(this);
  });

  Handlebars.registerHelper('ifTestData', function (test, options) {
    if ((test.status !== 'OK') || ((test.status === 'OK') && test.coveredLines)) {
      return options.fn(this);
    } else {
      return options.inverse(this);
    }
  });

  Handlebars.registerHelper('eqComponents', function (a, b, options) {
    var notEq = a && b && ((a.project !== b.project) || (a.subProject !== b.subProject));
    return notEq ? options.inverse(this) : options.fn(this);
  });

  Handlebars.registerHelper('notEqComponents', function (a, b, options) {
    var notEq = a && b && ((a.project !== b.project) || (a.subProject !== b.subProject));
    return notEq ? options.fn(this) : options.inverse(this);
  });

  Handlebars.registerHelper('projectFullName', function (component) {
    return component.projectName + (component.subProjectName ? (' / ' + component.subProjectName) : '');
  });

  Handlebars.registerHelper('dirFromPath', function (path) {
    if (typeof path === 'string') {
      var tokens = path.split('/');
      return tokens.length > 1 ? _.initial(tokens).join('/') + '/' : '';
    } else {
      return null;
    }
  });

  Handlebars.registerHelper('collapsedDirFromPath', function (path) {
    return window.collapsedDirFromPath(path);
  });

  Handlebars.registerHelper('fileFromPath', function (path) {
    return window.fileFromPath(path);
  });

  Handlebars.registerHelper('repeat', function (number, options) {
    var ret = '';
    for (var i = 0; i < number; i++) {
      ret += options.fn(this);
    }
    return ret;
  });

  Handlebars.registerHelper('limitString', function (str) {
    if (typeof str === 'string') {
      var LIMIT = 30;
      return str.length > LIMIT ? str.substr(0, LIMIT) + '...' : str;
    }
  });

  Handlebars.registerHelper('withSign', function (number) {
    return number >= 0 ? '+' + number : '' + number;
  });

  Handlebars.registerHelper('formatMeasure', function (measure, type) {
    return window.formatMeasure(measure, type);
  });

  Handlebars.registerHelper('formatMeasureVariation', function (measure, type) {
    return window.formatMeasureVariation(measure, type);
  });

  Handlebars.registerHelper('dashboardL10n', function (dashboardName) {
    var l10nKey = 'dashboard.' + dashboardName + '.name';
    var l10nLabel = window.t(l10nKey);
    if (l10nLabel !== l10nKey) {
      return l10nLabel;
    } else {
      return dashboardName;
    }
  });

  Handlebars.registerHelper('ifShowAvatars', function (options) {
    var cond = window.SS && window.SS.lf && window.SS.lf.enableGravatar;
    return cond ? options.fn(this) : options.inverse(this);
  });

  Handlebars.registerHelper('avatarHelper', function (email, size) {
    // double the size for high pixel density screens
    var emailHash = window.md5((email || '').trim()),
        url = ('' + window.SS.lf.gravatarServerUrl)
            .replace('{EMAIL_MD5}', emailHash)
            .replace('{SIZE}', size * 2);
    return new Handlebars.SafeString(
        '<img class="rounded" src="' + url + '" width="' + size + '" height="' + size + '" alt="' + email + '">'
    );
  });

  Handlebars.registerHelper('ifCanUseFilter', function (query, options) {
    var cond = window.SS.user || query.indexOf('__me__') === -1;
    return cond ? options.fn(this) : options.inverse(this);
  });

  Handlebars.registerHelper('length', function (array) {
    return _.size(array);
  });

  Handlebars.registerHelper('ifLengthGT', function (array, len, options) {
    return _.size(array) > len ? options.fn(this) : options.inverse(this);
  });

})();
