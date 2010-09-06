#
# color_base.rb - RubyTreemap
#
# Copyright (c) 2006 by Andrew Bruno <aeb@qnot.org>
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
#

module Treemap
    class ColorBase
        def get_hex_color(value)
            # base classes override
        end

        def get_rgb_color(value)
            # base classes override
        end
        
        def red(color)
            color.sub!(/^#/, "")
            color[0,2].hex 
        end 

        def green(color)
            color.sub!(/^#/, "")
            color[2,2].hex 
        end

        def blue(color)
            color.sub!(/^#/, "")
            color[4,2].hex 
        end

        def to_rgb(color)
            [red(color), green(color), blue(color)]
        end

        def to_html(width=1, height=20)
            html = "<div>"
            index = @min

            while(index <= @max)
                html += "<span style=\"display: block; float: left;"
                html += "width: " + width.to_s + "px;"
                html += "height: " + height.to_s + "px;"
                html += "background-color: #" + get_hex_color(index) + ";"
                html += "\">"
                html += "<img width=\"1\" height=\"1\" />"
                html += "</span>"
                index += @increment
            end

            html += "</div>"
            html
        end
    end
end
