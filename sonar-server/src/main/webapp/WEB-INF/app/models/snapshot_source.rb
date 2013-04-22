#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2013 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#
include ActionView::Helpers::JavaScriptHelper

class SnapshotSource < ActiveRecord::Base
  belongs_to :snapshot

  def to_hash_json(options={})
    from = (options[:from] ? options[:from].to_i - 1 : 0)
    to = (options[:to] ? options[:to].to_i - 2 : -1)

    lines=lines(false)

    json = {}
    lines[from..to].each_with_index do |line, id|
      json[id+from+1]=line
    end
    json
  end
  
  def to_xml(xml, options={})
    from = (options[:from] ? options[:from].to_i - 1 : 0)
    to = (options[:to] ? options[:to].to_i - 2 : -1)
    xml.source do
      lines(false)[from..to].each_with_index do |line, id|
        xml.line do
          xml.id(id+from+1)
          xml.val(line)
        end
      end
    end
  end

  def to_txt(options={})
    from = (options[:from] ? options[:from].to_i - 1 : 0)
    to = (options[:to] ? options[:to].to_i - 2 : -1)
    txt=''
    lines(false)[from..to].each_with_index do |line, id|
      txt += line + "\n"
    end
    txt
  end

  def encoded_data(escape_html=false)
    escape_html ? CGI::escapeHTML(data) : data
  end

  def lines(encode)
    Api::Utils.split_newlines(encoded_data(encode))
  end
  
  def syntax_highlighted_source
    @syntax_highlighted_source||=
      begin
        data ? Java::OrgSonarServerUi::JRubyFacade.getInstance().colorizeCode(data, snapshot.project.language) : ''
      end
  end
  
  def syntax_highlighted_lines
    Api::Utils.split_newlines(syntax_highlighted_source)
  end

end
