package com.vaadin.guice.server;

import com.vaadin.server.VaadinRequest;

final class PathUtil {
    private PathUtil(){
    }

    static String preparePath(String path) {
        if (path.length() > 0 && !path.startsWith("/")) {
            path = "/".concat(path);
        } else {
            // remove terminal slash from mapping
            path = path.replaceAll("/$", "");
        }

        return path;
    }

    static String extractUIPathFromRequest(VaadinRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.length() > 1) {
            String path = pathInfo;
            final int indexOfBang = path.indexOf('!');
            if (indexOfBang > -1) {
                path = path.substring(0, indexOfBang);
            }

            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            return path;
        }
        return "";
    }
}
