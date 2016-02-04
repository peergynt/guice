package com.vaadin.guice.server;
/*
 * Copyright 2002-2014 the original author or authors.
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

import com.google.common.net.UrlEscapers;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

//NOTE: repackaged spring class!!!

/**
 * Helper class for URL path matching. Provides support for URL paths in RequestDispatcher includes
 * and support for consistent URL decoding.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rossen Stoyanchev
 * @since 14.01.2004
 */
class UrlPathHelper {

    /**
     * Special WebSphere request attribute, indicating the original request URI. Preferable over the
     * standard Servlet 2.4 forward attribute on WebSphere, simply because we need the very first
     * URI in the request forwarding chain.
     */
    private static final String WEBSPHERE_URI_ATTRIBUTE = "com.ibm.websphere.servlet.uri_non_decoded";

    static volatile Boolean websphereComplianceFlag;

    private boolean urlDecode = true;

    /**
     * Return the path within the servlet mapping for the given request, i.e. the part of the
     * request's URL beyond the part that called the servlet, or "" if the whole URL has been used
     * to identify the servlet. <p>Detects include request URL if called within a RequestDispatcher
     * include. <p>E.g.: servlet mapping = "/*"; request URI = "/test/a" -> "/test/a". <p>E.g.:
     * servlet mapping = "/"; request URI = "/test/a" -> "/test/a". <p>E.g.: servlet mapping =
     * "/test/*"; request URI = "/test/a" -> "/a". <p>E.g.: servlet mapping = "/test"; request URI =
     * "/test" -> "". <p>E.g.: servlet mapping = "/*.test"; request URI = "/a.test" -> "".
     *
     * @param request current HTTP request
     * @return the path within the servlet mapping, or ""
     */
    public String getPathWithinServletMapping(HttpServletRequest request) {
        String pathWithinApp = getPathWithinApplication(request);
        String servletPath = getServletPath(request);
        String sanitizedPathWithinApp = getSanitizedPath(pathWithinApp);
        String path;

        // if the app container sanitized the servletPath, check against the sanitized version
        if (servletPath.contains(sanitizedPathWithinApp)) {
            path = getRemainingPath(sanitizedPathWithinApp, servletPath, false);
        } else {
            path = getRemainingPath(pathWithinApp, servletPath, false);
        }

        if (path != null) {
            // Normal case: URI contains servlet path.
            return path;
        } else {
            // Special case: URI is different from servlet path.
            String pathInfo = request.getPathInfo();
            if (pathInfo != null) {
                // Use path info if available. Indicates index page within a servlet mapping?
                // e.g. with index page: URI="/", servletPath="/index.html"
                return pathInfo;
            }
            if (!this.urlDecode) {
                // No path info... (not mapped by prefix, nor by extension, nor "/*")
                // For the default servlet mapping (i.e. "/"), urlDecode=false can
                // cause issues since getServletPath() returns a decoded path.
                // If decoding pathWithinApp yields a match just use pathWithinApp.
                path = getRemainingPath(decodeInternal(pathWithinApp), servletPath, false);
                if (path != null) {
                    return pathWithinApp;
                }
            }
            // Otherwise, use the full servlet path.
            return servletPath;
        }
    }

    /**
     * Return the path within the web application for the given request. <p>Detects include request
     * URL if called within a RequestDispatcher include.
     *
     * @param request current HTTP request
     * @return the path within the web application
     */
    public String getPathWithinApplication(HttpServletRequest request) {
        String contextPath = getContextPath(request);
        String requestUri = getRequestUri(request);
        String path = getRemainingPath(requestUri, contextPath, true);
        if (path != null) {
            // Normal case: URI contains context path.
            return (StringUtils.hasText(path) ? path : "/");
        } else {
            return requestUri;
        }
    }

    /**
     * Match the given "mapping" to the start of the "requestUri" and if there is a match return the
     * extra part. This method is needed because the context path and the servlet path returned by
     * the HttpServletRequest are stripped of semicolon content unlike the requesUri.
     */
    private String getRemainingPath(String requestUri, String mapping, boolean ignoreCase) {
        int index1 = 0;
        int index2 = 0;
        for (; (index1 < requestUri.length()) && (index2 < mapping.length()); index1++, index2++) {
            char c1 = requestUri.charAt(index1);
            char c2 = mapping.charAt(index2);
            if (c1 == ';') {
                index1 = requestUri.indexOf('/', index1);
                if (index1 == -1) {
                    return null;
                }
                c1 = requestUri.charAt(index1);
            }
            if (c1 == c2) {
                continue;
            } else if (ignoreCase && (Character.toLowerCase(c1) == Character.toLowerCase(c2))) {
                continue;
            }
            return null;
        }
        if (index2 != mapping.length()) {
            return null;
        } else if (index1 == requestUri.length()) {
            return "";
        } else if (requestUri.charAt(index1) == ';') {
            index1 = requestUri.indexOf('/', index1);
        }
        return (index1 != -1 ? requestUri.substring(index1) : "");
    }

