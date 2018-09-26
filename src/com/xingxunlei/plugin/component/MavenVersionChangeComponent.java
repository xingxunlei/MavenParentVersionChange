package com.xingxunlei.plugin.component;

import com.intellij.openapi.components.ApplicationComponent;
import com.xingxunlei.plugin.constant.Constants;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jetbrains.annotations.NotNull;

/**
 * 组件
 *
 * @author Simon.Xing
 * @date 2018/9/25
 */
public class MavenVersionChangeComponent implements ApplicationComponent {
    public static final SAXBuilder SAX_BUILDER = new SAXBuilder();
    public static final XMLOutputter XML_OUT_PUTTER = new XMLOutputter();

    public MavenVersionChangeComponent() {
    }

    @Override
    public void initComponent() {

    }

    @Override
    public void disposeComponent() {

    }

    @NotNull
    @Override
    public String getComponentName() {
        return Constants.COMPONENT_NAME;
    }
}
