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

#
# Sonar 2.8
#
class CreateReview < ActiveRecord::Migration

  def self.up
    create_table 'reviews' do |t|
      t.column 'created_at', 					:datetime
      t.column 'updated_at', 					:datetime,  :null => true
      t.column 'user_id', 						:integer, 	:null => true
      t.column 'assignee_id', 					:integer, 	:null => true
      t.column 'title',		 					:string, 	:null => true,	:limit => 500
      t.column 'review_type', 					:string, 	:null => true,	:limit => 15
      t.column 'status', 						:string, 	:null => true,	:limit => 10
      t.column 'severity', 						:string, 	:null => true,	:limit => 10
      t.column 'rule_failure_permanent_id', 	:integer, 	:null => true   
	    t.column 'project_id', 					:integer, 	:null => true
      t.column 'resource_id', 					:integer, 	:null => true   
      t.column 'resource_line', 				:integer, 	:null => true      
    end
    
    create_table 'review_comments' do |t|
      t.column 'created_at', 		:datetime
      t.column 'updated_at', 		:datetime
      t.column 'review_id', 		:integer
      t.column 'user_id', 			:integer, 	:null => true
      t.column 'review_text', 		:text, 		:null => true
    end
    
    alter_to_big_primary_key('reviews')
    alter_to_big_primary_key('review_comments')
    
  end

end
