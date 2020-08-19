package com.xingxunlei.plugin.constant;

/**
 * 静态常量
 *
 * @author Simon.Xing
 * @date 2018/9/25
 */
public final class Constants {
    public static final String COMPONENT_NAME = "Maven Version Change";
    public static final String POM_FILE_NAME = "pom.xml";
    public static final String POM_NODE_VERSION = "version";
    public static final String POM_NODE_MODULES = "modules";
    public static final String POM_NODE_DEPENDENCIES = "dependencies";
    public static final String POM_NODE_ARTIFACTID = "artifactId";
    public static final String POM_NODE_DEPENDENCY = "dependency";
    public static final String POM_NODE_DEPENDENCY_MANAGEMENT = "dependencyManagement";
    public static final String POM_NODE_PARENT = "parent";
    public static final String POM_NODE_PROPERTIES = "properties";
    public static final String POM_NODE_VERSION_EL_EXPRESSION_PREFIX = "${";
    public static final String POM_NODE_PROJECT_VERSION = "project.version";
    public static final String FILE_SP = System.getProperty("file.separator");
}