    /**
     * Sanitize the given path with the following rules: <ul> <li>replace all "//" by "/"</li>
     * </ul>
     */
    private String getSanitizedPath(final String path) {
        String sanitized = path;
        while (true) {
            int index = sanitized.indexOf("//");
            if (index < 0) {
                break;
            } else {
                sanitized = sanitized.substring(0, index) + sanitized.substring(index + 1);
            }
        }
        return sanitized;
    }

    /**
     * Return the request URI for the given request, detecting an include request URL if called
     * within a RequestDispatcher include. <p>As the value returned by {@code
     * request.getRequestURI()} is <i>not</i> decoded by the servlet container, this method will
     * decode it. <p>The URI that the web container resolves <i>should</i> be correct, but some
     * containers like JBoss/Jetty incorrectly include ";" strings like ";jsessionid" in the URI.
     * This method cuts off such incorrect appendices.
     *
     * @param request current HTTP request
     * @return the request URI
     */
    public String getRequestUri(HttpServletRequest request) {
        String uri = (String) request.getAttribute("javax.servlet.include.request_uri");
        if (uri == null) {
            uri = request.getRequestURI();
        }
        return decodeAndCleanUriString(request, uri);
    }

    /**
     * Return the context path for the given request, detecting an include request URL if called
     * within a RequestDispatcher include. <p>As the value returned by {@code
     * request.getContextPath()} is <i>not</i> decoded by the servlet container, this method will
     * decode it.
     *
     * @param request current HTTP request
     * @return the context path
     */
    public String getContextPath(HttpServletRequest request) {
        String contextPath = (String) request.getAttribute("javax.servlet.include.context_path");
        if (contextPath == null) {
            contextPath = request.getContextPath();
        }
        if ("/".equals(contextPath)) {
            // Invalid case, but happens for includes on Jetty: silently adapt it.
            contextPath = "";
        }
        return decodeRequestString(contextPath);
    }

    /**
     * Return the servlet path for the given request, regarding an include request URL if called
     * within a RequestDispatcher include. <p>As the value returned by {@code
     * request.getServletPath()} is already decoded by the servlet container, this method will not
     * attempt to decode it.
     *
     * @param request current HTTP request
     * @return the servlet path
     */
    public String getServletPath(HttpServletRequest request) {
        String servletPath = (String) request.getAttribute("javax.servlet.include.servlet_path");
        if (servletPath == null) {
            servletPath = request.getServletPath();
        }
        if (servletPath.length() > 1 && servletPath.endsWith("/") && shouldRemoveTrailingServletPathSlash(request)) {
            // On WebSphere, in non-compliant mode, for a "/foo/" case that would be "/foo"
            // on all other servlet containers: removing trailing slash, proceeding with
            // that remaining slash as final lookup path...
            servletPath = servletPath.substring(0, servletPath.length() - 1);
        }
        return servletPath;
    }

    /**
     * Decode the supplied URI string and strips any extraneous portion after a ';'.
     */
    private String decodeAndCleanUriString(HttpServletRequest request, String uri) {
        uri = removeSemicolonContent(uri);
        uri = decodeRequestString(uri);
        return uri;
    }


    public String decodeRequestString(String source) {
        if (this.urlDecode) {
            return decodeInternal(source);
        }
        return source;
    }

    @SuppressWarnings("deprecation")
    private String decodeInternal(String source) {
        return UrlEscapers.urlPathSegmentEscaper().escape(source);
    }

    String removeSemicolonContent(String requestUri) {
        int semicolonIndex = requestUri.indexOf(';');
        while (semicolonIndex != -1) {
            int slashIndex = requestUri.indexOf('/', semicolonIndex);
            String start = requestUri.substring(0, semicolonIndex);
            requestUri = (slashIndex != -1) ? start + requestUri.substring(slashIndex) : start;
            semicolonIndex = requestUri.indexOf(';', semicolonIndex);
        }
        return requestUri;
    }

    private boolean shouldRemoveTrailingServletPathSlash(HttpServletRequest request) {
        if (request.getAttribute(WEBSPHERE_URI_ATTRIBUTE) == null) {
            // Regular servlet container: behaves as expected in any case,
            // so the trailing slash is the result of a "/" url-pattern mapping.
            // Don't remove that slash.
            return false;
        }
        if (websphereComplianceFlag == null) {
            ClassLoader classLoader = UrlPathHelper.class.getClassLoader();
            String className = "com.ibm.ws.webcontainer.WebContainer";
            String methodName = "getWebContainerProperties";
            String propName = "com.ibm.ws.webcontainer.removetrailingservletpathslash";
            boolean flag = false;
            try {
                Class<?> cl = classLoader.loadClass(className);
                Properties prop = (Properties) cl.getMethod(methodName).invoke(null);
                flag = Boolean.parseBoolean(prop.getProperty(propName));
            } catch (Throwable ex) {
                System.err.println("Could not introspect WebSphere web container properties: " + ex);
            }
            websphereComplianceFlag = flag;
        }
        // Don't bother if WebSphere is configured to be fully Servlet compliant.
        // However, if it is not compliant, do remove the improper trailing slash!
        return !websphereComplianceFlag;
    }

}

