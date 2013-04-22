#
# SonarQube, open source software quality management tool.
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

#
# Sonar 3.4
#
class AddUniqueIndexToAuthors < ActiveRecord::Migration

  class Author < ActiveRecord::Base
  end

  def self.up
    delete_duplicated_authors
    begin
      add_index :authors, :login, :unique => true, :name => 'uniq_author_logins'
    rescue
      # Ignore, already exists
    end
  end

  private
  def self.delete_duplicated_authors
    say_with_time 'Delete duplicated authors' do
      authors_by_login={}
      authors=Author.find(:all, :select => 'id,login', :order => 'id')
      authors.each do |author|
        if authors_by_login[author.login]
          # already exists
          author.destroy
        else
          authors_by_login[author.login]=author
        end
      end
    end
  end
end
