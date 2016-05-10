/*
 * Copyright (C) 2005-2016 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.rest.api.Nodes;
import org.alfresco.rest.api.tests.AbstractBaseApiTest;
import org.alfresco.rest.api.tests.RepoService;
import org.alfresco.rest.api.tests.client.HttpResponse;
import org.alfresco.rest.api.tests.client.PublicApiClient;
import org.alfresco.rest.api.tests.client.RequestContext;
import org.alfresco.rest.api.tests.client.data.Document;
import org.alfresco.rest.api.tests.client.data.Folder;
import org.alfresco.rest.api.tests.client.data.Node;
import org.alfresco.rest.api.tests.util.JacksonUtil;
import org.alfresco.rest.api.tests.util.RestApiUtil;
import org.alfresco.rest.framework.jacksonextensions.JacksonHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

/**
 * Tests Deleting nodes and recovering
 *
 * @author gethin
 */
public class DeletedNodesTest extends AbstractSingleNetworkSiteTest
{

    protected static final String URL_DELETED_NODES = "deleted-nodes";

    @Test
    public void testCreateAndDelete() throws Exception
    {
        publicApiClient.setRequestContext(new RequestContext(u1.getId()));
        Date now = new Date();
        String folder1 = "folder" + now.getTime() + "_1";
        Folder createdFolder = createFolder(u1.getId(), docLibNodeRef.getId(), folder1, null);
        assertNotNull(createdFolder);

        //Create a folder outside a site
        Folder createdFolderNonSite = createFolder(u1.getId(),  Nodes.PATH_MY, folder1, null);
        assertNotNull(createdFolderNonSite);

        Document document = createDocument(createdFolder, "d1.txt");
        Document documentNotDeleted = createDocument(createdFolder, "notdeleted1.txt");

        PublicApiClient.Paging paging = getPaging(0, 100);
        //First get any deleted nodes
        HttpResponse response = getAll(URL_DELETED_NODES, u1.getId(), paging, 200);
        List<Node> nodes = RestApiUtil.parseRestApiEntries(response.getJsonResponse(), Node.class);
        assertNotNull(nodes);
        int numOfNodes = nodes.size();

        delete(URL_NODES, u1.getId(), document.getId(), 204);
        delete(URL_NODES, u1.getId(), createdFolder.getId(), 204);
        delete(URL_NODES, u1.getId(), createdFolderNonSite.getId(), 204);

        response = getAll(URL_DELETED_NODES, u1.getId(), paging, 200);
        nodes = RestApiUtil.parseRestApiEntries(response.getJsonResponse(), Node.class);
        assertNotNull(nodes);
        assertEquals(numOfNodes+3,nodes.size());

        response = getSingle(URL_DELETED_NODES, u1.getId(), document.getId(), 200);
        Document node = jacksonUtil.parseEntry(response.getJsonResponse(), Document.class);
        assertNotNull(node);
        assertEquals(u1.getId(), node.getArchivedByUser().getId());
        assertTrue(node.getArchivedAt().after(now));

        response = getSingle(URL_DELETED_NODES, u1.getId(), createdFolder.getId(), 200);
        Folder fNode = jacksonUtil.parseEntry(response.getJsonResponse(), Folder.class);
        assertNotNull(fNode);
        assertEquals(u1.getId(), fNode.getArchivedByUser().getId());
        assertTrue(fNode.getArchivedAt().after(now));

        response = getSingle(URL_DELETED_NODES, u1.getId(), createdFolderNonSite.getId(), 200);
        fNode = jacksonUtil.parseEntry(response.getJsonResponse(), Folder.class);
        assertNotNull(fNode);
        assertEquals(u1.getId(), fNode.getArchivedByUser().getId());
        assertTrue(fNode.getArchivedAt().after(now));

        //Invalid node ref
        response = getSingle(URL_DELETED_NODES, u1.getId(), "iddontexist", 404);
    }

}
