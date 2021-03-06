/*
 * Copyright (C) 2011 OSBI Ltd
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
package org.saiku.web.rest.resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.provider.FileReplicator;
import org.apache.commons.vfs.provider.TemporaryFileStore;
import org.apache.commons.vfs.provider.VfsComponentContext;
import org.apache.commons.vfs.provider.res.ResourceFileProvider;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.saiku.olap.dto.SaikuTag;
import org.saiku.service.olap.OlapQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * QueryServlet contains all the methods required when manipulating an OLAP Query.
 * @author Paul Stoellberger
 *
 */
@Component
@Path("/saiku/{username}/tags")
@XmlAccessorType(XmlAccessType.NONE)
public class BasicTagRepositoryResource {

	private static final Logger log = LoggerFactory.getLogger(BasicTagRepositoryResource.class);

	private OlapQueryService olapQueryService;

	private FileObject repo;

	public void setPath(String path) throws Exception {

		FileSystemManager fileSystemManager;
		try {
			 if (!path.endsWith("" + File.separatorChar)) {
				path += File.separatorChar;
			}
			fileSystemManager = VFS.getManager();
			FileObject fileObject;
			fileObject = fileSystemManager.resolveFile(path);
			if (fileObject == null) {
				throw new IOException("File cannot be resolved: " + path);
			}
			if(!fileObject.exists()) {
				throw new IOException("File does not exist: " + path);
			}
			repo = fileObject;
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Autowired
	public void setOlapQueryService(OlapQueryService olapqs) {
		olapQueryService = olapqs;
	}

	@GET
	@Path("/{cubeIdentifier}")
	@Produces({"application/json" })
	public List<SaikuTag> getSavedTags(
			@PathParam("cubeIdentifier") String cubeIdentifier) 
	{
		List<SaikuTag> allTags = new ArrayList<SaikuTag>();
		try {
			if (repo != null) {
				File[] files = new File(repo.getName().getPath()).listFiles();
				for (File file : files) {
					if (!file.isHidden()) {
						
						String filename = file.getName();
						if (filename.endsWith(".tag")) {
							filename = filename.substring(0,filename.length() - ".tag".length());
							if (filename.equals(cubeIdentifier)) {
								FileReader fi = new FileReader(file);
								BufferedReader br = new BufferedReader(fi);
								ObjectMapper om = new ObjectMapper();
							    om.setVisibilityChecker(om.getVisibilityChecker().withFieldVisibility(Visibility.ANY));
							    
								List<SaikuTag> tags = om.readValue(file, TypeFactory.collectionType(ArrayList.class, SaikuTag.class));
								allTags.addAll(tags);
							}
						}
					}
				}

			}
			else {
				throw new Exception("repo URL is null");
			}
		} catch (Exception e) {
			log.error(this.getClass().getName(),e);
			e.printStackTrace();
		}
		Collections.sort(allTags);
		return allTags;
	}

	/**
	 * Delete Query.
	 * @param queryName - The name of the query.
	 * @return A GONE Status if the query was deleted, otherwise it will return a NOT FOUND Status code.
	 */
	@DELETE
	@Produces({"application/json" })
	@Path("/{cubeIdentifier}/{tagName}")
	public Status deleteTag(
			@PathParam("cubeIdentifier") String cubeIdentifier,
			@PathParam("tagName") String tagName)
	{
		try{
			if (repo != null) {
				List<SaikuTag> tags = getSavedTags(cubeIdentifier);
				List<SaikuTag> remove = new ArrayList<SaikuTag>();
				for(SaikuTag tag : tags) {
					if(tag.getName().equals(tagName)) {
						remove.add(tag);
					}
				}
				tags.removeAll(remove);
				ObjectMapper om = new ObjectMapper();
			    om.setVisibilityChecker(om.getVisibilityChecker().withFieldVisibility(Visibility.ANY));

				String uri = repo.getName().getPath();
				if (!uri.endsWith("" + File.separatorChar)) {
					uri += File.separatorChar;
				}
				if (!cubeIdentifier.endsWith(".tag")) {
					cubeIdentifier += ".tag";
				}

				File tagFile = new File(uri+URLDecoder.decode(cubeIdentifier, "UTF-8"));
				if (tagFile.exists()) {
					tagFile.delete();
				}
				else {
					tagFile.createNewFile();
				}
				om.writeValue(tagFile, tags);
				return(Status.GONE);
				
			}
			throw new Exception("Cannot delete tag :" + tagName );
		}
		catch(Exception e){
			log.error("Cannot delete tag (" + tagName + ")",e);
			return(Status.NOT_FOUND);
		}
	}

	@POST
	@Produces({"application/json" })
	@Path("/{cubeIdentifier}/{tagname}")
	public SaikuTag saveTag(
			@PathParam("tagname") String tagName,
			@PathParam("cubeIdentifier") String cubeIdentifier,
			@FormParam("queryname") String queryName,
			@FormParam("positions") String positions)
	{
		try {
			List<List<Integer>> cellPositions = new ArrayList<List<Integer>>();
			for (String position : positions.split(",")) {
				String[] ps = position.split(":");
				List<Integer> cellPosition = new ArrayList<Integer>();

				for (String p : ps) {
					Integer pInt = Integer.parseInt(p);
					cellPosition.add(pInt);
				}
				cellPositions.add(cellPosition);
			}
			SaikuTag t = olapQueryService.createTag(queryName, tagName, cellPositions);
			
			if (repo != null) {
				List<SaikuTag> tags = getSavedTags(cubeIdentifier);
				if (!cubeIdentifier.endsWith(".tag")) {
					cubeIdentifier += ".tag";
				}
				List<SaikuTag> remove = new ArrayList<SaikuTag>();
				for(SaikuTag tag : tags) {
					if(tag.getName().equals(tagName)) {
						remove.add(tag);
					}
				}
				tags.removeAll(remove);
				
				tags.add(t);
				ObjectMapper om = new ObjectMapper();
			    om.setVisibilityChecker(om.getVisibilityChecker().withFieldVisibility(Visibility.ANY));

				String uri = repo.getName().getPath();
				if (!uri.endsWith("" + File.separatorChar)) {
					uri += File.separatorChar;
				}
				File tagFile = new File(uri+URLDecoder.decode(cubeIdentifier, "UTF-8"));
				if (tagFile.exists()) {
					tagFile.delete();
				}
				else {
					tagFile.createNewFile();
				}
				om.writeValue(tagFile, tags);
				return t;
			}
		}
		catch (Exception e) {
			log.error("Cannot add tag " + tagName + " for query (" + queryName + ")",e);
		}
		return null;

	}
	
	@GET
	@Produces({"application/json" })
	@Path("/{cubeIdentifier}/{tagName}")
	public SaikuTag getTag(
			@PathParam("cubeIdentifier") String cubeIdentifier,
			@PathParam("tagName") String tagName)
	{
		try{
			if (repo != null) {
				List<SaikuTag> tags = getSavedTags(cubeIdentifier);
				for(SaikuTag tag : tags) {
					if(tag.getName().equals(tagName)) {
						return tag;
					}
				}
			}
		}
		catch (Exception e) {
			log.error("Cannot get tag " + tagName + " for " + cubeIdentifier ,e);
		}
		return null;
	}

	
}
