/*
 * Copyright (C) 2005-2012 Alfresco Software Limited.
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
package org.alfresco.repo.webdav;

import static org.junit.Assert.*;

import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests for the WebDAVHelper class.
 * 
 * @author Matt Ward
 */
@RunWith(MockitoJUnitRunner.class)
public class WebDAVHelperTest
{
    private WebDAVHelper davHelper;
    private @Mock ServiceRegistry serviceRegistry;
    private @Mock AuthenticationService authService;
    private @Mock TenantService tenantService;
    
    @Before
    public void setUp() throws Exception
    {
        davHelper = new WebDAVHelper("", serviceRegistry, authService, tenantService);
    }

    @Test
    public void canGetDestinationPathWhenNoServletName()
    {
        assertPathForURL("/the-tenant.com/the-site/path/to/file",
                    "http://webdav.alfresco.com/the-tenant.com/the-site/path/to/file");
     
    }
    
    /**
     * THOR-1459: WebDAV: site names cannot start with 'webdav'.
     * <p>
     * <code>/webdav-test</code> begins with servlet path <code>/webdav</code>
     */
    @Test
    public void canGetDestinationPathWhenPathElementStartsWithServletPath()
    {
        assertPathForURL("/t/webdav-test/path/to/file",
                    "http://webdav.alfresco.com/t/webdav-test/path/to/file");

        // Looks like /contextPath/servletName in URL's path prefix, but isn't
        assertPathForURL("/alfresco/webdav-test/path/to/file",
                    "http://webdav.alfresco.com/alfresco/webdav-test/path/to/file");
    }
    
    @Test
    public void canGetDestinationPathWhenPrefixedWithContextPathAndServletName()
    {
        assertPathForURL("/path/to/file",
                    "http://webdav.alfresco.com/alfresco/webdav/path/to/file");
        
        assertPathForURL("/alfresco/webdav/path/to/file",
                    "http://webdav.alfresco.com/alfresco/webdav/alfresco/webdav/path/to/file");

        assertPathForURL("/my/folder/alfresco/webdav/path/to/file",
                    "http://webdav.alfresco.com/alfresco/webdav/my/folder/alfresco/webdav/path/to/file");
    }

    /**
     * Check that the expected path was extracted from a given URL.
     * 
     * @param path The expected path.
     * @param url URL to extract the path from.
     */
    private void assertPathForURL(String path, String url)
    {
        assertEquals(path, davHelper.getDestinationPath("/alfresco", "/webdav", url));
    }
}
