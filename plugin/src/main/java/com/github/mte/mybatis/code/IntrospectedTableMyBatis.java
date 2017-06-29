package com.github.mte.mybatis.code;

import org.mybatis.generator.api.GeneratedXmlFile;
import org.mybatis.generator.api.XmlFormatter;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.codegen.mybatis3.IntrospectedTableMyBatis3Impl;

import java.util.ArrayList;
import java.util.List;

public class IntrospectedTableMyBatis extends IntrospectedTableMyBatis3Impl {

    /** 新加的空 xml 文件的目录名及文件名中的部分 */
    private static final String NEW_XML_DIR = "custom";

    @Override
    public List<GeneratedXmlFile> getGeneratedXmlFiles() {
        List<GeneratedXmlFile> answer = new ArrayList<GeneratedXmlFile>();

        if (xmlMapperGenerator != null) {
            Document document = xmlMapperGenerator.getDocument();
            String xmlFileName = getMyBatis3XmlMapperFileName();
            String mapperPackage = getMyBatis3XmlMapperPackage();
            XmlFormatter formatter = context.getXmlFormatter();
            String targetProject = context.getSqlMapGeneratorConfiguration().getTargetProject();

            GeneratedXmlFile gxf = new GeneratedXmlFile(document,
                    xmlFileName, mapperPackage, targetProject, true, formatter);
            if (context.getPlugins().sqlMapGenerated(gxf, this)) {
                answer.add(gxf);
            }

            // 多生成一个文件
            Document customDocument = new Document(document.getPublicId(), document.getSystemId());
            XmlElement myElement = new XmlElement(document.getRootElement());
            myElement.getElements().clear();
            customDocument.setRootElement(myElement);

            GeneratedXmlFile custom = new GeneratedXmlFile(customDocument,
                    customFileName(xmlFileName), mapperPackage + "_" + NEW_XML_DIR, targetProject, true, formatter);
            if (context.getPlugins().sqlMapGenerated(custom, this)) {
                answer.add(custom);
            }
        }
        return answer;
    }

    private String customFileName(String xml) {
        int last = xml.lastIndexOf(".");
        return xml.substring(0, last) + "_" + NEW_XML_DIR + xml.substring(last);
    }
}
