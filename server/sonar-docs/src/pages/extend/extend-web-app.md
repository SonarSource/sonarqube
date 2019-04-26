---
title: Adding pages to the webapp
url: /extend/extend-web-app/
---
SonarQube provides the ability to add a new JavaScript page. A page (or page extension) is a JavaScript application that runs in the SonarQube environment. You can find the example of page extensions in the SonarQube or [sonar-custom-plugin-example](https://github.com/SonarSource/sonar-custom-plugin-example/tree/6.x/) repositories on GitHub. 

## Getting Started
### Step 1. Create a Java class implementing PageDefinition
For each page, you'll need to set a key and a name. The page key should have the format `plugin_key/page_id`. Example: `governance/project_dump`. The `plugin_key` is computed from the `<artifactId>` in your `pom.xml`, or can be set explicitly in the pom using the `<pluginKey>` parameter in `sonar-packaging-maven-plugin` configuration.

All the pages should be declared in this class. 
```
import org.sonar.api.web.page.PageDefinition;
 
public class MyPluginPageDefinition implements PageDefinition {
  @Override
  public void define(Context context) {
    context
      .addPage(Page.builder("my_plugin/my_page").setName("My Page").build())
      .addPage(Page.builder("my_plugin/another_page").setName("Another Page").build());
  }
}
```

### Step 2. Create a JavaScript file
This file should have the same name as the page key (`my_page.js` in this case) and should be located in `src/main/resources/static`.
```
// my_page.js
window.registerExtension('my_plugin/my_page', function (options) {
  options.el.textContent = 'This is my page!';
  return function () {
    options.el.textContent = '';
  };
});
```
Where `my_plugin/my_page` is the same page key specified in step 1.

### Configuring the page 
There are 3 settings available when you define the page extensions using the PageDefinition class:

* `isAdmin`: tells if the page should be restricted to users with the administer permission.
* `scope`: tells if the page should be displayed in the primary menu (`GLOBAL` scope) or inside a component page (`COMPONENT` scope). By default, a page is global. 
* `component qualifiers`: allows you to specify if the page should be displayed for `PROJECT`, `MODULE`, `VIEW` or `SUB_VIEW` (the last two come with the Enterprise Edition). If set, the scope of the page must be `COMPONENT`.

### Runtime environment
SonarQube provides a global function `registerExtension` which should be called from the main javascript file. The function accepts two parameters:

* page extension key, which has a form of `<plugin key>/<page key>` (Ex: `governance/project_dump`)
* callback function, which is executed when the page extension is loaded. This callback should return another function, which will be called once the page extension will be closed. The callback accepts a single parameter options containing: 
   * `options.el` is a DOM element you must use to put the content inside
   * `options.currentUser` contains the response of api/users/current (see Web API docs for details)
   * (optional) `options.component` contains the information of the current project or view, if the page is project-level: key, name and qualifier

[[info]]
| SonarQube doesn't guarantee any JavaScript library availability at runtime. If you need a library, include it in the final file.

### Example
Displaying the number of project issues
```
window.registerExtension('my_plugin/my_page', function (options) {
 
  // let's create a flag telling if the page is still displayed
  var isDisplayed = true;
 
  // then do a Web API call to the /api/issues/search to get the number of issues
  // we pass `resolved: false` to request only unresolved issues
  // and `componentKeys: options.component.key` to request issues of the given project
  window.SonarRequest.getJSON('/api/issues/search', {
    resolved: false,
    componentKeys: options.component.key
  }).then(function (response) {
 
    // once the request is done, and the page is still displayed (not closed already)
    if (isDisplayed) {
 
      // let's create an `h2` tag and place the text inside
      var header = document.createElement('h2');
      header.textContent = 'The project has ' + response.total + ' issues';
 
      // append just created element to the container
      options.el.appendChild(header);
    }
  });
 
  // return a function, which is called when the page is being closed
  return function () {
 
    // we unset the `isDisplayed` flag to ignore to Web API calls finished after the page is closed
    isDisplayed = false;
  };
});
```

## Implement Pages with React
### Prerequisites 
* Be familiar with how to [build, deploy, and debug a plugin](/extend/developing-plugin/)
* Read the Get Started guide of ReactJS: https://reactjs.org/ to be familiar with React Components
* [NodeJS](https://nodejs.org/en/) has to be installed on your developer box

[[info]]
| SonarQube uses React 15.6.2 in the background. You should not try to use features of React 16+ in Custom Pages. This has not been tested.


### Custom Plugin
Everything has been prepared for you to be ready to start coding Custom Pages in this repo: https://github.com/SonarSource/sonar-custom-plugin-example. This way you don't have to spend time with the glue (maven, yarn, npm) and you can concentrate on implementing your pages.

Clone it and run `mvn clean package`. You will get a deployable JAR file. Once deployed, you will see some new pages at global, project and admin level. This has been done so you see Custom Pages with React in action.

This plugin contains 2 example pages built with React: 

* https://github.com/SonarSource/sonar-custom-plugin-example/blob/master/src/main/js/app-measures_history.js
* https://github.com/SonarSource/sonar-custom-plugin-example/blob/master/src/main/js/app-sanity_check.js

### Instance Statistics Page Example
The goal of this page is to show some statistics about your SonarQube instance and so to demonstrate how to call SQ Web API from your custom page inside a React component.

The page `app-sanity_check.js` is made of only one React Component named `InstanceStatisticsApp`. `InstanceStatisticsApp` is called in the `render()` method and will be in charge of:

* executing the queries to gather the data
* displaying them.


In the `componentDidMount()` method you will retrieve all the method calls to get the data from SonarQube. The various methods such as `findQualityProfilesStatistics` are defined in the `api.js` file. The complexity to gather the information is hidden in the `api.js`:
```
//InstanceStatisticsApp.js
componentDidMount() {
  findQualityProfilesStatistics().then(
    (valuesReturnedByAPI) => {
     this.setState({
          numberOfQualityProfiles: valuesReturnedByAPI
     });
    }
  );
  [...]
}
```
In the render() method we display the information gathered by `componentDidMount()` by mixing HTML and data, aka JSX code.
```
render() {
  return (
    <div className="page page-limited sanity-check">
      <table className="data zebra">
        <tbody>
          <tr>
            <td className="code-name-cell"># Quality Profiles</td>
            <td className="thin nowrap text-right">{this.state.numberOfQualityProfiles}</td>
          </tr>
        </tbody>
      </table>
    </div>
  )
}
```

### FAQ

Q: Can I reuse React Components created by SonarSource to build SonarQube?  
A: No, SonarQube is not exposing them, so you will have to build your own React Component

Q: How can I add my own styles?  
A: Feed the `style.css` and reference it in your custom page


## Making AJAX requests
All ajax requests must provide CSRF protection token. In order to help you to do so, we provide a set of useful helpers.

### Getting Started
Let's start with a simple `GET` request
```
window.SonarRequest
    .getJSON('/api/issues/search')
    .then(function (response) {
      // here 'response' contains the object representing the JSON output
    });
```    
* `window.SonarRequest` contains all the helper methods to do an API requests.
* `window.SonarRequest.getJSON` is a simplest helper to do an API call, receive some data and parse it as JSON.

### API Documentation
* `window.SonarRequest.request(url: string): Request`  
Start making an API call. Return a Request instance which has the following methods:
   * `setMethod(method: string)`: Request sets the http method, can be GET, POST, etc.  
   * `setData(data: object)`: Request sets the request parameters`
   * `submit()`: Promise sends the request
   
* `window.SonarRequest.getJSON(url: string[, data: object]): Promise`  
Send a GET request, get a response, parse it as JSON.

* `window.SonarRequest.postJSON(url: string[, data: object]): Promise`  
Send a POST request, get a response, parse it as JSON.

* `window.SonarRequest.post(url: string[, data: object]): Promise`  
Send a POST request, ignore the response content.

### Examples
Get the list of unresolved issues
```
window.SonarRequest.getJSON(
  '/api/issues/search',
  { resolved: false }
).then(function (response) {
  // response.issues contains the list of issues
});
```
Create new project
```
window.SonarRequest.post(
  '/api/projects/create',
  { key: 'sample', name: 'Sample' }
).then(function () {
  // the project has been created
});
```
Handle bad requests
```
window.SonarRequest.post(
  '/api/users/deactivate',
  { login: 'admin' }
).catch(function (error) {
  // error.response.status === 400
  // error.response.statusText === 'Bad Request'
  // To read the response:
  // error.response.json().then(function (jsonResponse) { ... });
});
```




## Debugging your page
When you are developing a custom page, if you want to see the impacts of your changes, you have to compile your plugin, deploy it in SonarQube and finally point your browser to that page to see the changes. This process is long, not efficient and doesn't allow you to quickly adjust your code. 

The easiest way to shorten the loop is to setup an HTTP proxy working like this:

* each time you will request a standard SonarQube URL, the proxy will redirect the call to SonarQube itself
* when you will request your page in the browser, the proxy will server your JS file from your local box from the path you are currently developing it.

In this example, we are going to use a JS implementation for the HTTP proxy based on Node.js: https://github.com/nodejitsu/node-http-proxy

**Requirements**
* Node.js LTS 6.11+
* HTTP Proxy 1.16.2 for Node.js: https://github.com/nodejitsu/node-http-proxy (to be installed in the next step)
* Your custom plugin containing the custom page you want to debug has to be deployed at least once in SonarQube

### Running the Proxy
Once Node is installed, check you can run it using : `node --version`

Install HTTP Proxy using this command: `npm install http-proxy --save`

Then put the following code in a file named `sq-proxy.js`:
```
var http = require('http'),
    httpProxy = require('http-proxy'),
    fs = require('fs');
var proxy = httpProxy.createProxyServer({});
var server = http.createServer(function(req, res) {
  //console.log(req.url);
  if (req.url === '/static/example/custom_page_global.js') {
    res.writeHead(200, { 'Content-Type': 'application/javascript' });
    fs.readFile('/Users/.../sonar-custom-plugin-example/src/main/resources/static/custom_page_global.js', 'utf8', function (err,data) {
        if (err) {
          return console.log(err);
        }
        res.write(data);
        res.end();
    });
  } else {
    proxy.web(req, res, { target: 'http://127.0.0.1:9000' });
  }
});
console.log("listening on port 5050")
server.listen(5050);
```
... and finally run your proxy like this: `node sq-proxy.js`

This will:

* run an HTTP proxy on the port 5050 
* catch all calls to the URL /static/example/custom_page_global.js
* serve the file located in the path `/Users/.../sonar-custom-plugin-example/src/main/resources/static/custom_page_global.js` instead of the one available in the Custom Plugin.
