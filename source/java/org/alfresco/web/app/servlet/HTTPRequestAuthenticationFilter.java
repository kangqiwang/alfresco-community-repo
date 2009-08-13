/*
 * Copyright (C) 2005-2009 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.web.app.servlet;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.repository.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Generic HTTP request filter for setting the authenticated user when used with authentication systems such as
 * SiteMinder, Novell IChains and CAS.
 * 
 * @author Andy Hind
 */
public class HTTPRequestAuthenticationFilter implements Filter
{
    private static Log logger = LogFactory.getLog(HTTPRequestAuthenticationFilter.class);

    private ServletContext context;

    private String loginPage;

    private AuthenticationComponent authComponent;

    private String httpServletRequestAuthHeaderName;

    // By default match everything if this is not set
    private String authPatternString = null;

    private Pattern authPattern = null;

    public void destroy()
    {
        // Nothing to do
    }

    /**
     * Run the filter
     * 
     * @param sreq
     *            ServletRequest
     * @param sresp
     *            ServletResponse
     * @param chain
     *            FilterChain
     * @exception IOException
     * @exception ServletException
     */
    public void doFilter(ServletRequest sreq, ServletResponse sresp, FilterChain chain) throws IOException,
            ServletException
    {
        // Get the HTTP request/response/session

        HttpServletRequest req = (HttpServletRequest) sreq;
        HttpServletResponse resp = (HttpServletResponse) sresp;

        HttpSession httpSess = req.getSession(true);

        // Check for the auth header

        String authHdr = req.getHeader(httpServletRequestAuthHeaderName);
        if (logger.isDebugEnabled())
        {
            if (authHdr == null)
            {
                logger.debug("Header not found: " + httpServletRequestAuthHeaderName);
            }
            else
            {
                logger.debug("Header is <" + authHdr + ">");
            }
        }

        // Throw an error if we have an unknown authentication

        if ((authHdr == null) || (authHdr.length() < 1))
        {
            resp.sendRedirect(req.getContextPath() + "/jsp/noaccess.jsp");
            return;
        }

        // Get the user

        String userName = "";
        if (authPattern != null)
        {
            Matcher matcher = authPattern.matcher(authHdr);
            if (matcher.matches())
            {
                userName = matcher.group();
                if ((userName == null) || (userName.length() < 1))
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Extracted null or empty user name from pattern "
                                + authPatternString + " against " + authHdr);
                    }
                    resp.sendRedirect(req.getContextPath() + "/jsp/noaccess.jsp");
                    return;
                }
            }
            else
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("no pattern match for " + authPatternString + " against " + authHdr);
                }
                resp.sendRedirect(req.getContextPath() + "/jsp/noaccess.jsp");
                return;
            }
        }
        else
        {
            userName = authHdr;
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("User = " + userName);
        }

        // See if there is a user in the session and test if it matches

        User user = (User) httpSess.getAttribute(AuthenticationHelper.AUTHENTICATION_USER);

        if (user != null)
        {
            try
            {
                // Debug

                if (logger.isDebugEnabled())
                    logger.debug("User " + user.getUserName() + " validate ticket");

                // Validate the user ticket

                if (user.getUserName().equals(userName))
                {

                    // Set the current locale
                    authComponent.clearCurrentSecurityContext();
                    authComponent.setCurrentUser(user.getUserName());
                    AuthenticationHelper.setupThread(this.context, req, resp);
                    chain.doFilter(sreq, sresp);
                    return;
                }
                else
                {
                    // No match
                    setAuthenticatedUser(req, resp, userName);
                }
            }
            catch (AuthenticationException ex)
            {
                if (logger.isErrorEnabled())
                    logger.error("Failed to validate user " + user.getUserName(), ex);
            }
        }

        setAuthenticatedUser(req, resp, userName);

        // Redirect the login page as it is never seen as we always login by name
        if (req.getRequestURI().endsWith(getLoginPage()) == true)
        {
            if (logger.isDebugEnabled())
                logger.debug("Login page requested, chaining ...");

            resp.sendRedirect(req.getContextPath() + "/faces/jsp/browse/browse.jsp");
            return;
        }
        else
        {
            chain.doFilter(sreq, sresp);
            return;
        }
    }

    /**
     * Set the authenticated user. It does not check that the user exists at the moment.
     * 
     * @param req
     *            the request
     * @param res
     *            the response
     * @param userName
     *            the user name
     */
    private void setAuthenticatedUser(HttpServletRequest req, HttpServletResponse res,
            String userName)
    {
        // Set the authentication
        authComponent.clearCurrentSecurityContext();
        authComponent.setCurrentUser(userName);
        
        // Set up the user information
        AuthenticationHelper.setUser(context, req, userName, true);

        // Set the locale using the session
        AuthenticationHelper.setupThread(this.context, req, res);
    }

    
    public void init(FilterConfig config) throws ServletException
    {
        // Save the context

        this.context = config.getServletContext();

        // Setup the authentication context

        WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(context);
        authComponent = (AuthenticationComponent) ctx.getBean("authenticationComponent");
                
        httpServletRequestAuthHeaderName = config.getInitParameter("httpServletRequestAuthHeaderName");
        if(httpServletRequestAuthHeaderName == null)
        {
            httpServletRequestAuthHeaderName = "x-user";
        }
        this.authPatternString = config.getInitParameter("authPatternString");
        if (this.authPatternString != null)
        {
            try
            {
                authPattern = Pattern.compile(this.authPatternString);
            }
            catch (PatternSyntaxException e)
            {
                logger.warn("Invalid pattern: " + this.authPatternString, e);
                authPattern = null;
            }
        }
        
    }

    /**
     * Return the login page address
     * 
     * @return String
     */
    private String getLoginPage()
    {
        if (loginPage == null)
        {
            loginPage = Application.getLoginPage(context);
        }

        return loginPage;
    }

}
