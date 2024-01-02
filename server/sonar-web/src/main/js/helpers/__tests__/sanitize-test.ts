/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import { sanitizeString, sanitizeStringRestricted } from '../sanitize';

describe('sanitizeStringRestricted', () => {
  it('should preserve only specific formatting tags', () => {
    expect(
      sanitizeStringRestricted(`
    Hi <a href="http://example.com" target="_blank">this</a> is <i>in italics</i> and <ul>
    <li> lists </li>
    <li> are allowed</li>
    </ul>
    <p>
    as well. This is <b>Amazing</b> and this <strong>bold</strong> <br>
    and <code>code.is.accepted too</code>
    </p>
  `)
    ).toBe(`
    Hi <a target="_blank" href="http://example.com">this</a> is <i>in italics</i> and <ul>
    <li> lists </li>
    <li> are allowed</li>
    </ul>
    <p>
    as well. This is <b>Amazing</b> and this <strong>bold</strong> <br>
    and <code>code.is.accepted too</code>
    </p>
  `);
  });

  /*
   * Test code borrowed from OWASP's sanitizer tests
   * https://github.com/OWASP/java-html-sanitizer/blob/master/src/test/resources/org/owasp/html/htmllexerinput1.html
   */
  it('should strip everything else', () => {
    const clean = sanitizeStringRestricted(`<?xml version="not-even-close"?>

    <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
    
    <!-- a test input for HtmlLexer -->
    
    <html>
    <head>
    <title>Test File For HtmlLexer &amp; HtmlParser</title>
    <link rel=stylesheet type="text/css" src=foo/bar.css />
    <body
     bgcolor=white
     linkcolor = "blue"
     onload="document.writeln(
      &quot;&lt;p&gt;properly escaped code in a handler&lt;/p&gt;&quot;);"
    >
    
    <script type="text/javascript"><!--
    document.writeln("<p>Some initialization code in global context</p>");
    --></script>
    
    <script type="text/javascript">
    // hi there
    document.writeln("<p>More initialization</p>");
    </script>
    
    <div id=clickydiv onclick="handleClicky(event)"
     ondblclick=this.onclick(event);return(false)>
    Clicky
    </div>
    
    <input id=foo>
    <gxp:attr name="onchange">alert("&lt;b&gt;hi&lt;/b&gt;");</gxp:attr>
    </input>
    
    <pre>&lt;div id=notarealtag onclick=notcode()&gt;</pre>
    
    <!-- some tokenization corner cases -->
    
    < notatag <atag/>
    
    </ notatag> </redundantlyclosed/>
    
    <messyattributes a=b=c d="e"f=g h =i j= k l = m checked n="o"/>
    
    < < < all in one text block > > >
    
    <xmp>Make sure that <!-- comments don't obscure the xmp close</xmp>
    <% # some php code here
    write("<pre>$horriblySyntacticConstruct1</pre>\n\n");
    %>
    <script type="text/javascript"><!--
    alert("hello world");
    // --></script>
    
    <script>/* </script> */alert('hi');</script>
    <script><!--/* </script> */alert('hi');--></script>
    
    <xmp style=color:blue><!--/* </xmp> */alert('hi');--></xmp>
    
    <style><!-- p { contentf: '</style>' } --></style>
    <style>Foo<!-- > </style> --></style>
    <textarea><!-- Zoicks </textarea>--></textarea>
    <!-- An escaping text span start may share its U+002D HYPHEN-MINUS characters
       - with its corresponding escaping text span end. -->
    <script><!--></script>
    <script><!---></script>
    <script><!----></script>
    </body>
    </html>
    <![CDATA[ No such thing as a CDATA> section in HTML ]]>
    <script>a<b</script>
    <img src=foo.gif /><a href=><a href=/>
    <span title=malformed attribs' do=don't id=foo checked onclick="a<b">Bar</span>`);

    expect(clean.replace(/\s+/g, '')).toBe(
      `Clickyalert("&lt;b&gt;hi&lt;/b&gt;");&lt;divid=notarealtagonclick=notcode()&gt;&lt;notatag&lt;&lt;&lt;allinonetextblock&gt;&gt;&gt;&lt;%#somephpcodeherewrite("$horriblySyntacticConstruct1");%&gt;*/alert('hi');*/alert('hi');--&gt;*/alert('hi');--&gt;'}--&gt;--&gt;&lt;!--Zoicks--&gt;sectioninHTML]]&gt;<ahref=""></a><ahref="/">Bar</a>`
    );
  });
});

describe('sanitizeString', () => {
  it('should not allow MathML and SVG', () => {
    const tainted = `
    Hi <a href="javascript:alert('hello')" target="_blank">this</a> is <i>in italics</i> and <ul>
    <li> lists </li>
    <li> are allowed</li>
    </ul>
    <p class="some-class">
    as well. This is <b>Amazing</b> and this <strong>bold</strong> <br>
    and <code>code.is.accepted too</code>
    </p>
    <svg><text>SVG isn't allowed</text></svg>
    <math xmlns="http://www.w3.org/1998/Math/MathML">
      <infinity />
    </math>`;
    const clean = `
    Hi <a>this</a> is <i>in italics</i> and <ul>
    <li> lists </li>
    <li> are allowed</li>
    </ul>
    <p class="some-class">
    as well. This is <b>Amazing</b> and this <strong>bold</strong> <br>
    and <code>code.is.accepted too</code>
    </p>`;

    expect(sanitizeString(tainted).trimRight()).toBe(clean);
  });
});
