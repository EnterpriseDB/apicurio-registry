/*
 * Copyright 2020 Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apicurio.registry.ui.servlets;

import java.io.IOException;
import java.util.Date;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

/**
 * {@link Filter} to add cache control headers for resources such as CSS and images.
 *
 * @author eric.wittmann@gmail.com
 */
@ApplicationScoped
public class ResourceCacheControlFilter implements Filter {

    public static void disableHttpCaching(HttpServletResponse httpResponse) {
        Date now = new Date();
        httpResponse.setDateHeader("Date", now.getTime()); //$NON-NLS-1$
        httpResponse.setDateHeader("Expires", expiredSinceYesterday(now)); //$NON-NLS-1$
        httpResponse.setHeader("Pragma", "no-cache"); //$NON-NLS-1$ //$NON-NLS-2$
        httpResponse.setHeader("Cache-control", "no-cache, no-store, must-revalidate"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static long expiredSinceYesterday(Date now) {
        return now.getTime() - 86400000L;
    }

    @Inject
    Logger log;

    /**
     * C'tor
     */
    public ResourceCacheControlFilter() {
    }

    /**
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig config) throws ServletException {
    }

    /**
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String requestURI = ((HttpServletRequest) request).getRequestURI();
        log.debug("Running cache control filter on: " + requestURI);
        Date now = new Date();
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setDateHeader("Date", now.getTime()); //$NON-NLS-1$

        boolean disableCaching = false;
        if (requestURI == null) {
            disableCaching = true;
        } else if (requestURI.contains("version.js")) {
            disableCaching = true;
        } else if (requestURI.contains("config.js")) {
            disableCaching = true;
        } else if (requestURI.contains("/apis/")) {
            disableCaching = true;
        }
        
        if (disableCaching) {
            log.debug("    |-> Caching is disabled.");
            disableHttpCaching(httpResponse);
        } else {
            // Cache most files for one year
            log.debug("    |-> Caching for one year.");
            httpResponse.setDateHeader("Expires", expiresInOneYear(now)); //$NON-NLS-1$
            httpResponse.setHeader("Cache-control", "public, max-age=31536000"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        chain.doFilter(request, response);
    }

    private long expiresInOneYear(Date now) {
        return now.getTime() + 31536000000L;
    }

    /**
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
    }
}
