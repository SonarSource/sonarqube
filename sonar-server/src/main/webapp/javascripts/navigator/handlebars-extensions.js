(function() {

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

  Handlebars.registerHelper('fromNow', function(time) {
    return moment(time).fromNow(true);
  });

  Handlebars.registerHelper('dashboardUrl', function(component) {
    var url = '/dashboard/index/' + decodeURIComponent(component.key);
    if (component.qualifier === 'FIL' || component.qualifier === 'CLA') {
      url += '?metric=sqale_index';
    }
    return url;
  });

})();
