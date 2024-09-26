/* eslint-disable */
module.exports = {
name: "@yarnpkg/plugin-echo-execute",
factory: function (require) {
var plugin;(()=>{"use strict";var o={d:(t,e)=>{for(var r in e)o.o(e,r)&&!o.o(t,r)&&Object.defineProperty(t,r,{enumerable:!0,get:e[r]})},o:(o,t)=>Object.prototype.hasOwnProperty.call(o,t),r:o=>{"undefined"!=typeof Symbol&&Symbol.toStringTag&&Object.defineProperty(o,Symbol.toStringTag,{value:"Module"}),Object.defineProperty(o,"__esModule",{value:!0})}},t={};o.r(t),o.d(t,{default:()=>r});const e=require("@yarnpkg/core"),r={hooks:{wrapScriptExecution:async(o,t,r,a,n)=>async()=>(await e.StreamReport.start({configuration:t.configuration,json:!1,includeFooter:!1,stdout:n.stdout},async o=>{const r=e.formatUtils.applyColor(t.configuration,a,e.formatUtils.Type.NAME),i=e.formatUtils.applyColor(t.configuration,n.script,e.formatUtils.Type.CODE);o.reportInfo(e.MessageName.UNNAMED,`executing [${r}]: ${i}`)}),o())}};plugin=t})();
return plugin;
}
};