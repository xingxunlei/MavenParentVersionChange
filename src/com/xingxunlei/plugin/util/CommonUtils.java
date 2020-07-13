package com.xingxunlei.plugin.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.xingxunlei.plugin.component.MavenVersionChangeComponent;
import com.xingxunlei.plugin.constant.Constants;
import com.yourkit.util.Strings;
import org.apache.commons.collections.CollectionUtils;
import org.jdom.Document;
import org.jdom.Element;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 公用工具类
 *
 * @author Simon.Xing
 * @date 2018/9/25
 */
public class CommonUtils {

    /**
     * 获取pom文件列表
     *
     * @param path
     * @return
     */
    public static List<String> getPomFiles(String path) {
        if (Strings.isNullOrEmpty(path)) {
            return Lists.newArrayList();
        }

        File file = new File(path);
        if (!file.exists()) {
            return Lists.newArrayList();
        }

        return listPomFile(file);
    }

    /**
     * 获取POM文件
     *
     * @param pomFilePath
     * @return
     */
    public static Document getPomDocument(String pomFilePath) {
        try (InputStream file = new FileInputStream(pomFilePath)) {
            return MavenVersionChangeComponent.SAX_BUILDER.build(file);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 获取POM文件中的version
     *
     * @return
     */
    public static String getPomVersion(Document document) {
        Element rootElement = document.getRootElement();
        if (null == rootElement) {
            return null;
        }

        List<Element> elementList = rootElement.getChildren(Constants.POM_NODE_VERSION, rootElement.getNamespace());
        if (CollectionUtils.isEmpty(elementList)) {
            return null;
        }

        return elementList.get(0).getValue();
    }

    /**
     * 获取module列表
     *
     * @param document
     * @return
     */
    public static List<String> getSubModuleList(Document document) {
        Element rootElement = document.getRootElement();
        if (null == rootElement) {
            return Lists.newArrayList();
        }

        List<Element> modulesElementList = rootElement.getChildren(Constants.POM_NODE_MODULES, rootElement.getNamespace());
        if (CollectionUtils.isEmpty(modulesElementList)) {
            return Lists.newArrayList();
        }

        List<String> subModules = Lists.newArrayList();
        Element modules = modulesElementList.get(0);
        for (Element element : modules.getChildren()) {
            subModules.add(element.getValue());
        }
        return subModules;
    }

    /**
     * 获取POM属性节点
     *
     * @param document
     * @return
     */
    public static Map<String, String> getPomPropertiesNodeMap(Document document) {
        Element rootElement = document.getRootElement();
        if (null == rootElement) {
            return Maps.newHashMap();
        }

        List<Element> propertiesElementList = rootElement.getChildren(Constants.POM_NODE_PROPERTIES, rootElement.getNamespace());
        if (CollectionUtils.isEmpty(propertiesElementList)) {
            return Maps.newHashMap();
        }

        Map<String, String> result = Maps.newHashMap();
        Element propertiesNode = propertiesElementList.get(0);
        List<Element> propertiesNodeChildren = propertiesNode.getChildren();
        propertiesNodeChildren = (null == propertiesNodeChildren) ? Lists.newArrayList() : propertiesNodeChildren;
        for (Element element : propertiesNodeChildren) {
            result.put(element.getName().trim(), element.getTextTrim());
        }
        return result;
    }

    private static List<String> listPomFile(File file) {
        File[] files = file.listFiles();
        if (files == null) {
            return Lists.newArrayList();
        }

        LinkedList<File> list = new LinkedList<>();
        List<String> outs = Lists.newArrayList();
        addFilePath(files, list, outs);

        while (!list.isEmpty()) {
            File tmp = list.removeFirst();
            if (tmp.isDirectory()) {
                files = tmp.listFiles();
                if (files == null) {
                    continue;
                }

                addFilePath(files, list, outs);
            }
        }

        return outs;
    }

    private static void addFilePath(File[] files, LinkedList<File> list, List<String> outs) {
        for (File f : files) {
            if (f.isDirectory()) {
                list.add(f);
            }
            if (!Constants.POM_FILE_NAME.equals(f.getName())) {
                continue;
            }
            outs.add(f.getAbsolutePath());
        }
    }

}
