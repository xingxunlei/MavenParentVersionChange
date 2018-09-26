package com.xingxunlei.plugin;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.xingxunlei.plugin.component.MavenVersionChangeComponent;
import com.xingxunlei.plugin.constant.Constants;
import com.xingxunlei.plugin.util.CommonUtils;
import com.yourkit.util.Strings;
import org.apache.commons.collections.CollectionUtils;
import org.jdom.Document;
import org.jdom.Element;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 插件执行主类
 *
 * @author Simon.Xing
 * @date 2018/9/25
 */
public class MavenVersionChangeAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (null == project) {
            return;
        }

        String basePath = project.getBasePath();
        if (Strings.isNullOrEmpty(basePath)) {
            return;
        }

        List<String> poms = CommonUtils.getPomFiles(basePath);
        if (CollectionUtils.isEmpty(poms)) {
            return;
        }

        Document parentPomDocument = CommonUtils.getPomDocument(poms.get(0));
        String parentPomVersion = CommonUtils.getPomVersion(parentPomDocument);
        if (Strings.isNullOrEmpty(parentPomVersion)) {
            return;
        }

        String newVersion = Messages.showInputDialog("新版本:", "当前版本(" + parentPomVersion + ")", Messages.getInformationIcon());
        newVersion = null == newVersion ? "" : newVersion;
        newVersion = newVersion.trim();
        if (Strings.isNullOrEmpty(newVersion)) {
            return;
        }

        updateRootPomVersion(newVersion, parentPomDocument);

        basePath = poms.get(0).replace(Constants.POM_FILE_NAME, "");

        Map<String, String> parentPomPropertiesMap = CommonUtils.getPomPropertiesNodeMap(parentPomDocument);
        List<String> subModuleList = CommonUtils.getSubModuleList(parentPomDocument);
        for (String module : subModuleList) {
            String modulePath = basePath + module + File.separator + Constants.POM_FILE_NAME;
            Document moduleDocument = CommonUtils.getPomDocument(modulePath);
            updatePomVersion(newVersion, subModuleList, moduleDocument, parentPomPropertiesMap);
            writeNewPomFile(moduleDocument, modulePath);
        }

        updatePomPropertiesNode(parentPomDocument, parentPomPropertiesMap);
        writeNewPomFile(parentPomDocument, poms.get(0));
        refreshActiveEditor(project);
    }

    private void updateRootPomVersion(String newVersion, Document document) {
        Element rootElement = document.getRootElement();
        if (null == rootElement) {
            return;
        }

        List<Element> versionElementList = rootElement.getChildren(Constants.POM_NODE_VERSION, rootElement.getNamespace());
        versionElementList = (null == versionElementList) ? Lists.newArrayList() : versionElementList;
        for (Element element : versionElementList) {
            element.setText(newVersion);
        }
    }

    private void updatePomVersion(String newVersion, List<String> subModuleList, Document document, Map<String, String> parentPomPropertiesMap) {
        Element rootElement = document.getRootElement();
        if (null == rootElement) {
            return;
        }

        Set<String> moduleSetHash = Sets.newHashSet();
        moduleSetHash.addAll(subModuleList);

        Map<String, String> currentPomPropertiesMap = Maps.newHashMap();
        currentPomPropertiesMap.putAll(parentPomPropertiesMap);
        currentPomPropertiesMap.putAll(CommonUtils.getPomPropertiesNodeMap(document));

        List<Element> versionElementList = rootElement.getChildren(Constants.POM_NODE_VERSION, rootElement.getNamespace());
        versionElementList = (null == versionElementList) ? Lists.newArrayList() : versionElementList;
        for (Element element : versionElementList) {
            element.setText(newVersion);
        }
        List<Element> parentNodeList = rootElement.getChildren(Constants.POM_NODE_PARENT, rootElement.getNamespace());
        if (null != parentNodeList && !parentNodeList.isEmpty()) {
            Element parentNode = parentNodeList.get(0);
            List<Element> parentNodeVersionList = parentNode.getChildren(Constants.POM_NODE_VERSION, rootElement.getNamespace());
            if (null != parentNodeVersionList && !parentNodeVersionList.isEmpty()) {
                parentNodeVersionList.get(0).setText(newVersion);
            }
        }
        List<Element> dependenciesElementList = rootElement.getChildren(Constants.POM_NODE_DEPENDENCIES, rootElement.getNamespace());
        if (null != dependenciesElementList && !dependenciesElementList.isEmpty()) {
            Element dependenciesNode = dependenciesElementList.get(0);
            updateDependencyNodeVersion(newVersion, dependenciesNode, moduleSetHash, currentPomPropertiesMap);
        }
        List<Element> dependencyManagementList = rootElement.getChildren(Constants.POM_NODE_DEPENDENCY_MANAGEMENT, rootElement.getNamespace());
        if (null != dependencyManagementList && !dependencyManagementList.isEmpty()) {
            Element dependencyManagement = dependencyManagementList.get(0);
            dependenciesElementList = dependencyManagement.getChildren(Constants.POM_NODE_DEPENDENCIES, rootElement.getNamespace());
            if (null != dependenciesElementList && !dependenciesElementList.isEmpty()) {
                Element dependenciesNode = dependenciesElementList.get(0);
                updateDependencyNodeVersion(newVersion, dependenciesNode, moduleSetHash, currentPomPropertiesMap);
            }
        }
        for (String key : parentPomPropertiesMap.keySet()) {
            if (currentPomPropertiesMap.containsKey(key)) {
                parentPomPropertiesMap.put(key, currentPomPropertiesMap.get(key));
            }
        }
        List<Element> propertiesElementList = rootElement.getChildren(Constants.POM_NODE_PROPERTIES, rootElement.getNamespace());
        if (null != propertiesElementList && !propertiesElementList.isEmpty()) {
            Element propertiesNode = propertiesElementList.get(0);
            List<Element> propertiesNodeChildren = propertiesNode.getChildren();
            propertiesNodeChildren = (null == propertiesNodeChildren) ? Lists.newArrayList() : propertiesNodeChildren;
            for (Element element : propertiesNodeChildren) {
                if (currentPomPropertiesMap.containsKey(element.getName().trim())) {
                    element.setText(currentPomPropertiesMap.get(element.getName().trim()));
                }
            }
        }
    }

    private void writeNewPomFile(Document document, String path) {
        if (Strings.isNullOrEmpty(path)) {
            return;
        }

        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(path), Charsets.UTF_8.name())) {
            MavenVersionChangeComponent.XML_OUT_PUTTER.output(document, osw);
        } catch (Exception ex) {
            return;
        }
    }

    private void updatePomPropertiesNode(Document document, Map<String, String> pomPropertiesMap) {
        Element rootElement = document.getRootElement();
        if (null == rootElement) {
            return;
        }

        List<Element> propertiesElementList = rootElement.getChildren(Constants.POM_NODE_PROPERTIES, rootElement.getNamespace());
        if (null == propertiesElementList || propertiesElementList.isEmpty()
                || null == pomPropertiesMap || pomPropertiesMap.isEmpty()) {
            return;
        }

        Element propertiesNode = propertiesElementList.get(0);
        List<Element> propertiesNodeChildren = propertiesNode.getChildren();
        propertiesNodeChildren = (null == propertiesNodeChildren) ? Lists.newArrayList() : propertiesNodeChildren;
        for (Element element : propertiesNodeChildren) {
            if (pomPropertiesMap.containsKey(element.getName().trim())) {
                element.setText(pomPropertiesMap.get(element.getName().trim()));
            }
        }
    }

    private void refreshActiveEditor(Project project) {
        VirtualFileManager.getInstance().refreshWithoutFileWatcher(true);
        final Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (null == editor) {
            return;
        }

        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (null == virtualFile) {
            return;
        }

        try {
            ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> editor.getDocument().setText(FileDocumentManager.getInstance().getDocument(virtualFile).getText())));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateDependencyNodeVersion(String newVersion, Element dependenciesNode, Set<String> moduleSetHash, final Map<String, String> pomPropertiesNodeMap) {
        List<Element> elementList = dependenciesNode.getChildren(Constants.POM_NODE_DEPENDENCY, dependenciesNode.getNamespace());
        if (null == elementList || elementList.isEmpty()) {
            return;
        }

        for (Element element : elementList) {
            List<Element> artifactIdList = element.getChildren(Constants.POM_NODE_ARTIFACTID, dependenciesNode.getNamespace());
            artifactIdList = (null == artifactIdList) ? Lists.newArrayList() : artifactIdList;
            if (artifactIdList.isEmpty()) {
                continue;
            }

            Element artifactId = artifactIdList.get(0);
            if (moduleSetHash.contains(artifactId.getValue().trim())) {
                List<Element> versions = element.getChildren(Constants.POM_NODE_VERSION, dependenciesNode.getNamespace());
                versions = (null == versions) ? Lists.newArrayList() : versions;
                versions.forEach(element1 -> {
                    String version = element1.getValue();
                    version = (null == version) ? "" : version.trim();
                    if (version.startsWith(Constants.POM_NODE_VERSION_EL_EXPRESSION_PREFIX)) {
                        version = parseELExpressionVersion(version);
                        if (pomPropertiesNodeMap.containsKey(version)) {
                            pomPropertiesNodeMap.put(version, newVersion);
                            return;
                        }
                    }
                    element1.setText(newVersion);
                });
            }
        }
    }

    private String parseELExpressionVersion(String version) {
        if (!version.startsWith(Constants.POM_NODE_VERSION_EL_EXPRESSION_PREFIX)) {
            return "";
        }

        return version.substring(Constants.POM_NODE_VERSION_EL_EXPRESSION_PREFIX.length(), version.length() - 1);
    }


}
