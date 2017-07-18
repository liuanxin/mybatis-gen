package com.github.liuanxin.mybatis.code;

import com.github.liuanxin.mybatis.code.generator.CustomAliasXmlMapperGenerator;
import com.github.liuanxin.mybatis.code.generator.CustomOgnlEmptyXmlMapperGenerator;
import org.mybatis.generator.api.GeneratedXmlFile;
import org.mybatis.generator.api.XmlFormatter;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.codegen.AbstractJavaClientGenerator;
import org.mybatis.generator.codegen.mybatis3.IntrospectedTableMyBatis3Impl;
import org.mybatis.generator.codegen.mybatis3.javamapper.*;
import org.mybatis.generator.internal.ObjectFactory;

import java.util.ArrayList;
import java.util.List;

public class IntrospectedTableMyBatis extends IntrospectedTableMyBatis3Impl {

    /** 新加的空 xml 文件的目录名及文件名中的部分 */
    private static final String NEW_XML_DIR = "-custom";

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
            addNewDocument(answer, document, xmlFileName, mapperPackage, formatter, targetProject);
        }
        return answer;
    }

    private void addNewDocument(List<GeneratedXmlFile> answer, Document document,
                                String xmlFileName, String mapperPackage,
                                XmlFormatter formatter, String targetProject) {
        Document customDocument = new Document(document.getPublicId(), document.getSystemId());
        XmlElement myElement = new XmlElement(document.getRootElement());
        myElement.getElements().clear();
        customDocument.setRootElement(myElement);

        int last = xmlFileName.lastIndexOf(".");
        String newFileName = xmlFileName.substring(0, last) + NEW_XML_DIR + xmlFileName.substring(last);
        String newMapperPackage = mapperPackage + NEW_XML_DIR;

        GeneratedXmlFile custom = new GeneratedXmlFile(customDocument,
                newFileName, newMapperPackage, targetProject, true, formatter);
        if (context.getPlugins().sqlMapGenerated(custom, this)) {
            answer.add(custom);
        }
    }

    @Override
    protected AbstractJavaClientGenerator createJavaClientGenerator() {
        if (context.getJavaClientGeneratorConfiguration() == null) {
            return null;
        }
        String type = context.getJavaClientGeneratorConfiguration().getConfigurationType();

        AbstractJavaClientGenerator javaGenerator;
        if ("ALIAS".equalsIgnoreCase(type)) {
            javaGenerator = new CustomAliasXmlMapperGenerator();
        } else if ("ALIASANDEMPTY".equalsIgnoreCase(type)) {
            javaGenerator = new CustomOgnlEmptyXmlMapperGenerator();
        } else if ("XMLMAPPER".equalsIgnoreCase(type)) { //$NON-NLS-1$
            javaGenerator = new JavaMapperGenerator();
        } else if ("MIXEDMAPPER".equalsIgnoreCase(type)) { //$NON-NLS-1$
            javaGenerator = new MixedClientGenerator();
        } else if ("ANNOTATEDMAPPER".equalsIgnoreCase(type)) { //$NON-NLS-1$
            javaGenerator = new AnnotatedClientGenerator();
        } else if ("MAPPER".equalsIgnoreCase(type)) { //$NON-NLS-1$
            javaGenerator = new JavaMapperGenerator();
        } else {
            javaGenerator = (AbstractJavaClientGenerator) ObjectFactory.createInternalObject(type);
        }

        return javaGenerator;
    }
}
