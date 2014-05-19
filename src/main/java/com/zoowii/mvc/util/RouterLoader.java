package com.zoowii.mvc.util;

import com.zoowii.mvc.http.HttpRequest;
import com.zoowii.mvc.http.HttpResponse;
import com.zoowii.mvc.http.HttpRouter;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RouterLoader {

    /**
     * routes文件的 import $packageFullName 的命令
     */
    private static class RouteFileImportCommand {
        public String importPackage;

        public RouteFileImportCommand(String line) {
            String[] splited = line.split("\\s+");
            if (splited.length < 2) {
                importPackage = null;
            } else {
                importPackage = splited[1].trim();
            }
        }

        public boolean isValid() {
            return importPackage != null && importPackage.length() > 0;
        }
    }

    /**
     * routes文件的路由命令
     */
    private static class RouteFileRouteCommand {
        public String httpMethod;
        public String urlPattern;
        public Class handlerClass;
        public String handlerClassName;
        public String handlerMethodName;
        private boolean valid = true;

        public RouteFileRouteCommand(String line) {
            String[] items = line.split("\\s+");
            if (items.length < 4) {
                valid = false;
                return;
            }
            httpMethod = items[0].trim().toUpperCase();
            urlPattern = items[1].trim();
            handlerClassName = items[2].trim();
            handlerMethodName = items[3].trim();
        }

        public boolean isValid() {
            return valid;
        }
    }

    private static class RouteFileSystemCommand {
        public String systemCommandName;
        public String[] systemCommandParams;
        private boolean valid = true;

        public boolean isValid() {
            return valid;
        }

        public RouteFileSystemCommand(String line) {
            String[] items = line.split("\\s+");
            if (items.length < 2) {
                valid = false;
                return;
            }
            systemCommandName = items[1].trim();
            systemCommandParams = new String[items.length - 2];
            for (int i = 2; i < items.length; ++i) {
                systemCommandParams[i - 2] = items[i].trim();
            }
        }
    }

    private static class RouteFile {
        private List<RouteFileImportCommand> importCommands = new ArrayList<RouteFileImportCommand>();
        private List<RouteFileRouteCommand> routeCommands = new ArrayList<RouteFileRouteCommand>();
        private List<RouteFileSystemCommand> systemCommands = new ArrayList<RouteFileSystemCommand>(); // like 404 page, etc.

        public RouteFile(InputStream inputStream) throws IOException {
            List<String> lines = IOUtils.readLines(inputStream);
            for (String line : lines) {
                if (line.length() <= 0 || line.startsWith("//")) {
                    continue;
                }
                if (line.startsWith("import")) {
                    RouteFileImportCommand importCommand = new RouteFileImportCommand(line);
                    if (importCommand.isValid()) {
                        importCommands.add(importCommand);
                    }
                } else if (line.startsWith("system")) {
                    RouteFileSystemCommand systemCommand = new RouteFileSystemCommand(line);
                    if (systemCommand.isValid()) {
                        systemCommands.add(systemCommand);
                    }
                } else {
                    RouteFileRouteCommand routeCommand = new RouteFileRouteCommand(line);
                    if (routeCommand.isValid()) {
                        routeCommands.add(routeCommand);
                    }
                }
            }
            preProcess();
        }

        public List<String> getImportedPackages() {
            List<String> packages = new ArrayList<String>();
            for (RouteFileImportCommand importCommand : importCommands) {
                packages.add(importCommand.importPackage);
            }
            return packages;
        }

        /**
         * 预处理
         * 1. 将routes命令中的类名补全，形成具有完整包路径的类名
         */
        public void preProcess() {
            List<String> packages = getImportedPackages();
            List<RouteFileRouteCommand> validRouteCommands = new ArrayList<RouteFileRouteCommand>();
            for (RouteFileRouteCommand routeCommand : routeCommands) {
                Class clz = ResourceUtil.findClassInPackages(packages, routeCommand.handlerClassName);
                if (clz != null) {
                    routeCommand.handlerClass = clz;
                    validRouteCommands.add(routeCommand);
                }
            }
            routeCommands = validRouteCommands;
        }
    }

    public static void loadRouterFromFile(String path) throws IOException {
        if (HttpRouter.isRouteTableInited()) {
            return;
        }
        InputStream inputStream = ResourceUtil.readResource(path);
        RouteFile routeFile = new RouteFile(inputStream);
        for (RouteFileRouteCommand routeCommand : routeFile.routeCommands) {
            try {
                HttpRouter.addRouter(routeCommand.httpMethod, routeCommand.urlPattern, routeCommand.handlerClass, routeCommand.handlerMethodName);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        for (RouteFileSystemCommand systemCommand : routeFile.systemCommands) {
            if (systemCommand.systemCommandName.equals("404")) {
                try {
                    Class handlerClass = ResourceUtil.findClassInPackages(routeFile.getImportedPackages(), systemCommand.systemCommandParams[0]);
                    HttpRouter.setWeb404Handler(handlerClass, handlerClass.getMethod(systemCommand.systemCommandParams[1], HttpRequest.class, HttpResponse.class));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
