/*
 * Copyright (C) 2011 Paul Stoellberger
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the Free 
 * Software Foundation; either version 2 of the License, or (at your option) 
 * any later version.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 *
 */
package org.saiku.olap.dto;

//import org.olap4j.metadata.NamedList;
//import org.olap4j.metadata.Property;

public class SaikuMember extends AbstractSaikuObject {
	
	private String caption;
	private String dimensionUniqueName;
	private String levelUniqueName;
	//KB: Add MEMBER_KEY property:
	//private NamedList<Property> properties;
	private String memberKey;
	//KB Add ChildMemberCount:
	private int childMemberCount;
	
	


	public SaikuMember() {
		super(null,null);
		throw new RuntimeException("Unsupported Constructor. Serialization only");
	}

	public SaikuMember(String name, String uniqueName, String caption, String dimensionUniqueName, String levelUniqueName, String memberKey, int childMemberCount) { //NamedList<Property> properties) {
		super(uniqueName,name);
		this.caption = caption;
		this.dimensionUniqueName = dimensionUniqueName;
		//this.properties = properties;
		this.memberKey = memberKey;
		this.childMemberCount = childMemberCount;
	}

	public String getCaption() {
		return caption;
	}
	
	public String getLevelUniqueName() {
		return levelUniqueName;
	}
	
	public String getDimensionUniqueName() {
		return dimensionUniqueName;
	}
	/*
	public NamedList<Property> getProperties() {
		return properties;
	}
	*/
	public String getMemberKey() {
		return memberKey;
	}

	public void setMemberKey(String memberKey) {
		this.memberKey = memberKey;
	}
	

	public int getChildMemberCount() {
		return childMemberCount;
	}
}
