(function(){function B(){}function C(W,U){for(var V in (U||{})){W[V]=U[V]
}return W
}function M(U){return(typeof U=="function")?U:function(){return U
}
}var K=Date.now||function(){return +new Date
};
function J(V){var U=H(V);
return(U)?((U!="array")?[V]:V):[]
}var H=function(U){return H.s.call(U).match(/^\[object\s(.*)\]$/)[1].toLowerCase()
};
H.s=Object.prototype.toString;
function G(Y,X){var W=H(Y);
if(W=="object"){for(var V in Y){X(Y[V],V)
}}else{for(var U=0;
U<Y.length;
U++){X(Y[U],U)
}}}function R(){var Y={};
for(var X=0,U=arguments.length;
X<U;
X++){var V=arguments[X];
if(H(V)!="object"){continue
}for(var W in V){var a=V[W],Z=Y[W];
Y[W]=(Z&&H(a)=="object"&&H(Z)=="object")?R(Z,a):N(a)
}}return Y
}function N(W){var V;
switch(H(W)){case"object":V={};
for(var Y in W){V[Y]=N(W[Y])
}break;
case"array":V=[];
for(var X=0,U=W.length;
X<U;
X++){V[X]=N(W[X])
}break;
default:return W
}return V
}function D(Y,X){if(Y.length<3){return null
}if(Y.length==4&&Y[3]==0&&!X){return"transparent"
}var V=[];
for(var U=0;
U<3;
U++){var W=(Y[U]-0).toString(16);
V.push((W.length==1)?"0"+W:W)
}return(X)?V:"#"+V.join("")
}function I(U){F(U);
if(U.parentNode){U.parentNode.removeChild(U)
}if(U.clearAttributes){U.clearAttributes()
}}function F(W){for(var V=W.childNodes,U=0;
U<V.length;
U++){I(V[U])
}}function T(W,V,U){if(W.addEventListener){W.addEventListener(V,U,false)
}else{W.attachEvent("on"+V,U)
}}function S(V,U){return(" "+V.className+" ").indexOf(" "+U+" ")>-1
}function P(V,U){if(!S(V,U)){V.className=(V.className+" "+U)
}}function A(V,U){V.className=V.className.replace(new RegExp("(^|\\s)"+U+"(?:\\s|$)"),"$1")
}function E(U){return document.getElementById(U)
}var O=function(V){V=V||{};
var U=function(){this.constructor=U;
if(O.prototyping){return this
}var X=(this.initialize)?this.initialize.apply(this,arguments):this;
return X
};
for(var W in O.Mutators){if(!V[W]){continue
}V=O.Mutators[W](V,V[W]);
delete V[W]
}C(U,this);
U.constructor=O;
U.prototype=V;
return U
};
O.Mutators={Extends:function(W,U){O.prototyping=U.prototype;
var V=new U;
delete V.parent;
V=O.inherit(V,W);
delete O.prototyping;
return V
},Implements:function(U,V){G(J(V),function(W){O.prototying=W;
C(U,(H(W)=="function")?new W:W);
delete O.prototyping
});
return U
}};
C(O,{inherit:function(V,Y){var U=arguments.callee.caller;
for(var X in Y){var W=Y[X];
var a=V[X];
var Z=H(W);
if(a&&Z=="function"){if(W!=a){if(U){W.__parent=a;
V[X]=W
}else{O.override(V,X,W)
}}}else{if(Z=="object"){V[X]=R(a,W)
}else{V[X]=W
}}}if(U){V.parent=function(){return arguments.callee.caller.__parent.apply(this,arguments)
}
}return V
},override:function(V,U,Y){var X=O.prototyping;
if(X&&V[U]!=X[U]){X=null
}var W=function(){var Z=this.parent;
this.parent=X?X[U]:V[U];
var a=Y.apply(this,arguments);
this.parent=Z;
return a
};
V[U]=W
}});
O.prototype.implement=function(){var U=this.prototype;
G(Array.prototype.slice.call(arguments||[]),function(V){O.inherit(U,V)
});
return this
};
this.TreeUtil={prune:function(V,U){this.each(V,function(X,W){if(W==U&&X.children){delete X.children;
X.children=[]
}})
},getParent:function(U,Y){if(U.id==Y){return false
}var X=U.children;
if(X&&X.length>0){for(var W=0;
W<X.length;
W++){if(X[W].id==Y){return U
}else{var V=this.getParent(X[W],Y);
if(V){return V
}}}}return false
},getSubtree:function(U,Y){if(U.id==Y){return U
}for(var W=0,X=U.children;
W<X.length;
W++){var V=this.getSubtree(X[W],Y);
if(V!=null){return V
}}return null
},getLeaves:function(W,U){var X=[],V=U||Number.MAX_VALUE;
this.each(W,function(Z,Y){if(Y<V&&(!Z.children||Z.children.length==0)){X.push({node:Z,level:V-Y})
}});
return X
},eachLevel:function(U,Z,W,Y){if(Z<=W){Y(U,Z);
for(var V=0,X=U.children;
V<X.length;
V++){this.eachLevel(X[V],Z+1,W,Y)
}}},each:function(U,V){this.eachLevel(U,0,Number.MAX_VALUE,V)
},loadSubtrees:function(d,X){var c=X.request&&X.levelsToShow;
var Y=this.getLeaves(d,c),a=Y.length,Z={};
if(a==0){X.onComplete()
}for(var W=0,U=0;
W<a;
W++){var b=Y[W],V=b.node.id;
Z[V]=b.node;
X.request(V,b.level,{onComplete:function(g,e){var f=e.children;
Z[g].children=f;
if(++U==a){X.onComplete()
}}})
}}};
this.Canvas=(function(){var V={injectInto:"id",width:200,height:200,backgroundColor:"#333333",styles:{fillStyle:"#000000",strokeStyle:"#000000"},backgroundCanvas:false};
function X(){X.t=X.t||typeof (HTMLCanvasElement);
return"function"==X.t||"object"==X.t
}function W(Z,c,b){var a=document.createElement(Z);
(function(e,f){if(f){for(var d in f){e[d]=f[d]
}}return arguments.callee
})(a,c)(a.style,b);
if(Z=="canvas"&&!X()&&G_vmlCanvasManager){a=G_vmlCanvasManager.initElement(document.body.appendChild(a))
}return a
}function U(Z){return document.getElementById(Z)
}function Y(c,b,a,e){var d=a?(c.width-a):c.width;
var Z=e?(c.height-e):c.height;
b.translate(d/2,Z/2)
}return function(b,c){var n,g,Z,k,d,j;
if(arguments.length<1){throw"Arguments missing"
}var a=b+"-label",i=b+"-canvas",e=b+"-bkcanvas";
c=R(V,c||{});
var f={width:c.width,height:c.height};
Z=W("div",{id:b},R(f,{position:"relative"}));
k=W("div",{id:a},{overflow:"visible",position:"absolute",top:0,left:0,width:f.width+"px",height:0});
var l={position:"absolute",top:0,left:0,width:f.width+"px",height:f.height+"px"};
d=W("canvas",R({id:i},f),l);
var h=c.backgroundCanvas;
if(h){j=W("canvas",R({id:e},f),l);
Z.appendChild(j)
}Z.appendChild(d);
Z.appendChild(k);
U(c.injectInto).appendChild(Z);
n=d.getContext("2d");
Y(d,n);
var m=c.styles;
var o;
for(o in m){n[o]=m[o]
}if(h){g=j.getContext("2d");
m=h.styles;
for(o in m){g[o]=m[o]
}Y(j,g);
h.impl.init(j,g);
h.impl.plot(j,g)
}return{id:b,getCtx:function(){return n
},getElement:function(){return Z
},resize:function(u,p){var t=d.width,v=d.height;
d.width=u;
d.height=p;
d.style.width=u+"px";
d.style.height=p+"px";
if(h){j.width=u;
j.height=p;
j.style.width=u+"px";
j.style.height=p+"px"
}if(!X()){Y(d,n,t,v)
}else{Y(d,n)
}var q=c.styles;
var r;
for(r in q){n[r]=q[r]
}if(h){q=h.styles;
for(r in q){g[r]=q[r]
}if(!X()){Y(j,g,t,v)
}else{Y(j,g)
}h.impl.init(j,g);
h.impl.plot(j,g)
}},getSize:function(){return{width:d.width,height:d.height}
},path:function(p,q){n.beginPath();
q(n);
n[p]();
n.closePath()
},clear:function(){var p=this.getSize();
n.clearRect(-p.width/2,-p.height/2,p.width,p.height)
},clearRectangle:function(t,r,q,s){if(!X()){var p=n.fillStyle;
n.fillStyle=c.backgroundColor;
n.fillRect(s,t,Math.abs(r-s),Math.abs(q-t));
n.fillStyle=p
}else{n.clearRect(s,t,Math.abs(r-s),Math.abs(q-t))
}}}
}
})();
this.Polar=function(V,U){this.theta=V;
this.rho=U
};
Polar.prototype={getc:function(U){return this.toComplex(U)
},getp:function(){return this
},set:function(U){U=U.getp();
this.theta=U.theta;
this.rho=U.rho
},setc:function(U,V){this.rho=Math.sqrt(U*U+V*V);
this.theta=Math.atan2(V,U);
if(this.theta<0){this.theta+=Math.PI*2
}},setp:function(V,U){this.theta=V;
this.rho=U
},clone:function(){return new Polar(this.theta,this.rho)
},toComplex:function(W){var U=Math.cos(this.theta)*this.rho;
var V=Math.sin(this.theta)*this.rho;
if(W){return{x:U,y:V}
}return new Complex(U,V)
},add:function(U){return new Polar(this.theta+U.theta,this.rho+U.rho)
},scale:function(U){return new Polar(this.theta,this.rho*U)
},equals:function(U){return this.theta==U.theta&&this.rho==U.rho
},$add:function(U){this.theta=this.theta+U.theta;
this.rho+=U.rho;
return this
},$madd:function(U){this.theta=(this.theta+U.theta)%(Math.PI*2);
this.rho+=U.rho;
return this
},$scale:function(U){this.rho*=U;
return this
},interpolate:function(W,c){var X=Math.PI,a=X*2;
var V=function(d){return(d<0)?(d%a)+a:d%a
};
var Z=this.theta,b=W.theta;
var Y;
if(Math.abs(Z-b)>X){if(Z>b){Y=V((b+((Z-a)-b)*c))
}else{Y=V((b-a+(Z-(b-a))*c))
}}else{Y=V((b+(Z-b)*c))
}var U=(this.rho-W.rho)*c+W.rho;
return{theta:Y,rho:U}
}};
var L=function(V,U){return new Polar(V,U)
};
Polar.KER=L(0,0);
this.Complex=function(U,V){this.x=U;
this.y=V
};
Complex.prototype={getc:function(){return this
},getp:function(U){return this.toPolar(U)
},set:function(U){U=U.getc(true);
this.x=U.x;
this.y=U.y
},setc:function(U,V){this.x=U;
this.y=V
},setp:function(V,U){this.x=Math.cos(V)*U;
this.y=Math.sin(V)*U
},clone:function(){return new Complex(this.x,this.y)
},toPolar:function(W){var U=this.norm();
var V=Math.atan2(this.y,this.x);
if(V<0){V+=Math.PI*2
}if(W){return{theta:V,rho:U}
}return new Polar(V,U)
},norm:function(){return Math.sqrt(this.squaredNorm())
},squaredNorm:function(){return this.x*this.x+this.y*this.y
},add:function(U){return new Complex(this.x+U.x,this.y+U.y)
},prod:function(U){return new Complex(this.x*U.x-this.y*U.y,this.y*U.x+this.x*U.y)
},conjugate:function(){return new Complex(this.x,-this.y)
},scale:function(U){return new Complex(this.x*U,this.y*U)
},equals:function(U){return this.x==U.x&&this.y==U.y
},$add:function(U){this.x+=U.x;
this.y+=U.y;
return this
},$prod:function(W){var U=this.x,V=this.y;
this.x=U*W.x-V*W.y;
this.y=V*W.x+U*W.y;
return this
},$conjugate:function(){this.y=-this.y;
return this
},$scale:function(U){this.x*=U;
this.y*=U;
return this
},$div:function(X){var U=this.x,W=this.y;
var V=X.squaredNorm();
this.x=U*X.x+W*X.y;
this.y=W*X.x-U*X.y;
return this.$scale(1/V)
}};
var Q=function(V,U){return new Complex(V,U)
};
Complex.KER=Q(0,0);
this.Graph=new O({initialize:function(U){var V={complex:false,Node:{}};
this.opt=R(V,U||{});
this.nodes={}
},getNode:function(U){if(this.hasNode(U)){return this.nodes[U]
}return false
},getAdjacence:function(W,U){var V=[];
if(this.hasNode(W)&&this.hasNode(U)&&this.nodes[W].adjacentTo({id:U})&&this.nodes[U].adjacentTo({id:W})){V.push(this.nodes[W].getAdjacency(U));
V.push(this.nodes[U].getAdjacency(W));
return V
}return false
},addNode:function(U){if(!this.nodes[U.id]){this.nodes[U.id]=new Graph.Node(C({id:U.id,name:U.name,data:U.data},this.opt.Node),this.opt.complex)
}return this.nodes[U.id]
},addAdjacence:function(X,W,V){var Y=[];
if(!this.hasNode(X.id)){this.addNode(X)
}if(!this.hasNode(W.id)){this.addNode(W)
}X=this.nodes[X.id];
W=this.nodes[W.id];
for(var U in this.nodes){if(this.nodes[U].id==X.id){if(!this.nodes[U].adjacentTo(W)){Y.push(this.nodes[U].addAdjacency(W,V))
}}if(this.nodes[U].id==W.id){if(!this.nodes[U].adjacentTo(X)){Y.push(this.nodes[U].addAdjacency(X,V))
}}}return Y
},removeNode:function(W){if(this.hasNode(W)){var V=this.nodes[W];
for(var U=0 in V.adjacencies){var adj=V.adjacencies[U];
this.removeAdjacence(W,adj.nodeTo.id)
}delete this.nodes[W]
}},removeAdjacence:function(Y,X){if(this.hasNode(Y)){this.nodes[Y].removeAdjacency(X)
}if(this.hasNode(X)){this.nodes[X].removeAdjacency(Y)
}},hasNode:function(X){return X in this.nodes
}});
Graph.Node=new O({initialize:function(X,Z){var Y={id:"",name:"",data:{},adjacencies:{},selected:false,drawn:false,exist:false,angleSpan:{begin:0,end:0},alpha:1,startAlpha:1,endAlpha:1,pos:(Z&&Q(0,0))||L(0,0),startPos:(Z&&Q(0,0))||L(0,0),endPos:(Z&&Q(0,0))||L(0,0)};
C(this,C(Y,X))
},adjacentTo:function(X){return X.id in this.adjacencies
},getAdjacency:function(X){return this.adjacencies[X]
},addAdjacency:function(Y,Z){var X=new Graph.Adjacence(this,Y,Z);
return this.adjacencies[Y.id]=X
},removeAdjacency:function(X){delete this.adjacencies[X]
}});
Graph.Adjacence=function(X,Z,Y){this.nodeFrom=X;
this.nodeTo=Z;
this.data=Y||{};
this.alpha=1;
this.startAlpha=1;
this.endAlpha=1
};
Graph.Util={filter:function(Y){if(!Y||!(H(Y)=="string")){return function(){return true
}
}var X=Y.split(" ");
return function(a){for(var Z=0;
Z<X.length;
Z++){if(a[X[Z]]){return false
}}return true
}
},getNode:function(X,Y){return X.getNode(Y)
},eachNode:function(b,a,X){var Z=this.filter(X);
for(var Y in b.nodes){if(Z(b.nodes[Y])){a(b.nodes[Y])
}}},eachAdjacency:function(a,b,X){var Y=a.adjacencies,Z=this.filter(X);
for(var c in Y){if(Z(Y[c])){b(Y[c],c)
}}},computeLevels:function(d,e,a,Z){a=a||0;
var b=this.filter(Z);
this.eachNode(d,function(f){f._flag=false;
f._depth=-1
},Z);
var Y=d.getNode(e);
Y._depth=a;
var X=[Y];
while(X.length!=0){var c=X.pop();
c._flag=true;
this.eachAdjacency(c,function(f){var g=f.nodeTo;
if(g._flag==false&&b(g)){if(g._depth<0){g._depth=c._depth+1+a
}X.unshift(g)
}},Z)
}},eachBFS:function(c,d,b,Y){var Z=this.filter(Y);
this.clean(c);
var X=[c.getNode(d)];
while(X.length!=0){var a=X.pop();
a._flag=true;
b(a,a._depth);
this.eachAdjacency(a,function(e){var f=e.nodeTo;
if(f._flag==false&&Z(f)){f._flag=true;
X.unshift(f)
}},Y)
}},eachLevel:function(b,g,Y,c,a){var f=b._depth,X=this.filter(a),e=this;
Y=Y===false?Number.MAX_VALUE-f:Y;
(function Z(j,h,i){var k=j._depth;
if(k>=h&&k<=i&&X(j)){c(j,k)
}if(k<i){e.eachAdjacency(j,function(d){var l=d.nodeTo;
if(l._depth>k){Z(l,h,i)
}})
}})(b,g+f,Y+f)
},eachSubgraph:function(Y,Z,X){this.eachLevel(Y,0,false,Z,X)
},eachSubnode:function(Y,Z,X){this.eachLevel(Y,1,1,Z,X)
},anySubnode:function(a,Z,Y){var X=false;
Z=Z||M(true);
var b=H(Z)=="string"?function(c){return c[Z]
}:Z;
this.eachSubnode(a,function(c){if(b(c)){X=true
}},Y);
return X
},getSubnodes:function(c,d,X){var Z=[],b=this;
d=d||0;
var a,Y;
if(H(d)=="array"){a=d[0];
Y=d[1]
}else{a=d;
Y=Number.MAX_VALUE-c._depth
}this.eachLevel(c,a,Y,function(e){Z.push(e)
},X);
return Z
},getParents:function(Y){var X=[];
this.eachAdjacency(Y,function(Z){var a=Z.nodeTo;
if(a._depth<Y._depth){X.push(a)
}});
return X
},isDescendantOf:function(a,b){if(a.id==b){return true
}var Z=this.getParents(a),X=false;
for(var Y=0;
!X&&Y<Z.length;
Y++){X=X||this.isDescendantOf(Z[Y],b)
}return X
},clean:function(X){this.eachNode(X,function(Y){Y._flag=false
})
}};
Graph.Op={options:{type:"nothing",duration:2000,hideLabels:true,fps:30},removeNode:function(c,a){var X=this.viz;
var Y=R(this.options,X.controller,a);
var e=J(c);
var Z,b,d;
switch(Y.type){case"nothing":for(Z=0;
Z<e.length;
Z++){X.graph.removeNode(e[Z])
}break;
case"replot":this.removeNode(e,{type:"nothing"});
X.fx.clearLabels();
X.refresh(true);
break;
case"fade:seq":case"fade":b=this;
for(Z=0;
Z<e.length;
Z++){d=X.graph.getNode(e[Z]);
d.endAlpha=0
}X.fx.animate(R(Y,{modes:["fade:nodes"],onComplete:function(){b.removeNode(e,{type:"nothing"});
X.fx.clearLabels();
X.reposition();
X.fx.animate(R(Y,{modes:["linear"]}))
}}));
break;
case"fade:con":b=this;
for(Z=0;
Z<e.length;
Z++){d=X.graph.getNode(e[Z]);
d.endAlpha=0;
d.ignore=true
}X.reposition();
X.fx.animate(R(Y,{modes:["fade:nodes","linear"],onComplete:function(){b.removeNode(e,{type:"nothing"})
}}));
break;
case"iter":b=this;
X.fx.sequence({condition:function(){return e.length!=0
},step:function(){b.removeNode(e.shift(),{type:"nothing"});
X.fx.clearLabels()
},onComplete:function(){Y.onComplete()
},duration:Math.ceil(Y.duration/e.length)});
break;
default:this.doError()
}},removeEdge:function(d,b){var X=this.viz;
var Z=R(this.options,X.controller,b);
var Y=(H(d[0])=="string")?[d]:d;
var a,c,e;
switch(Z.type){case"nothing":for(a=0;
a<Y.length;
a++){X.graph.removeAdjacence(Y[a][0],Y[a][1])
}break;
case"replot":this.removeEdge(Y,{type:"nothing"});
X.refresh(true);
break;
case"fade:seq":case"fade":c=this;
for(a=0;
a<Y.length;
a++){e=X.graph.getAdjacence(Y[a][0],Y[a][1]);
if(e){e[0].endAlpha=0;
e[1].endAlpha=0
}}X.fx.animate(R(Z,{modes:["fade:vertex"],onComplete:function(){c.removeEdge(Y,{type:"nothing"});
X.reposition();
X.fx.animate(R(Z,{modes:["linear"]}))
}}));
break;
case"fade:con":c=this;
for(a=0;
a<Y.length;
a++){e=X.graph.getAdjacence(Y[a][0],Y[a][1]);
if(e){e[0].endAlpha=0;
e[0].ignore=true;
e[1].endAlpha=0;
e[1].ignore=true
}}X.reposition();
X.fx.animate(R(Z,{modes:["fade:vertex","linear"],onComplete:function(){c.removeEdge(Y,{type:"nothing"})
}}));
break;
case"iter":c=this;
X.fx.sequence({condition:function(){return Y.length!=0
},step:function(){c.removeEdge(Y.shift(),{type:"nothing"});
X.fx.clearLabels()
},onComplete:function(){Z.onComplete()
},duration:Math.ceil(Z.duration/Y.length)});
break;
default:this.doError()
}},sum:function(e,Y){var c=this.viz;
var f=R(this.options,c.controller,Y),b=c.root;
var a,d;
c.root=Y.id||c.root;
switch(f.type){case"nothing":d=c.construct(e);
a=Graph.Util;
a.eachNode(d,function(g){a.eachAdjacency(g,function(h){c.graph.addAdjacence(h.nodeFrom,h.nodeTo,h.data)
})
});
break;
case"replot":c.refresh(true);
this.sum(e,{type:"nothing"});
c.refresh(true);
break;
case"fade:seq":case"fade":case"fade:con":a=Graph.Util;
that=this;
d=c.construct(e);
var X=this.preprocessSum(d);
var Z=!X?["fade:nodes"]:["fade:nodes","fade:vertex"];
c.reposition();
if(f.type!="fade:con"){c.fx.animate(R(f,{modes:["linear"],onComplete:function(){c.fx.animate(R(f,{modes:Z,onComplete:function(){f.onComplete()
}}))
}}))
}else{a.eachNode(c.graph,function(g){if(g.id!=b&&g.pos.getp().equals(Polar.KER)){g.pos.set(g.endPos);
g.startPos.set(g.endPos)
}});
c.fx.animate(R(f,{modes:["linear"].concat(Z)}))
}break;
default:this.doError()
}},morph:function(e,Y){var c=this.viz;
var f=R(this.options,c.controller,Y),b=c.root;
var a,d;
c.root=Y.id||c.root;
switch(f.type){case"nothing":d=c.construct(e);
a=Graph.Util;
a.eachNode(d,function(g){a.eachAdjacency(g,function(h){c.graph.addAdjacence(h.nodeFrom,h.nodeTo,h.data)
})
});
a.eachNode(c.graph,function(g){a.eachAdjacency(g,function(h){if(!d.getAdjacence(h.nodeFrom.id,h.nodeTo.id)){c.graph.removeAdjacence(h.nodeFrom.id,h.nodeTo.id)
}});
if(!d.hasNode(g.id)){c.graph.removeNode(g.id)
}});
break;
case"replot":c.fx.clearLabels(true);
this.morph(e,{type:"nothing"});
c.refresh(true);
c.refresh(true);
break;
case"fade:seq":case"fade":case"fade:con":a=Graph.Util;
that=this;
d=c.construct(e);
var X=this.preprocessSum(d);
a.eachNode(c.graph,function(g){if(!d.hasNode(g.id)){g.alpha=1;
g.startAlpha=1;
g.endAlpha=0;
g.ignore=true
}});
a.eachNode(c.graph,function(g){if(g.ignore){return 
}a.eachAdjacency(g,function(h){if(h.nodeFrom.ignore||h.nodeTo.ignore){return 
}var i=d.getNode(h.nodeFrom.id);
var j=d.getNode(h.nodeTo.id);
if(!i.adjacentTo(j)){var k=c.graph.getAdjacence(i.id,j.id);
X=true;
k[0].alpha=1;
k[0].startAlpha=1;
k[0].endAlpha=0;
k[0].ignore=true;
k[1].alpha=1;
k[1].startAlpha=1;
k[1].endAlpha=0;
k[1].ignore=true
}})
});
var Z=!X?["fade:nodes"]:["fade:nodes","fade:vertex"];
c.reposition();
a.eachNode(c.graph,function(g){if(g.id!=b&&g.pos.getp().equals(Polar.KER)){g.pos.set(g.endPos);
g.startPos.set(g.endPos)
}});
c.fx.animate(R(f,{modes:["polar"].concat(Z),onComplete:function(){a.eachNode(c.graph,function(g){if(g.ignore){c.graph.removeNode(g.id)
}});
a.eachNode(c.graph,function(g){a.eachAdjacency(g,function(h){if(h.ignore){c.graph.removeAdjacence(h.nodeFrom.id,h.nodeTo.id)
}})
});
f.onComplete()
}}));
break;
default:this.doError()
}},preprocessSum:function(Z){var X=this.viz;
var Y=Graph.Util;
Y.eachNode(Z,function(b){if(!X.graph.hasNode(b.id)){X.graph.addNode(b);
var c=X.graph.getNode(b.id);
c.alpha=0;
c.startAlpha=0;
c.endAlpha=1
}});
var a=false;
Y.eachNode(Z,function(b){Y.eachAdjacency(b,function(c){var d=X.graph.getNode(c.nodeFrom.id);
var e=X.graph.getNode(c.nodeTo.id);
if(!d.adjacentTo(e)){var f=X.graph.addAdjacence(d,e,c.data);
if(d.startAlpha==d.endAlpha&&e.startAlpha==e.endAlpha){a=true;
f[0].alpha=0;
f[0].startAlpha=0;
f[0].endAlpha=1;
f[1].alpha=0;
f[1].startAlpha=0;
f[1].endAlpha=1
}}})
});
return a
}};
Graph.Plot={Interpolator:{moebius:function(a,c,Y){if(c<=1||Y.norm()<=1){var X=Y.x,b=Y.y;
var Z=a.startPos.getc().moebiusTransformation(Y);
a.pos.setc(Z.x,Z.y);
Y.x=X;
Y.y=b
}},linear:function(X,a){var Z=X.startPos.getc(true);
var Y=X.endPos.getc(true);
X.pos.setc((Y.x-Z.x)*a+Z.x,(Y.y-Z.y)*a+Z.y)
},"fade:nodes":function(X,a){if(a<=1&&(X.endAlpha!=X.alpha)){var Z=X.startAlpha;
var Y=X.endAlpha;
X.alpha=Z+(Y-Z)*a
}},"fade:vertex":function(X,a){var Z=X.adjacencies;
for(var Y in Z){this["fade:nodes"](Z[Y],a)
}},polar:function(Y,b){var a=Y.startPos.getp(true);
var Z=Y.endPos.getp();
var X=Z.interpolate(a,b);
Y.pos.setp(X.theta,X.rho)
}},labelsHidden:false,labelContainer:false,labels:{},getLabelContainer:function(){return this.labelContainer?this.labelContainer:this.labelContainer=document.getElementById(this.viz.config.labelContainer)
},getLabel:function(X){return(X in this.labels&&this.labels[X]!=null)?this.labels[X]:this.labels[X]=document.getElementById(X)
},hideLabels:function(Y){var X=this.getLabelContainer();
if(Y){X.style.display="none"
}else{X.style.display=""
}this.labelsHidden=Y
},clearLabels:function(X){for(var Y in this.labels){if(X||!this.viz.graph.hasNode(Y)){this.disposeLabel(Y);
delete this.labels[Y]
}}},disposeLabel:function(Y){var X=this.getLabel(Y);
if(X&&X.parentNode){X.parentNode.removeChild(X)
}},hideLabel:function(b,X){b=J(b);
var Y=X?"":"none",Z,a=this;
G(b,function(d){var c=a.getLabel(d.id);
if(c){c.style.display=Y
}})
},sequence:function(Y){var Z=this;
Y=R({condition:M(false),step:B,onComplete:B,duration:200},Y||{});
var X=setInterval(function(){if(Y.condition()){Y.step()
}else{clearInterval(X);
Y.onComplete()
}Z.viz.refresh(true)
},Y.duration)
},animate:function(Z,Y){var b=this,X=this.viz,c=X.graph,a=Graph.Util;
Z=R(X.controller,Z||{});
if(Z.hideLabels){this.hideLabels(true)
}this.animation.setOptions(R(Z,{$animating:false,compute:function(e){var d=Y?Y.scale(-e):null;
a.eachNode(c,function(g){for(var f=0;
f<Z.modes.length;
f++){b.Interpolator[Z.modes[f]](g,e,d)
}});
b.plot(Z,this.$animating);
this.$animating=true
},complete:function(){a.eachNode(c,function(d){d.startPos.set(d.pos);
d.startAlpha=d.alpha
});
if(Z.hideLabels){b.hideLabels(false)
}b.plot(Z);
Z.onComplete();
Z.onAfterCompute()
}})).start()
},plot:function(Y,g){var e=this.viz,b=e.graph,Z=e.canvas,X=e.root,c=this,f=Z.getCtx(),d=Graph.Util;
Y=Y||this.viz.controller;
Y.clearCanvas&&Z.clear();
var a=!!b.getNode(X).visited;
d.eachNode(b,function(h){d.eachAdjacency(h,function(i){var j=i.nodeTo;
if(!!j.visited===a&&h.drawn&&j.drawn){!g&&Y.onBeforePlotLine(i);
f.save();
f.globalAlpha=Math.min(Math.min(h.alpha,j.alpha),i.alpha);
c.plotLine(i,Z,g);
f.restore();
!g&&Y.onAfterPlotLine(i)
}});
f.save();
if(h.drawn){f.globalAlpha=h.alpha;
!g&&Y.onBeforePlotNode(h);
c.plotNode(h,Z,g);
!g&&Y.onAfterPlotNode(h)
}if(!c.labelsHidden&&Y.withLabels){if(h.drawn&&f.globalAlpha>=0.95){c.plotLabel(Z,h,Y)
}else{c.hideLabel(h,false)
}}f.restore();
h.visited=!a
})
},plotLabel:function(a,b,Z){var c=b.id,X=this.getLabel(c);
if(!X&&!(X=document.getElementById(c))){X=document.createElement("div");
var Y=this.getLabelContainer();
Y.appendChild(X);
X.id=c;
X.className="node";
X.style.position="absolute";
Z.onCreateLabel(X,b);
this.labels[b.id]=X
}this.placeLabel(X,b,Z)
},plotNode:function(Z,Y,h){var e=this.node,b=Z.data;
var d=e.overridable&&b;
var X=d&&b.$lineWidth||e.lineWidth;
var a=d&&b.$color||e.color;
var g=Y.getCtx();
g.lineWidth=X;
g.fillStyle=a;
g.strokeStyle=a;
var c=Z.data&&Z.data.$type||e.type;
this.nodeTypes[c].call(this,Z,Y,h)
},plotLine:function(e,Z,h){var X=this.edge,b=e.data;
var d=X.overridable&&b;
var Y=d&&b.$lineWidth||X.lineWidth;
var a=d&&b.$color||X.color;
var g=Z.getCtx();
g.lineWidth=Y;
g.fillStyle=a;
g.strokeStyle=a;
var c=e.data&&e.data.$type||X.type;
this.edgeTypes[c].call(this,e,Z,h)
},fitsInCanvas:function(Z,X){var Y=X.getSize();
if(Z.x>=Y.width||Z.x<0||Z.y>=Y.height||Z.y<0){return false
}return true
}};
var Loader={construct:function(Y){var Z=(H(Y)=="array");
var X=new Graph(this.graphOptions);
if(!Z){(function(a,c){a.addNode(c);
for(var b=0,d=c.children;
b<d.length;
b++){a.addAdjacence(c,d[b]);
arguments.callee(a,d[b])
}})(X,Y)
}else{(function(b,e){var h=function(j){for(var i=0;
i<e.length;
i++){if(e[i].id==j){return e[i]
}}return undefined
};
for(var d=0;
d<e.length;
d++){b.addNode(e[d]);
for(var c=0,a=e[d].adjacencies;
c<a.length;
c++){var f=a[c],g;
if(typeof a[c]!="string"){g=f.data;
f=f.nodeTo
}b.addAdjacence(e[d],h(f),g)
}}})(X,Y)
}return X
},loadJSON:function(Y,X){this.json=Y;
this.graph=this.construct(Y);
if(H(Y)!="array"){this.root=Y.id
}else{this.root=Y[X?X:0].id
}}};
this.Trans={linear:function(X){return X
}};
(function(){var X=function(a,Z){Z=J(Z);
return C(a,{easeIn:function(b){return a(b,Z)
},easeOut:function(b){return 1-a(1-b,Z)
},easeInOut:function(b){return(b<=0.5)?a(2*b,Z)/2:(2-a(2*(1-b),Z))/2
}})
};
var Y={Pow:function(a,Z){return Math.pow(a,Z[0]||6)
},Expo:function(Z){return Math.pow(2,8*(Z-1))
},Circ:function(Z){return 1-Math.sin(Math.acos(Z))
},Sine:function(Z){return 1-Math.sin((1-Z)*Math.PI/2)
},Back:function(a,Z){Z=Z[0]||1.618;
return Math.pow(a,2)*((Z+1)*a-Z)
},Bounce:function(e){var d;
for(var c=0,Z=1;
1;
c+=Z,Z/=2){if(e>=(7-4*c)/11){d=Z*Z-Math.pow((11-6*c-11*e)/4,2);
break
}}return d
},Elastic:function(a,Z){return Math.pow(2,10*--a)*Math.cos(20*a*Math.PI*(Z[0]||1)/3)
}};
G(Y,function(a,Z){Trans[Z]=X(a)
});
G(["Quad","Cubic","Quart","Quint"],function(a,Z){Trans[a]=X(function(b){return Math.pow(b,[Z+2])
})
})
})();
var Animation=new O({initalize:function(X){this.setOptions(X)
},setOptions:function(X){var Y={duration:2500,fps:40,transition:Trans.Quart.easeInOut,compute:B,complete:B};
this.opt=R(Y,X||{});
return this
},getTime:function(){return K()
},step:function(){var Y=this.getTime(),X=this.opt;
if(Y<this.time+X.duration){var Z=X.transition((Y-this.time)/X.duration);
X.compute(Z)
}else{this.timer=clearInterval(this.timer);
X.compute(1);
X.complete()
}},start:function(){this.time=0;
this.startTimer();
return this
},startTimer:function(){var Y=this,X=this.opt;
if(this.timer){return false
}this.time=this.getTime()-this.time;
this.timer=setInterval((function(){Y.step()
}),Math.round(1000/X.fps));
return true
}});
(function(){var g=Array.prototype.slice;
function e(q,k,i,o){var m=k.Node,n=Graph.Util;
var j=k.multitree;
if(m.overridable){var p=-1,l=-1;
n.eachNode(q,function(t){if(t._depth==i&&(!j||("$orn" in t.data)&&t.data.$orn==o)){var r=t.data.$width||m.width;
var s=t.data.$height||m.height;
p=(p<r)?r:p;
l=(l<s)?s:l
}});
return{width:p<0?m.width:p,height:l<0?m.height:l}
}else{return m
}}function h(j,m,l,i){var k=(i=="left"||i=="right")?"y":"x";
j[m][k]+=l
}function c(j,k){var i=[];
G(j,function(l){l=g.call(l);
l[0]+=k;
l[1]+=k;
i.push(l)
});
return i
}function f(l,i){if(l.length==0){return i
}if(i.length==0){return l
}var k=l.shift(),j=i.shift();
return[[k[0],j[1]]].concat(f(l,i))
}function a(i,j){j=j||[];
if(i.length==0){return j
}var k=i.pop();
return a(i,f(k,j))
}function d(m,k,n,j,l){if(m.length<=l||k.length<=l){return 0
}var r=m[l][1],o=k[l][0];
return Math.max(d(m,k,n,j,++l)+n,r-o+j)
}function b(l,j,i){function k(o,q,n){if(q.length<=n){return[]
}var p=q[n],m=d(o,p,j,i,0);
return[m].concat(k(f(o,c(p,m)),q,++n))
}return k([],l,0)
}function Y(m,l,k){function i(p,r,o){if(r.length<=o){return[]
}var q=r[o],n=-d(q,p,l,k,0);
return[n].concat(i(f(c(q,n),p),r,++o))
}m=g.call(m);
var j=i([],m.reverse(),0);
return j.reverse()
}function X(p,n,k,q){var l=b(p,n,k),o=Y(p,n,k);
if(q=="left"){o=l
}else{if(q=="right"){l=o
}}for(var m=0,j=[];
m<l.length;
m++){j[m]=(l[m]+o[m])/2
}return j
}function Z(j,v,k,AC,AA){var m=AC.multitree;
var u=["x","y"],q=["width","height"];
var l=+(AA=="left"||AA=="right");
var r=u[l],AB=u[1-l];
var x=AC.Node;
var o=q[l],z=q[1-l];
var n=AC.siblingOffset;
var y=AC.subtreeOffset;
var w=AC.align;
var i=Graph.Util;
function t(AD,AH,AL){var s=(x.overridable&&AD.data["$"+o])||x[o];
var AK=AH||((x.overridable&&AD.data["$"+z])||x[z]);
var AO=[],AM=[],AI=false;
var p=AK+AC.levelDistance;
i.eachSubnode(AD,function(AQ){if(AQ.exist&&(!m||("$orn" in AQ.data)&&AQ.data.$orn==AA)){if(!AI){AI=e(j,AC,AQ._depth,AA)
}var AP=t(AQ,AI[z],AL+p);
AO.push(AP.tree);
AM.push(AP.extent)
}});
var AG=X(AM,y,n,w);
for(var AF=0,AE=[],AJ=[];
AF<AO.length;
AF++){h(AO[AF],k,AG[AF],AA);
AJ.push(c(AM[AF],AG[AF]))
}var AN=[[-s/2,s/2]].concat(a(AJ));
AD[k][r]=0;
if(AA=="top"||AA=="left"){AD[k][AB]=AL
}else{AD[k][AB]=-AL
}return{tree:AD,extent:AN}
}t(v,false,0)
}this.ST=(function(){var j=[];
function k(q){q=q||this.clickedNode;
var m=this.geom,u=Graph.Util;
var v=this.graph;
var o=this.canvas;
var l=q._depth,r=[];
u.eachNode(v,function(w){if(w.exist&&!w.selected){if(u.isDescendantOf(w,q.id)){if(w._depth<=l){r.push(w)
}}else{r.push(w)
}}});
var s=m.getRightLevelToShow(q,o);
u.eachLevel(q,s,s,function(w){if(w.exist&&!w.selected){r.push(w)
}});
for(var t=0;
t<j.length;
t++){var p=this.graph.getNode(j[t]);
if(!u.isDescendantOf(p,q.id)){r.push(p)
}}return r
}function i(o){var n=[],m=Graph.Util,l=this.config;
o=o||this.clickedNode;
m.eachLevel(this.clickedNode,0,l.levelsToShow,function(p){if(l.multitree&&!("$orn" in p.data)&&m.anySubnode(p,function(q){return q.exist&&!q.drawn
})){n.push(p)
}else{if(p.drawn&&!m.anySubnode(p,"drawn")){n.push(p)
}}});
return n
}return new O({Implements:Loader,initialize:function(o,l){var m={onBeforeCompute:B,onAfterCompute:B,onCreateLabel:B,onPlaceLabel:B,onComplete:B,onBeforePlotNode:B,onAfterPlotNode:B,onBeforePlotLine:B,onAfterPlotLine:B,request:false};
var n={orientation:"left",labelContainer:o.id+"-label",levelsToShow:2,subtreeOffset:8,siblingOffset:5,levelDistance:30,withLabels:true,clearCanvas:true,align:"center",indent:10,multitree:false,constrained:true,Node:{overridable:false,type:"rectangle",color:"#ccb",lineWidth:1,height:20,width:90,dim:15,align:"center"},Edge:{overridable:false,type:"line",color:"#ccc",dim:15,lineWidth:1},duration:700,fps:25,transition:Trans.Quart.easeInOut};
this.controller=this.config=R(n,m,l);
this.canvas=o;
this.graphOptions={complex:true};
this.graph=new Graph(this.graphOptions);
this.fx=new ST.Plot(this);
this.op=new ST.Op(this);
this.group=new ST.Group(this);
this.geom=new ST.Geom(this);
this.clickedNode=null
},plot:function(){this.fx.plot(this.controller)
},switchPosition:function(q,p,o){var l=this.geom,m=this.fx,n=this;
if(!m.busy){m.busy=true;
this.contract({onComplete:function(){l.switchOrientation(q);
n.compute("endPos",false);
m.busy=false;
if(p=="animate"){n.onClick(n.clickedNode.id,o)
}else{if(p=="replot"){n.select(n.clickedNode.id,o)
}}}},q)
}},switchAlignment:function(n,m,l){this.config.align=n;
if(m=="animate"){this.select(this.clickedNode.id,l)
}else{if(m=="replot"){this.onClick(this.clickedNode.id,l)
}}},addNodeInPath:function(l){j.push(l);
this.select((this.clickedNode&&this.clickedNode.id)||this.root)
},clearNodesInPath:function(l){j.length=0;
this.select((this.clickedNode&&this.clickedNode.id)||this.root)
},refresh:function(){this.reposition();
this.select((this.clickedNode&&this.clickedNode.id)||this.root)
},reposition:function(){Graph.Util.computeLevels(this.graph,this.root,0,"ignore");
this.geom.setRightLevelToShow(this.clickedNode,this.canvas);
Graph.Util.eachNode(this.graph,function(l){if(l.exist){l.drawn=true
}});
this.compute("endPos")
},compute:function(n,m){var o=n||"startPos";
var l=this.graph.getNode(this.root);
C(l,{drawn:true,exist:true,selected:true});
if(!!m||!("_depth" in l)){Graph.Util.computeLevels(this.graph,this.root,0,"ignore")
}this.computePositions(l,o)
},computePositions:function(p,l){var n=this.config;
var m=n.multitree;
var s=n.align;
var o=s!=="center"&&n.indent;
var t=n.orientation;
var r=m?["top","right","bottom","left"]:[t];
var q=this;
G(r,function(u){Z(q.graph,p,l,q.config,u);
var v=["x","y"][+(u=="left"||u=="right")];
(function w(x){Graph.Util.eachSubnode(x,function(y){if(y.exist&&(!m||("$orn" in y.data)&&y.data.$orn==u)){y[l][v]+=x[l][v];
if(o){y[l][v]+=s=="left"?o:-o
}w(y)
}})
})(p)
})
},requestNodes:function(o,p){var m=R(this.controller,p),l=this.config.levelsToShow,n=Graph.Util;
if(m.request){var r=[],q=o._depth;
n.eachLevel(o,0,l,function(s){if(s.drawn&&!n.anySubnode(s)){r.push(s);
s._level=l-(s._depth-q)
}});
this.group.requestNodes(r,m)
}else{m.onComplete()
}},contract:function(p,q){var o=this.config.orientation;
var l=this.geom,n=this.group;
if(q){l.switchOrientation(q)
}var m=k.call(this);
if(q){l.switchOrientation(o)
}n.contract(m,R(this.controller,p))
},move:function(m,n){this.compute("endPos",false);
var l=n.Move,o={x:l.offsetX,y:l.offsetY};
if(l.enable){this.geom.translate(m.endPos.add(o).$scale(-1),"endPos")
}this.fx.animate(R(this.controller,{modes:["linear"]},n))
},expand:function(m,n){var l=i.call(this,m);
this.group.expand(l,R(this.controller,n))
},selectPath:function(p){var o=Graph.Util,n=this;
o.eachNode(this.graph,function(r){r.selected=false
});
function q(s){if(s==null||s.selected){return 
}s.selected=true;
G(n.group.getSiblings([s])[s.id],function(t){t.exist=true;
t.drawn=true
});
var r=o.getParents(s);
r=(r.length>0)?r[0]:null;
q(r)
}for(var l=0,m=[p.id].concat(j);
l<m.length;
l++){q(this.graph.getNode(m[l]))
}},setRoot:function(s,r,q){var p=this,n=this.canvas;
var l=this.graph.getNode(this.root);
var m=this.graph.getNode(s);
function o(){if(this.config.multitree&&m.data.$orn){var u=m.data.$orn;
var v={left:"right",right:"left",top:"bottom",bottom:"top"}[u];
l.data.$orn=v;
(function t(w){Graph.Util.eachSubnode(w,function(x){if(x.id!=s){x.data.$orn=v;
t(x)
}})
})(l);
delete m.data.$orn
}this.root=s;
this.clickedNode=m;
Graph.Util.computeLevels(this.graph,this.root,0,"ignore")
}delete l.data.$orns;
if(r=="animate"){this.onClick(s,{onBeforeMove:function(){o.call(p);
p.selectPath(m)
}})
}else{if(r=="replot"){o.call(this);
this.select(this.root)
}}},addSubtree:function(l,n,m){if(n=="replot"){this.op.sum(l,C({type:"replot"},m||{}))
}else{if(n=="animate"){this.op.sum(l,C({type:"fade:seq"},m||{}))
}}},removeSubtree:function(q,m,p,o){var n=this.graph.getNode(q),l=[];
Graph.Util.eachLevel(n,+!m,false,function(r){l.push(r.id)
});
if(p=="replot"){this.op.removeNode(l,C({type:"replot"},o||{}))
}else{if(p=="animate"){this.op.removeNode(l,C({type:"fade:seq"},o||{}))
}}},select:function(l,o){var t=this.group,r=this.geom;
var p=this.graph.getNode(l),n=this.canvas;
var s=this.graph.getNode(this.root);
var m=R(this.controller,o);
var q=this;
m.onBeforeCompute(p);
this.selectPath(p);
this.clickedNode=p;
this.requestNodes(p,{onComplete:function(){t.hide(t.prepare(k.call(q)),m);
r.setRightLevelToShow(p,n);
q.compute("pos");
Graph.Util.eachNode(q.graph,function(v){var u=v.pos.getc(true);
v.startPos.setc(u.x,u.y);
v.endPos.setc(u.x,u.y);
v.visited=false
});
q.geom.translate(p.endPos.scale(-1),["pos","startPos","endPos"]);
t.show(i.call(q));
q.plot();
m.onAfterCompute(q.clickedNode);
m.onComplete()
}})
},onClick:function(n,u){var o=this.canvas,s=this,r=this.fx,t=Graph.Util,l=this.geom;
var q={Move:{enable:true,offsetX:0,offsetY:0},onBeforeRequest:B,onBeforeContract:B,onBeforeMove:B,onBeforeExpand:B};
var m=R(this.controller,q,u);
if(!this.busy){this.busy=true;
var p=this.graph.getNode(n);
this.selectPath(p,this.clickedNode);
this.clickedNode=p;
m.onBeforeCompute(p);
m.onBeforeRequest(p);
this.requestNodes(p,{onComplete:function(){m.onBeforeContract(p);
s.contract({onComplete:function(){l.setRightLevelToShow(p,o);
m.onBeforeMove(p);
s.move(p,{Move:m.Move,onComplete:function(){m.onBeforeExpand(p);
s.expand(p,{onComplete:function(){s.busy=false;
m.onAfterCompute(n);
m.onComplete()
}})
}})
}})
}})
}}})
})();
ST.Op=new O({Implements:Graph.Op,initialize:function(i){this.viz=i
}});
ST.Group=new O({initialize:function(i){this.viz=i;
this.canvas=i.canvas;
this.config=i.config;
this.animation=new Animation;
this.nodes=null
},requestNodes:function(o,n){var m=0,k=o.length,q={};
var l=function(){n.onComplete()
};
var j=this.viz;
if(k==0){l()
}for(var p=0;
p<k;
p++){q[o[p].id]=o[p];
n.request(o[p].id,o[p]._level,{onComplete:function(r,i){if(i&&i.children){i.id=r;
j.op.sum(i,{type:"nothing"})
}if(++m==k){Graph.Util.computeLevels(j.graph,j.root,0);
l()
}}})
}},contract:function(k,j){var m=Graph.Util;
var i=this.viz;
var l=this;
k=this.prepare(k);
this.animation.setOptions(R(j,{$animating:false,compute:function(n){if(n==1){n=0.99
}l.plotStep(1-n,j,this.$animating);
this.$animating="contract"
},complete:function(){l.hide(k,j)
}})).start()
},hide:function(l,k){var o=Graph.Util,j=this.viz;
for(var m=0;
m<l.length;
m++){if(true||!k||!k.request){o.eachLevel(l[m],1,false,function(i){if(i.exist){C(i,{drawn:false,exist:false})
}})
}else{var n=[];
o.eachLevel(l[m],1,false,function(i){n.push(i.id)
});
j.op.removeNode(n,{type:"nothing"});
j.fx.clearLabels()
}}k.onComplete()
},expand:function(j,i){var l=this,k=Graph.Util;
this.show(j);
this.animation.setOptions(R(i,{$animating:false,compute:function(m){l.plotStep(m,i,this.$animating);
this.$animating="expand"
},complete:function(){l.plotStep(undefined,i,false);
i.onComplete()
}})).start()
},show:function(i){var k=Graph.Util,j=this.config;
this.prepare(i);
G(i,function(m){if(j.multitree&&!("$orn" in m.data)){delete m.data.$orns;
var l=" ";
k.eachSubnode(m,function(n){if(("$orn" in n.data)&&l.indexOf(n.data.$orn)<0&&n.exist&&!n.drawn){l+=n.data.$orn+" "
}});
m.data.$orns=l
}k.eachLevel(m,0,j.levelsToShow,function(o){if(o.exist){o.drawn=true
}})
})
},prepare:function(i){this.nodes=this.getNodesWithChildren(i);
return this.nodes
},getNodesWithChildren:function(m){var l=[],q=Graph.Util,o=this.config,k=this.viz.root;
m.sort(function(j,i){return(j._depth<=i._depth)-(j._depth>=i._depth)
});
for(var p=0;
p<m.length;
p++){if(q.anySubnode(m[p],"exist")){for(var n=p+1,r=false;
!r&&n<m.length;
n++){if(!o.multitree||"$orn" in m[n].data){r=r||q.isDescendantOf(m[p],m[n].id)
}}if(!r){l.push(m[p])
}}}return l
},plotStep:function(u,p,w){var t=this.viz,m=this.config,l=t.canvas,v=l.getCtx(),j=this.nodes,r=Graph.Util;
var o,n;
var k={};
for(o=0;
o<j.length;
o++){n=j[o];
k[n.id]=[];
var s=m.multitree&&!("$orn" in n.data);
var q=s&&n.data.$orns;
r.eachSubgraph(n,function(i){if(s&&q&&q.indexOf(i.data.$orn)>0&&i.drawn){i.drawn=false;
k[n.id].push(i)
}else{if((!s||!q)&&i.drawn){i.drawn=false;
k[n.id].push(i)
}}});
n.drawn=true
}if(j.length>0){t.fx.plot()
}for(o in k){G(k[o],function(i){i.drawn=true
})
}for(o=0;
o<j.length;
o++){n=j[o];
v.save();
t.fx.plotSubtree(n,p,u,w);
v.restore()
}},getSiblings:function(i){var k={},j=Graph.Util;
G(i,function(o){var m=j.getParents(o);
if(m.length==0){k[o.id]=[o]
}else{var l=[];
j.eachSubnode(m[0],function(n){l.push(n)
});
k[o.id]=l
}});
return k
}});
ST.Geom=new O({initialize:function(i){this.viz=i;
this.config=i.config;
this.node=i.config.Node;
this.edge=i.config.Edge
},translate:function(j,i){i=J(i);
Graph.Util.eachNode(this.viz.graph,function(k){G(i,function(l){k[l].$add(j)
})
})
},switchOrientation:function(i){this.config.orientation=i
},dispatch:function(){var j=Array.prototype.slice.call(arguments);
var k=j.shift(),i=j.length;
var l=function(m){return typeof m=="function"?m():m
};
if(i==2){return(k=="top"||k=="bottom")?l(j[0]):l(j[1])
}else{if(i==4){switch(k){case"top":return l(j[0]);
case"right":return l(j[1]);
case"bottom":return l(j[2]);
case"left":return l(j[3])
}}}return undefined
},getSize:function(j,i){var l=this.node,m=j.data,k=this.config;
var p=l.overridable,q=k.siblingOffset;
var t=(this.config.multitree&&("$orn" in j.data)&&j.data.$orn)||this.config.orientation;
var r=(p&&m.$width||l.width)+q;
var o=(p&&m.$height||l.height)+q;
if(!i){return this.dispatch(t,o,r)
}else{return this.dispatch(t,r,o)
}},getTreeBaseSize:function(m,n,j){var k=this.getSize(m,true),i=0,l=this;
if(j(n,m)){return k
}if(n===0){return 0
}Graph.Util.eachSubnode(m,function(o){i+=l.getTreeBaseSize(o,n-1,j)
});
return(k>i?k:i)+this.config.subtreeOffset
},getEdge:function(i,n,q){var m=function(s,r){return function(){return i.pos.add(new Complex(s,r))
}
};
var l=this.node;
var o=this.node.overridable,j=i.data;
var p=o&&j.$width||l.width;
var k=o&&j.$height||l.height;
if(n=="begin"){if(l.align=="center"){return this.dispatch(q,m(0,k/2),m(-p/2,0),m(0,-k/2),m(p/2,0))
}else{if(l.align=="left"){return this.dispatch(q,m(0,k),m(0,0),m(0,0),m(p,0))
}else{if(l.align=="right"){return this.dispatch(q,m(0,0),m(-p,0),m(0,-k),m(0,0))
}else{throw"align: not implemented"
}}}}else{if(n=="end"){if(l.align=="center"){return this.dispatch(q,m(0,-k/2),m(p/2,0),m(0,k/2),m(-p/2,0))
}else{if(l.align=="left"){return this.dispatch(q,m(0,0),m(p,0),m(0,k),m(0,0))
}else{if(l.align=="right"){return this.dispatch(q,m(0,-k),m(0,0),m(0,0),m(-p,0))
}else{throw"align: not implemented"
}}}}}},getScaledTreePosition:function(i,j){var l=this.node;
var o=this.node.overridable,k=i.data;
var p=(o&&k.$width||l.width);
var m=(o&&k.$height||l.height);
var q=(this.config.multitree&&("$orn" in i.data)&&i.data.$orn)||this.config.orientation;
var n=function(s,r){return function(){return i.pos.add(new Complex(s,r)).$scale(1-j)
}
};
if(l.align=="left"){return this.dispatch(q,n(0,m),n(0,0),n(0,0),n(p,0))
}else{if(l.align=="center"){return this.dispatch(q,n(0,m/2),n(-p/2,0),n(0,-m/2),n(p/2,0))
}else{if(l.align=="right"){return this.dispatch(q,n(0,0),n(-p,0),n(0,-m),n(0,0))
}else{throw"align: not implemented"
}}}},treeFitsInCanvas:function(n,i,o){var k=i.getSize(n);
var l=(this.config.multitree&&("$orn" in n.data)&&n.data.$orn)||this.config.orientation;
var j=this.dispatch(l,k.width,k.height);
var m=this.getTreeBaseSize(n,o,function(q,p){return q===0||!Graph.Util.anySubnode(p)
});
return(m<j)
},setRightLevelToShow:function(k,i){var l=this.getRightLevelToShow(k,i),j=this.viz.fx;
Graph.Util.eachLevel(k,0,this.config.levelsToShow,function(o){var m=o._depth-k._depth;
if(m>l){o.drawn=false;
o.exist=false;
j.hideLabel(o,false)
}else{o.exist=true
}});
k.drawn=true
},getRightLevelToShow:function(l,j){var i=this.config;
var m=i.levelsToShow;
var k=i.constrained;
if(!k){return m
}while(!this.treeFitsInCanvas(l,j,m)&&m>1){m--
}return m
}});
ST.Plot=new O({Implements:Graph.Plot,initialize:function(i){this.viz=i;
this.config=i.config;
this.node=this.config.Node;
this.edge=this.config.Edge;
this.animation=new Animation;
this.nodeTypes=new ST.Plot.NodeTypes;
this.edgeTypes=new ST.Plot.EdgeTypes
},plotSubtree:function(n,m,p,k){var i=this.viz,l=i.canvas;
p=Math.min(Math.max(0.001,p),1);
if(p>=0){n.drawn=false;
var j=l.getCtx();
var o=i.geom.getScaledTreePosition(n,p);
j.translate(o.x,o.y);
j.scale(p,p)
}this.plotTree(n,!p,m,k);
if(p>=0){n.drawn=true
}},plotTree:function(l,m,i,s){var o=this,q=this.viz,j=q.canvas,k=this.config,r=j.getCtx();
var p=k.multitree&&!("$orn" in l.data);
var n=p&&l.data.$orns;
Graph.Util.eachSubnode(l,function(u){if((!p||n.indexOf(u.data.$orn)>0)&&u.exist&&u.drawn){var t=l.getAdjacency(u.id);
!s&&i.onBeforePlotLine(t);
r.globalAlpha=Math.min(l.alpha,u.alpha);
o.plotLine(t,j,s);
!s&&i.onAfterPlotLine(t);
o.plotTree(u,m,i,s)
}});
if(l.drawn){r.globalAlpha=l.alpha;
!s&&i.onBeforePlotNode(l);
this.plotNode(l,j,s);
!s&&i.onAfterPlotNode(l);
if(m&&r.globalAlpha>=0.95){this.plotLabel(j,l,i)
}else{this.hideLabel(l,false)
}}else{this.hideLabel(l,true)
}},placeLabel:function(t,l,o){var r=l.pos.getc(true),m=this.node,j=this.viz.canvas;
var s=m.overridable&&l.data.$width||m.width;
var n=m.overridable&&l.data.$height||m.height;
var p=j.getSize();
var k,q;
if(m.align=="center"){k={x:Math.round(r.x-s/2+p.width/2),y:Math.round(r.y-n/2+p.height/2)}
}else{if(m.align=="left"){q=this.config.orientation;
if(q=="bottom"||q=="top"){k={x:Math.round(r.x-s/2+p.width/2),y:Math.round(r.y+p.height/2)}
}else{k={x:Math.round(r.x+p.width/2),y:Math.round(r.y-n/2+p.height/2)}
}}else{if(m.align=="right"){q=this.config.orientation;
if(q=="bottom"||q=="top"){k={x:Math.round(r.x-s/2+p.width/2),y:Math.round(r.y-n+p.height/2)}
}else{k={x:Math.round(r.x-s+p.width/2),y:Math.round(r.y-n/2+p.height/2)}
}}else{throw"align: not implemented"
}}}var i=t.style;
i.left=k.x+"px";
i.top=k.y+"px";
i.display=this.fitsInCanvas(k,j)?"":"none";
o.onPlaceLabel(t,l)
},getAlignedPos:function(n,l,i){var k=this.node;
var m,j;
if(k.align=="center"){m={x:n.x-l/2,y:n.y-i/2}
}else{if(k.align=="left"){j=this.config.orientation;
if(j=="bottom"||j=="top"){m={x:n.x-l/2,y:n.y}
}else{m={x:n.x,y:n.y-i/2}
}}else{if(k.align=="right"){j=this.config.orientation;
if(j=="bottom"||j=="top"){m={x:n.x-l/2,y:n.y-i}
}else{m={x:n.x-l,y:n.y-i/2}
}}else{throw"align: not implemented"
}}}return m
},getOrientation:function(i){var k=this.config;
var j=k.orientation;
if(k.multitree){var l=i.nodeFrom;
var m=i.nodeTo;
j=(("$orn" in l.data)&&l.data.$orn)||(("$orn" in m.data)&&m.data.$orn)
}return j
}});
ST.Plot.NodeTypes=new O({none:function(){},circle:function(m,j){var p=m.pos.getc(true),l=this.node,n=m.data;
var k=l.overridable&&n;
var o=k&&n.$dim||l.dim;
var i=this.getAlignedPos(p,o*2,o*2);
j.path("fill",function(q){q.arc(i.x+o,i.y+o,o,0,Math.PI*2,true)
})
},square:function(m,j){var p=m.pos.getc(true),l=this.node,n=m.data;
var k=l.overridable&&n;
var o=k&&n.$dim||l.dim;
var i=this.getAlignedPos(p,o,o);
j.getCtx().fillRect(i.x,i.y,o,o)
},ellipse:function(k,j){var n=k.pos.getc(true),o=this.node,l=k.data;
var m=o.overridable&&l;
var i=(m&&l.$width||o.width)/2;
var q=(m&&l.$height||o.height)/2;
var p=this.getAlignedPos(n,i*2,q*2);
var r=j.getCtx();
r.save();
r.scale(i/q,q/i);
j.path("fill",function(s){s.arc((p.x+i)*(q/i),(p.y+q)*(i/q),q,0,Math.PI*2,true)
});
r.restore()
},rectangle:function(k,j){var n=k.pos.getc(true),o=this.node,l=k.data;
var m=o.overridable&&l;
var i=m&&l.$width||o.width;
var q=m&&l.$height||o.height;
var p=this.getAlignedPos(n,i,q);
j.getCtx().fillRect(p.x,p.y,i,q)
}});
ST.Plot.EdgeTypes=new O({none:function(){},line:function(j,l){var k=this.getOrientation(j);
var n=j.nodeFrom,o=j.nodeTo;
var m=this.viz.geom.getEdge(n._depth<o._depth?n:o,"begin",k);
var i=this.viz.geom.getEdge(n._depth<o._depth?o:n,"end",k);
l.path("stroke",function(p){p.moveTo(m.x,m.y);
p.lineTo(i.x,i.y)
})
},"quadratic:begin":function(r,j){var q=this.getOrientation(r);
var m=r.data,i=this.edge;
var o=r.nodeFrom,s=r.nodeTo;
var k=this.viz.geom.getEdge(o._depth<s._depth?o:s,"begin",q);
var l=this.viz.geom.getEdge(o._depth<s._depth?s:o,"end",q);
var p=i.overridable&&m;
var n=p&&m.$dim||i.dim;
switch(q){case"left":j.path("stroke",function(t){t.moveTo(k.x,k.y);
t.quadraticCurveTo(k.x+n,k.y,l.x,l.y)
});
break;
case"right":j.path("stroke",function(t){t.moveTo(k.x,k.y);
t.quadraticCurveTo(k.x-n,k.y,l.x,l.y)
});
break;
case"top":j.path("stroke",function(t){t.moveTo(k.x,k.y);
t.quadraticCurveTo(k.x,k.y+n,l.x,l.y)
});
break;
case"bottom":j.path("stroke",function(t){t.moveTo(k.x,k.y);
t.quadraticCurveTo(k.x,k.y-n,l.x,l.y)
});
break
}},"quadratic:end":function(r,j){var q=this.getOrientation(r);
var m=r.data,i=this.edge;
var o=r.nodeFrom,s=r.nodeTo;
var k=this.viz.geom.getEdge(o._depth<s._depth?o:s,"begin",q);
var l=this.viz.geom.getEdge(o._depth<s._depth?s:o,"end",q);
var p=i.overridable&&m;
var n=p&&m.$dim||i.dim;
switch(q){case"left":j.path("stroke",function(t){t.moveTo(k.x,k.y);
t.quadraticCurveTo(l.x-n,l.y,l.x,l.y)
});
break;
case"right":j.path("stroke",function(t){t.moveTo(k.x,k.y);
t.quadraticCurveTo(l.x+n,l.y,l.x,l.y)
});
break;
case"top":j.path("stroke",function(t){t.moveTo(k.x,k.y);
t.quadraticCurveTo(l.x,l.y-n,l.x,l.y)
});
break;
case"bottom":j.path("stroke",function(t){t.moveTo(k.x,k.y);
t.quadraticCurveTo(l.x,l.y+n,l.x,l.y)
});
break
}},bezier:function(r,j){var m=r.data,i=this.edge;
var q=this.getOrientation(r);
var o=r.nodeFrom,s=r.nodeTo;
var k=this.viz.geom.getEdge(o._depth<s._depth?o:s,"begin",q);
var l=this.viz.geom.getEdge(o._depth<s._depth?s:o,"end",q);
var p=i.overridable&&m;
var n=p&&m.$dim||i.dim;
switch(q){case"left":j.path("stroke",function(t){t.moveTo(k.x,k.y);
t.bezierCurveTo(k.x+n,k.y,l.x-n,l.y,l.x,l.y)
});
break;
case"right":j.path("stroke",function(t){t.moveTo(k.x,k.y);
t.bezierCurveTo(k.x-n,k.y,l.x+n,l.y,l.x,l.y)
});
break;
case"top":j.path("stroke",function(t){t.moveTo(k.x,k.y);
t.bezierCurveTo(k.x,k.y+n,l.x,l.y-n,l.x,l.y)
});
break;
case"bottom":j.path("stroke",function(t){t.moveTo(k.x,k.y);
t.bezierCurveTo(k.x,k.y-n,l.x,l.y+n,l.x,l.y)
});
break
}},arrow:function(q,l){var w=this.getOrientation(q);
var u=q.nodeFrom,m=q.nodeTo;
var z=q.data,p=this.edge;
var r=p.overridable&&z;
var o=r&&z.$dim||p.dim;
if(r&&z.$direction&&z.$direction.length>1){var k={};
k[u.id]=u;
k[m.id]=m;
var v=z.$direction;
u=k[v[0]];
m=k[v[1]]
}var n=this.viz.geom.getEdge(u,"begin",w);
var s=this.viz.geom.getEdge(m,"end",w);
var t=new Complex(s.x-n.x,s.y-n.y);
t.$scale(o/t.norm());
var x=new Complex(s.x-t.x,s.y-t.y);
var y=new Complex(-t.y/2,t.x/2);
var j=x.add(y),i=x.$add(y.$scale(-1));
l.path("stroke",function(AA){AA.moveTo(n.x,n.y);
AA.lineTo(s.x,s.y)
});
l.path("fill",function(AA){AA.moveTo(j.x,j.y);
AA.lineTo(i.x,i.y);
AA.lineTo(s.x,s.y)
})
}})
})();
var AngularWidth={setAngularWidthForNodes:function(){var X=this.config.Node;
var Z=X.overridable;
var Y=X.dim;
Graph.Util.eachBFS(this.graph,this.root,function(c,a){var b=(Z&&c.data&&c.data.$aw)||Y;
c._angularWidth=b/a
},"ignore")
},setSubtreesAngularWidth:function(){var X=this;
Graph.Util.eachNode(this.graph,function(Y){X.setSubtreeAngularWidth(Y)
},"ignore")
},setSubtreeAngularWidth:function(a){var Z=this,Y=a._angularWidth,X=0;
Graph.Util.eachSubnode(a,function(b){Z.setSubtreeAngularWidth(b);
X+=b._treeAngularWidth
},"ignore");
a._treeAngularWidth=Math.max(Y,X)
},computeAngularWidths:function(){this.setAngularWidthForNodes();
this.setSubtreesAngularWidth()
}};
this.RGraph=new O({Implements:[Loader,AngularWidth],initialize:function(a,X){var Z={labelContainer:a.id+"-label",interpolation:"linear",levelDistance:100,withLabels:true,Node:{overridable:false,type:"circle",dim:3,color:"#ccb",width:5,height:5,lineWidth:1},Edge:{overridable:false,type:"line",color:"#ccb",lineWidth:1},fps:40,duration:2500,transition:Trans.Quart.easeInOut,clearCanvas:true};
var Y={onBeforeCompute:B,onAfterCompute:B,onCreateLabel:B,onPlaceLabel:B,onComplete:B,onBeforePlotLine:B,onAfterPlotLine:B,onBeforePlotNode:B,onAfterPlotNode:B};
this.controller=this.config=R(Z,Y,X);
this.graphOptions={complex:false,Node:{selected:false,exist:true,drawn:true}};
this.graph=new Graph(this.graphOptions);
this.fx=new RGraph.Plot(this);
this.op=new RGraph.Op(this);
this.json=null;
this.canvas=a;
this.root=null;
this.busy=false;
this.parent=false
},refresh:function(){this.compute();
this.plot()
},reposition:function(){this.compute("endPos")
},plot:function(){this.fx.plot()
},compute:function(Y){var Z=Y||["pos","startPos","endPos"];
var X=this.graph.getNode(this.root);
X._depth=0;
Graph.Util.computeLevels(this.graph,this.root,0,"ignore");
this.computeAngularWidths();
this.computePositions(Z)
},computePositions:function(e){var Y=J(e);
var d=this.graph;
var c=Graph.Util;
var X=this.graph.getNode(this.root);
var b=this.parent;
var Z=this.config;
for(var a=0;
a<Y.length;
a++){X[Y[a]]=L(0,0)
}X.angleSpan={begin:0,end:2*Math.PI};
X._rel=1;
c.eachBFS(this.graph,this.root,function(j){var n=j.angleSpan.end-j.angleSpan.begin;
var q=(j._depth+1)*Z.levelDistance;
var o=j.angleSpan.begin;
var p=0,f=[];
c.eachSubnode(j,function(i){p+=i._treeAngularWidth;
f.push(i)
},"ignore");
if(b&&b.id==j.id&&f.length>0&&f[0].dist){f.sort(function(k,i){return(k.dist>=i.dist)-(k.dist<=i.dist)
})
}for(var l=0;
l<f.length;
l++){var h=f[l];
if(!h._flag){h._rel=h._treeAngularWidth/p;
var r=h._rel*n;
var g=o+r/2;
for(var m=0;
m<Y.length;
m++){h[Y[m]]=L(g,q)
}h.angleSpan={begin:o,end:o+r};
o+=r
}}},"ignore")
},getNodeAndParentAngle:function(e){var Z=false;
var d=this.graph.getNode(e);
var b=Graph.Util.getParents(d);
var a=(b.length>0)?b[0]:false;
if(a){var X=a.pos.getc(),c=d.pos.getc();
var Y=X.add(c.scale(-1));
Z=Math.atan2(Y.y,Y.x);
if(Z<0){Z+=2*Math.PI
}}return{parent:a,theta:Z}
},tagChildren:function(b,d){if(b.angleSpan){var c=[];
Graph.Util.eachAdjacency(b,function(e){c.push(e.nodeTo)
},"ignore");
var X=c.length;
for(var a=0;
a<X&&d!=c[a].id;
a++){}for(var Z=(a+1)%X,Y=0;
d!=c[Z].id;
Z=(Z+1)%X){c[Z].dist=Y++
}}},onClick:function(b,Y){if(this.root!=b&&!this.busy){this.busy=true;
this.root=b;
that=this;
this.controller.onBeforeCompute(this.graph.getNode(b));
var Z=this.getNodeAndParentAngle(b);
this.tagChildren(Z.parent,b);
this.parent=Z.parent;
this.compute("endPos");
var X=Z.theta-Z.parent.endPos.theta;
Graph.Util.eachNode(this.graph,function(c){c.endPos.set(c.endPos.getp().add(L(X,0)))
});
var a=this.config.interpolation;
Y=R({onComplete:B},Y||{});
this.fx.animate(R({hideLabels:true,modes:[a]},Y,{onComplete:function(){that.busy=false;
Y.onComplete()
}}))
}}});
RGraph.Op=new O({Implements:Graph.Op,initialize:function(X){this.viz=X
}});
RGraph.Plot=new O({Implements:Graph.Plot,initialize:function(X){this.viz=X;
this.config=X.config;
this.node=X.config.Node;
this.edge=X.config.Edge;
this.animation=new Animation;
this.nodeTypes=new RGraph.Plot.NodeTypes;
this.edgeTypes=new RGraph.Plot.EdgeTypes
},placeLabel:function(Y,c,Z){var e=c.pos.getc(true),a=this.viz.canvas;
var X=a.getSize();
var d={x:Math.round(e.x+X.width/2),y:Math.round(e.y+X.height/2)};
var b=Y.style;
b.left=d.x+"px";
b.top=d.y+"px";
b.display=this.fitsInCanvas(d,a)?"":"none";
Z.onPlaceLabel(Y,c)
}});
RGraph.Plot.NodeTypes=new O({none:function(){},circle:function(Z,X){var c=Z.pos.getc(true),Y=this.node,b=Z.data;
var a=Y.overridable&&b&&b.$dim||Y.dim;
X.path("fill",function(d){d.arc(c.x,c.y,a,0,Math.PI*2,true)
})
},square:function(a,X){var d=a.pos.getc(true),Z=this.node,c=a.data;
var b=Z.overridable&&c&&c.$dim||Z.dim;
var Y=2*b;
X.getCtx().fillRect(d.x-b,d.y-b,Y,Y)
},rectangle:function(b,Y){var d=b.pos.getc(true),a=this.node,c=b.data;
var Z=a.overridable&&c&&c.$width||a.width;
var X=a.overridable&&c&&c.$height||a.height;
Y.getCtx().fillRect(d.x-Z/2,d.y-X/2,Z,X)
},triangle:function(b,Y){var f=b.pos.getc(true),g=this.node,c=b.data;
var X=g.overridable&&c&&c.$dim||g.dim;
var a=f.x,Z=f.y-X,i=a-X,h=f.y+X,e=a+X,d=h;
Y.path("fill",function(j){j.moveTo(a,Z);
j.lineTo(i,h);
j.lineTo(e,d)
})
},star:function(Z,Y){var d=Z.pos.getc(true),e=this.node,b=Z.data;
var X=e.overridable&&b&&b.$dim||e.dim;
var f=Y.getCtx(),c=Math.PI/5;
f.save();
f.translate(d.x,d.y);
f.beginPath();
f.moveTo(X,0);
for(var a=0;
a<9;
a++){f.rotate(c);
if(a%2==0){f.lineTo((X/0.525731)*0.200811,0)
}else{f.lineTo(X,0)
}}f.closePath();
f.fill();
f.restore()
}});
RGraph.Plot.EdgeTypes=new O({none:function(){},line:function(X,Y){var a=X.nodeFrom.pos.getc(true);
var Z=X.nodeTo.pos.getc(true);
Y.path("stroke",function(b){b.moveTo(a.x,a.y);
b.lineTo(Z.x,Z.y)
})
},arrow:function(j,b){var d=j.nodeFrom,a=j.nodeTo;
var e=j.data,X=this.edge;
var i=X.overridable&&e;
var l=i&&e.$dim||14;
if(i&&e.$direction&&e.$direction.length>1){var Y={};
Y[d.id]=d;
Y[a.id]=a;
var Z=e.$direction;
d=Y[Z[0]];
a=Y[Z[1]]
}var n=d.pos.getc(true),c=a.pos.getc(true);
var h=new Complex(c.x-n.x,c.y-n.y);
h.$scale(l/h.norm());
var f=new Complex(c.x-h.x,c.y-h.y);
var g=new Complex(-h.y/2,h.x/2);
var m=f.add(g),k=f.$add(g.$scale(-1));
b.path("stroke",function(o){o.moveTo(n.x,n.y);
o.lineTo(c.x,c.y)
});
b.path("fill",function(o){o.moveTo(m.x,m.y);
o.lineTo(k.x,k.y);
o.lineTo(c.x,c.y)
})
}});
Complex.prototype.moebiusTransformation=function(Z){var X=this.add(Z);
var Y=Z.$conjugate().$prod(this);
Y.x++;
return X.$div(Y)
};
Graph.Util.getClosestNodeToOrigin=function(Y,Z,X){return this.getClosestNodeToPos(Y,Polar.KER,Z,X)
};
Graph.Util.getClosestNodeToPos=function(Z,c,b,X){var Y=null;
b=b||"pos";
c=c&&c.getc(true)||Complex.KER;
var a=function(e,d){var g=e.x-d.x,f=e.y-d.y;
return g*g+f*f
};
this.eachNode(Z,function(d){Y=(Y==null||a(d[b].getc(true),c)<a(Y[b].getc(true),c))?d:Y
},X);
return Y
};
Graph.Util.moebiusTransformation=function(Z,b,a,Y,X){this.eachNode(Z,function(d){for(var c=0;
c<a.length;
c++){var f=b[c].scale(-1),e=Y?Y:a[c];
d[a[c]].set(d[e].getc().moebiusTransformation(f))
}},X)
};
this.Hypertree=new O({Implements:[Loader,AngularWidth],initialize:function(a,X){var Z={labelContainer:a.id+"-label",withLabels:true,Node:{overridable:false,type:"circle",dim:7,color:"#ccb",width:5,height:5,lineWidth:1,transform:true},Edge:{overridable:false,type:"hyperline",color:"#ccb",lineWidth:1},clearCanvas:true,fps:40,duration:1500,transition:Trans.Quart.easeInOut};
var Y={onBeforeCompute:B,onAfterCompute:B,onCreateLabel:B,onPlaceLabel:B,onComplete:B,onBeforePlotLine:B,onAfterPlotLine:B,onBeforePlotNode:B,onAfterPlotNode:B};
this.controller=this.config=R(Z,Y,X);
this.graphOptions={complex:false,Node:{selected:false,exist:true,drawn:true}};
this.graph=new Graph(this.graphOptions);
this.fx=new Hypertree.Plot(this);
this.op=new Hypertree.Op(this);
this.json=null;
this.canvas=a;
this.root=null;
this.busy=false
},refresh:function(X){if(X){this.reposition();
Graph.Util.eachNode(this.graph,function(Y){Y.startPos.rho=Y.pos.rho=Y.endPos.rho;
Y.startPos.theta=Y.pos.theta=Y.endPos.theta
})
}else{this.compute()
}this.plot()
},reposition:function(){this.compute("endPos");
var X=this.graph.getNode(this.root).pos.getc().scale(-1);
Graph.Util.moebiusTransformation(this.graph,[X],["endPos"],"endPos","ignore");
Graph.Util.eachNode(this.graph,function(Y){if(Y.ignore){Y.endPos.rho=Y.pos.rho;
Y.endPos.theta=Y.pos.theta
}})
},plot:function(){this.fx.plot()
},compute:function(Y){var Z=Y||["pos","startPos"];
var X=this.graph.getNode(this.root);
X._depth=0;
Graph.Util.computeLevels(this.graph,this.root,0,"ignore");
this.computeAngularWidths();
this.computePositions(Z)
},computePositions:function(f){var g=J(f);
var b=this.graph,d=Graph.Util;
var e=this.graph.getNode(this.root),c=this,X=this.config;
var h=this.canvas.getSize();
var Z=Math.min(h.width,h.height)/2;
for(var a=0;
a<g.length;
a++){e[g[a]]=L(0,0)
}e.angleSpan={begin:0,end:2*Math.PI};
e._rel=1;
var Y=(function(){var l=0;
d.eachNode(b,function(i){l=(i._depth>l)?i._depth:l;
i._scale=Z
},"ignore");
for(var k=0.51;
k<=1;
k+=0.01){var j=(function(i,m){return(1-Math.pow(i,m))/(1-i)
})(k,l+1);
if(j>=2){return k-0.01
}}return 0.5
})();
d.eachBFS(this.graph,this.root,function(o){var k=o.angleSpan.end-o.angleSpan.begin;
var p=o.angleSpan.begin;
var n=(function(i){var r=0;
d.eachSubnode(i,function(s){r+=s._treeAngularWidth
},"ignore");
return r
})(o);
for(var m=1,j=0,l=Y,q=o._depth;
m<=q+1;
m++){j+=l;
l*=Y
}d.eachSubnode(o,function(u){if(!u._flag){u._rel=u._treeAngularWidth/n;
var t=u._rel*k;
var s=p+t/2;
for(var r=0;
r<g.length;
r++){u[g[r]]=L(s,j)
}u.angleSpan={begin:p,end:p+t};
p+=t
}},"ignore")
},"ignore")
},onClick:function(Z,X){var Y=this.graph.getNode(Z).pos.getc(true);
this.move(Y,X)
},move:function(c,Z){var Y=Q(c.x,c.y);
if(this.busy===false&&Y.norm()<1){var b=Graph.Util;
this.busy=true;
var X=b.getClosestNodeToPos(this.graph,Y),a=this;
b.computeLevels(this.graph,X.id,0);
this.controller.onBeforeCompute(X);
if(Y.norm()<1){Z=R({onComplete:B},Z||{});
this.fx.animate(R({modes:["moebius"],hideLabels:true},Z,{onComplete:function(){a.busy=false;
Z.onComplete()
}}),Y)
}}}});
Hypertree.Op=new O({Implements:Graph.Op,initialize:function(X){this.viz=X
}});
Hypertree.Plot=new O({Implements:Graph.Plot,initialize:function(X){this.viz=X;
this.config=X.config;
this.node=this.config.Node;
this.edge=this.config.Edge;
this.animation=new Animation;
this.nodeTypes=new Hypertree.Plot.NodeTypes;
this.edgeTypes=new Hypertree.Plot.EdgeTypes
},hyperline:function(i,a){var b=i.nodeFrom,Z=i.nodeTo,f=i.data;
var j=b.pos.getc(),e=Z.pos.getc();
var d=this.computeArcThroughTwoPoints(j,e);
var k=a.getSize();
var c=Math.min(k.width,k.height)/2;
if(d.a>1000||d.b>1000||d.ratio>1000){a.path("stroke",function(l){l.moveTo(j.x*c,j.y*c);
l.lineTo(e.x*c,e.y*c)
})
}else{var h=Math.atan2(e.y-d.y,e.x-d.x);
var g=Math.atan2(j.y-d.y,j.x-d.x);
var Y=this.sense(h,g);
var X=a.getCtx();
a.path("stroke",function(l){l.arc(d.x*c,d.y*c,d.ratio*c,h,g,Y)
})
}},computeArcThroughTwoPoints:function(l,k){var d=(l.x*k.y-l.y*k.x),X=d;
var c=l.squaredNorm(),Z=k.squaredNorm();
if(d==0){return{x:0,y:0,ratio:1001}
}var j=(l.y*Z-k.y*c+l.y-k.y)/d;
var h=(k.x*c-l.x*Z+k.x-l.x)/X;
var i=-j/2;
var g=-h/2;
var f=(j*j+h*h)/4-1;
if(f<0){return{x:0,y:0,ratio:1001}
}var e=Math.sqrt(f);
var Y={x:i,y:g,ratio:e,a:j,b:h};
return Y
},sense:function(X,Y){return(X<Y)?((X+Math.PI>Y)?false:true):((Y+Math.PI>X)?true:false)
},placeLabel:function(f,a,c){var e=a.pos.getc(true),Y=this.viz.canvas;
var d=Y.getSize();
var b=a._scale;
var Z={x:Math.round(e.x*b+d.width/2),y:Math.round(e.y*b+d.height/2)};
var X=f.style;
X.left=Z.x+"px";
X.top=Z.y+"px";
X.display="";
c.onPlaceLabel(f,a)
}});
Hypertree.Plot.NodeTypes=new O({none:function(){},circle:function(a,Y){var Z=this.node,c=a.data;
var b=Z.overridable&&c&&c.$dim||Z.dim;
var d=a.pos.getc(),e=d.scale(a._scale);
var X=Z.transform?b*(1-d.squaredNorm()):b;
if(X>=b/4){Y.path("fill",function(f){f.arc(e.x,e.y,X,0,Math.PI*2,true)
})
}},square:function(a,Z){var f=this.node,c=a.data;
var X=f.overridable&&c&&c.$dim||f.dim;
var Y=a.pos.getc(),e=Y.scale(a._scale);
var d=f.transform?X*(1-Y.squaredNorm()):X;
var b=2*d;
if(d>=X/4){Z.getCtx().fillRect(e.x-d,e.y-d,b,b)
}},rectangle:function(a,Z){var e=this.node,b=a.data;
var Y=e.overridable&&b&&b.$width||e.width;
var f=e.overridable&&b&&b.$height||e.height;
var X=a.pos.getc(),d=X.scale(a._scale);
var c=1-X.squaredNorm();
Y=e.transform?Y*c:Y;
f=e.transform?f*c:f;
if(c>=0.25){Z.getCtx().fillRect(d.x-Y/2,d.y-f/2,Y,f)
}},triangle:function(c,Z){var i=this.node,d=c.data;
var X=i.overridable&&d&&d.$dim||i.dim;
var Y=c.pos.getc(),h=Y.scale(c._scale);
var g=i.transform?X*(1-Y.squaredNorm()):X;
if(g>=X/4){var b=h.x,a=h.y-g,k=b-g,j=h.y+g,f=b+g,e=j;
Z.path("fill",function(l){l.moveTo(b,a);
l.lineTo(k,j);
l.lineTo(f,e)
})
}},star:function(a,Z){var g=this.node,c=a.data;
var X=g.overridable&&c&&c.$dim||g.dim;
var Y=a.pos.getc(),f=Y.scale(a._scale);
var e=g.transform?X*(1-Y.squaredNorm()):X;
if(e>=X/4){var h=Z.getCtx(),d=Math.PI/5;
h.save();
h.translate(f.x,f.y);
h.beginPath();
h.moveTo(X,0);
for(var b=0;
b<9;
b++){h.rotate(d);
if(b%2==0){h.lineTo((e/0.525731)*0.200811,0)
}else{h.lineTo(e,0)
}}h.closePath();
h.fill();
h.restore()
}}});
Hypertree.Plot.EdgeTypes=new O({none:function(){},line:function(X,Y){var Z=X.nodeFrom._scale;
var b=X.nodeFrom.pos.getc(true);
var a=X.nodeTo.pos.getc(true);
Y.path("stroke",function(c){c.moveTo(b.x*Z,b.y*Z);
c.lineTo(a.x*Z,a.y*Z)
})
},hyperline:function(X,Y){this.hyperline(X,Y)
}});
this.TM={layout:{orientation:"h",vertical:function(){return this.orientation=="v"
},horizontal:function(){return this.orientation=="h"
},change:function(){this.orientation=this.vertical()?"h":"v"
}},innerController:{onBeforeCompute:B,onAfterCompute:B,onComplete:B,onCreateElement:B,onDestroyElement:B,request:false},config:{orientation:"h",titleHeight:13,rootId:"infovis",offset:4,levelsToShow:3,addLeftClickHandler:false,addRightClickHandler:false,selectPathOnHover:false,Color:{allow:false,minValue:-100,maxValue:100,minColorValue:[255,0,50],maxColorValue:[0,255,50]},Tips:{allow:false,offsetX:20,offsetY:20,onShow:B}},initialize:function(X){this.tree=null;
this.shownTree=null;
this.controller=this.config=R(this.config,this.innerController,X);
this.rootId=this.config.rootId;
this.layout.orientation=this.config.orientation;
if(this.config.Tips.allow&&document.body){var b=document.getElementById("_tooltip")||document.createElement("div");
b.id="_tooltip";
b.className="tooltip";
var Z=b.style;
Z.position="absolute";
Z.display="none";
Z.zIndex=13000;
document.body.appendChild(b);
this.tip=b
}var a=this;
var Y=function(){a.empty();
if(window.CollectGarbage){window.CollectGarbage()
}delete Y
};
if(window.addEventListener){window.addEventListener("unload",Y,false)
}else{window.attachEvent("onunload",Y)
}},each:function(X){(function Y(d){if(!d){return 
}var c=d.childNodes,Z=c.length;
if(Z>0){X.apply(this,[d,Z===1,c[0],c[1]])
}if(Z>1){for(var a=c[1].childNodes,b=0;
b<a.length;
b++){Y(a[b])
}}})(E(this.rootId).firstChild)
},toStyle:function(Z){var X="";
for(var Y in Z){X+=Y+":"+Z[Y]+";"
}return X
},leaf:function(X){return X.children==0
},createBox:function(Y,a,X){var Z;
if(!this.leaf(Y)){Z=this.headBox(Y,a)+this.bodyBox(X,a)
}else{Z=this.leafBox(Y,a)
}return this.contentBox(Y,a,Z)
},plot:function(b){var d=b.coord,a="";
if(this.leaf(b)){return this.createBox(b,d,null)
}for(var Z=0,c=b.children;
Z<c.length;
Z++){var Y=c[Z],X=Y.coord;
if(X.width*X.height>1){a+=this.plot(Y)
}}return this.createBox(b,d,a)
},headBox:function(Y,b){var X=this.config,a=X.offset;
var Z={height:X.titleHeight+"px",width:(b.width-a)+"px",left:a/2+"px"};
return'<div class="head" style="'+this.toStyle(Z)+'">'+Y.name+"</div>"
},bodyBox:function(Y,d){var X=this.config,Z=X.titleHeight,b=X.offset;
var a={width:(d.width-b)+"px",height:(d.height-b-Z)+"px",top:(Z+b/2)+"px",left:(b/2)+"px"};
return'<div class="body" style="'+this.toStyle(a)+'">'+Y+"</div>"
},contentBox:function(Z,b,Y){var a={};
for(var X in b){a[X]=b[X]+"px"
}return'<div class="content" style="'+this.toStyle(a)+'" id="'+Z.id+'">'+Y+"</div>"
},leafBox:function(a,f){var Z=this.config;
var Y=Z.Color.allow&&this.setColor(a),e=Z.offset,b=f.width-e,X=f.height-e;
var d={top:(e/2)+"px",height:X+"px",width:b+"px",left:(e/2)+"px"};
if(Y){d["background-color"]=Y
}return'<div class="leaf" style="'+this.toStyle(d)+'">'+a.name+"</div>"
},setColor:function(f){var Z=this.config.Color,a=Z.maxColorValue,X=Z.minColorValue,b=Z.maxValue,g=Z.minValue,e=b-g,d=(f.data.$color-0);
var Y=function(h,c){return Math.round((((a[h]-X[h])/e)*(c-g)+X[h]))
};
return D([Y(0,d),Y(1,d),Y(2,d)])
},enter:function(X){this.view(X.parentNode.id)
},onLeftClick:function(X){this.enter(X)
},out:function(){var X=TreeUtil.getParent(this.tree,this.shownTree.id);
if(X){if(this.controller.request){TreeUtil.prune(X,this.config.levelsToShow)
}this.view(X.id)
}},onRightClick:function(){this.out()
},view:function(b){var X=this.config,Z=this;
var Y={onComplete:function(){Z.loadTree(b);
E(X.rootId).focus()
}};
if(this.controller.request){var a=TreeUtil;
a.loadSubtrees(a.getSubtree(this.tree,b),R(this.controller,Y))
}else{Y.onComplete()
}},resetPath:function(X){var Y=this.rootId,b=this.resetPath.previous;
this.resetPath.previous=X||false;
function Z(e){var d=e.parentNode;
return d&&(d.id!=Y)&&d
}function a(f,c){if(f){var d=E(f.id);
if(d){var e=Z(d);
while(e){f=e.childNodes[0];
if(S(f,"in-path")){if(c==undefined||!!c){A(f,"in-path")
}}else{if(!c){P(f,"in-path")
}}e=Z(e)
}}}}a(b,true);
a(X,false)
},initializeElements:function(){var X=this.controller,Z=this;
var Y=M(false),a=X.Tips.allow;
this.each(function(f,e,d,c){var b=TreeUtil.getSubtree(Z.tree,f.id);
X.onCreateElement(f,b,e,d,c);
if(X.addRightClickHandler){d.oncontextmenu=Y
}if(X.addLeftClickHandler||X.addRightClickHandler){T(d,"mouseup",function(g){var i=g.target||g.srcElement;
if(i.nodeName.toLowerCase() == 'a'){return 
}var h=(g.which==3||g.button==2);
if(h){if(X.addRightClickHandler){Z.onRightClick()
}}else{if(X.addLeftClickHandler){Z.onLeftClick(d)
}}if(g.preventDefault){g.preventDefault()
}else{g.returnValue=false
}})
}if(X.selectPathOnHover||a){T(d,"mouseover",function(g){if(X.selectPathOnHover){if(e){P(d,"over-leaf")
}else{P(d,"over-head");
P(f,"over-content")
}if(f.id){Z.resetPath(b)
}}if(a){X.Tips.onShow(Z.tip,b,e,d)
}});
T(d,"mouseout",function(g){if(X.selectPathOnHover){if(e){A(d,"over-leaf")
}else{A(d,"over-head");
A(f,"over-content")
}Z.resetPath()
}if(a){Z.tip.style.display="none"
}});
if(a){T(d,"mousemove",function(j,i){var o=Z.tip;
i=i||window;
j=j||i.event;
var n=i.document;
n=n.html||n.body;
var k={x:j.pageX||j.clientX+n.scrollLeft,y:j.pageY||j.clientY+n.scrollTop};
o.style.display="";
i={height:document.body.clientHeight,width:document.body.clientWidth};
var h={width:o.offsetWidth,height:o.offsetHeight};
var g=o.style,m=X.Tips.offsetX,l=X.Tips.offsetY;
g.top=((k.y+l+h.height>i.height)?(k.y-h.height-l):k.y+l)+"px";
g.left=((k.x+h.width+m>i.width)?(k.x-h.width-m):k.x+m)+"px"
})
}}})
},destroyElements:function(){if(this.controller.onDestroyElement!=B){var X=this.controller,Y=this;
this.each(function(c,b,a,Z){X.onDestroyElement(c,TreeUtil.getSubtree(Y.tree,c.id),b,a,Z)
})
}},empty:function(){this.destroyElements();
F(E(this.rootId))
},loadTree:function(X){this.empty();
this.loadJSON(TreeUtil.getSubtree(this.tree,X))
}};
TM.SliceAndDice=new O({Implements:TM,loadJSON:function(a){this.controller.onBeforeCompute(a);
var Y=E(this.rootId),Z=this.config,b=Y.offsetWidth,X=Y.offsetHeight;
var c={coord:{top:0,left:0,width:b,height:X+Z.titleHeight+Z.offset}};
if(this.tree==null){this.tree=a
}this.shownTree=a;
this.compute(c,a,this.layout.orientation);
Y.innerHTML=this.plot(a);
this.initializeElements();
this.controller.onAfterCompute(a)
},compute:function(d,m,b){var o=this.config,i=d.coord,l=o.offset,h=i.width-l,f=i.height-l-o.titleHeight,Y=d.data,X=(Y&&("$area" in Y))?m.data.$area/Y.$area:1;
var g,e,k,c,a;
var n=(b=="h");
if(n){b="v";
g=f;
e=Math.round(h*X);
k="height";
c="top";
a="left"
}else{b="h";
g=Math.round(f*X);
e=h;
k="width";
c="left";
a="top"
}m.coord={width:e,height:g,top:0,left:0};
var j=0,Z=this;
G(m.children,function(p){Z.compute(m,p,b);
p.coord[c]=j;
p.coord[a]=0;
j+=Math.floor(p.coord[k])
})
}});
TM.Area=new O({loadJSON:function(Z){this.controller.onBeforeCompute(Z);
var Y=E(this.rootId),a=Y.offsetWidth,X=Y.offsetHeight,e=this.config.offset,c=a-e,b=X-e-this.config.titleHeight;
Z.coord={height:X,width:a,top:0,left:0};
var d=R(Z.coord,{width:c,height:b});
this.compute(Z,d);
Y.innerHTML=this.plot(Z);
if(this.tree==null){this.tree=Z
}this.shownTree=Z;
this.initializeElements();
this.controller.onAfterCompute(Z)
},computeDim:function(a,f,Y,e,Z){if(a.length+f.length==1){var X=(a.length==1)?a:f;
this.layoutLast(X,Y,e);
return 
}if(a.length>=2&&f.length==0){f=[a[0]];
a=a.slice(1)
}if(a.length==0){if(f.length>0){this.layoutRow(f,Y,e)
}return 
}var d=a[0];
if(Z(f,Y)>=Z([d].concat(f),Y)){this.computeDim(a.slice(1),f.concat([d]),Y,e,Z)
}else{var b=this.layoutRow(f,Y,e);
this.computeDim(a,[],b.dim,b,Z)
}},worstAspectRatio:function(X,e){if(!X||X.length==0){return Number.MAX_VALUE
}var Y=0,f=0,b=Number.MAX_VALUE;
for(var c=0;
c<X.length;
c++){var Z=X[c]._area;
Y+=Z;
b=(b<Z)?b:Z;
f=(f>Z)?f:Z
}var d=e*e,a=Y*Y;
return Math.max(d*f/a,a/(d*b))
},avgAspectRatio:function(a,X){if(!a||a.length==0){return Number.MAX_VALUE
}var c=0;
for(var Y=0;
Y<a.length;
Y++){var b=a[Y]._area;
var Z=b/X;
c+=(X>Z)?X/Z:Z/X
}return c/a.length
},layoutLast:function(Y,X,Z){Y[0].coord=Z
}});
TM.Squarified=new O({Implements:[TM,TM.Area],compute:function(f,c){if(!(c.width>=c.height&&this.layout.horizontal())){this.layout.change()
}var X=f.children,Z=this.config;
if(X.length>0){this.processChildrenLayout(f,X,c);
for(var b=0;
b<X.length;
b++){var a=X[b].coord,d=Z.offset,e=a.height-(Z.titleHeight+d),Y=a.width-d;
c={width:Y,height:e,top:0,left:0};
this.compute(X[b],c)
}}},processChildrenLayout:function(f,X,b){var Y=b.width*b.height;
var a,c=0,g=[];
for(a=0;
a<X.length;
a++){g[a]=parseFloat(X[a].data.$area);
c+=g[a]
}for(a=0;
a<g.length;
a++){X[a]._area=Y*g[a]/c
}var Z=(this.layout.horizontal())?b.height:b.width;
X.sort(function(i,h){return(i._area<=h._area)-(i._area>=h._area)
});
var e=[X[0]];
var d=X.slice(1);
this.squarify(d,e,Z,b)
},squarify:function(Y,a,X,Z){this.computeDim(Y,a,X,Z,this.worstAspectRatio)
},layoutRow:function(Y,X,Z){if(this.layout.horizontal()){return this.layoutV(Y,X,Z)
}else{return this.layoutH(Y,X,Z)
}},layoutV:function(X,f,c){var g=0,Z=Math.round;
G(X,function(h){g+=h._area
});
var Y=Z(g/f),d=0;
for(var a=0;
a<X.length;
a++){var b=Z(X[a]._area/Y);
X[a].coord={height:b,width:Y,top:c.top+d,left:c.left};
d+=b
}var e={height:c.height,width:c.width-Y,top:c.top,left:c.left+Y};
e.dim=Math.min(e.width,e.height);
if(e.dim!=e.height){this.layout.change()
}return e
},layoutH:function(X,e,b){var g=0,Y=Math.round;
G(X,function(h){g+=h._area
});
var f=Y(g/e),c=b.top,Z=0;
for(var a=0;
a<X.length;
a++){X[a].coord={height:f,width:Y(X[a]._area/f),top:c,left:b.left+Z};
Z+=X[a].coord.width
}var d={height:b.height-f,width:b.width,top:b.top+f,left:b.left};
d.dim=Math.min(d.width,d.height);
if(d.dim!=d.width){this.layout.change()
}return d
}});
TM.Strip=new O({Implements:[TM,TM.Area],compute:function(f,c){var X=f.children,Z=this.config;
if(X.length>0){this.processChildrenLayout(f,X,c);
for(var b=0;
b<X.length;
b++){var a=X[b].coord,d=Z.offset,e=a.height-(Z.titleHeight+d),Y=a.width-d;
c={width:Y,height:e,top:0,left:0};
this.compute(X[b],c)
}}},processChildrenLayout:function(a,Z,e){var b=e.width*e.height;
var c=parseFloat(a.data.$area);
G(Z,function(f){f._area=b*parseFloat(f.data.$area)/c
});
var Y=(this.layout.horizontal())?e.width:e.height;
var d=[Z[0]];
var X=Z.slice(1);
this.stripify(X,d,Y,e)
},stripify:function(Y,a,X,Z){this.computeDim(Y,a,X,Z,this.avgAspectRatio)
},layoutRow:function(Y,X,Z){if(this.layout.horizontal()){return this.layoutH(Y,X,Z)
}else{return this.layoutV(Y,X,Z)
}},layoutV:function(X,f,c){var g=0,Z=function(h){return h
};
G(X,function(h){g+=h._area
});
var Y=Z(g/f),d=0;
for(var a=0;
a<X.length;
a++){var b=Z(X[a]._area/Y);
X[a].coord={height:b,width:Y,top:c.top+(f-b-d),left:c.left};
d+=b
}var e={height:c.height,width:c.width-Y,top:c.top,left:c.left+Y,dim:f};
return e
},layoutH:function(X,e,b){var g=0,Y=function(h){return h
};
G(X,function(h){g+=h._area
});
var f=Y(g/e),c=b.height-f,Z=0;
for(var a=0;
a<X.length;
a++){X[a].coord={height:f,width:Y(X[a]._area/f),top:c,left:b.left+Z};
Z+=X[a].coord.width
}var d={height:b.height-f,width:b.width,top:b.top,left:b.left,dim:e};
return d
}})
})();