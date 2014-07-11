define ['third-party/jquery.mockjax'], ->

  jQuery.mockjaxSettings.contentType = 'text/json';
  jQuery.mockjaxSettings.responseTime = 250;

  jQuery.mockjax
    url: "#{baseUrl}/api/sources/app"
    responseText: JSON.stringify
      key: 'org.codehaus.sonar:sonar-dev-maven-plugin:src/main/java/org/sonar/dev/UploadMojo.java'
      path: 'src/main/java/org/sonar/dev/UploadMojo.java'
      name: 'UploadMojo.java'
      q: 'FIL'
      fav: false
      project: 'org.codehaus.sonar:sonar-dev-maven-plugin'
      projectName: 'SonarQube Development Maven Plugin'
      periods: []
      measures:
        'ncloc': 69
        'coverage': '30%'
        'duplication_density': '7.4%'
        'debt': '3d 2h'
        'issues': 4
        'blocker_issues': 1
        'critical_issues': 2
        'major_issues': 0
        'minor_issues': 1
        'info_issues': 0