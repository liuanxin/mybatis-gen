package com.github.liuanxin.mybatis.code.mapper;

import com.github.liuanxin.mybatis.code.elements.BaseColumnListElementGenerator;
import com.github.liuanxin.mybatis.code.elements.BlobColumnListElementGenerator;
import com.github.liuanxin.mybatis.code.elements.ResultMapWithBLOBsElementGenerator;
import com.github.liuanxin.mybatis.code.elements.ResultMapWithoutBLOBsElementGenerator;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.codegen.mybatis3.xmlmapper.XMLMapperGenerator;
import org.mybatis.generator.codegen.mybatis3.xmlmapper.elements.AbstractXmlElementGenerator;

public class CustomAliasXMLMapperGenerator extends XMLMapperGenerator {

    // 下面四个在 sql 和 result map 中添加一个别名, 供多表时使用

    @Override
    protected void addBaseColumnListElement(XmlElement parentElement) {
        if (introspectedTable.getRules().generateBaseColumnList()) {
            AbstractXmlElementGenerator elementGenerator = new BaseColumnListElementGenerator();
            initializeAndExecuteGenerator(elementGenerator, parentElement);
        }
    }

    @Override
    protected void addBlobColumnListElement(XmlElement parentElement) {
        if (introspectedTable.getRules().generateBlobColumnList()) {
            AbstractXmlElementGenerator elementGenerator = new BlobColumnListElementGenerator();
            initializeAndExecuteGenerator(elementGenerator, parentElement);
        }
    }

    @Override
    protected void addResultMapWithoutBLOBsElement(XmlElement parentElement) {
        if (introspectedTable.getRules().generateBaseResultMap()) {
            AbstractXmlElementGenerator elementGenerator = new ResultMapWithoutBLOBsElementGenerator(false);
            initializeAndExecuteGenerator(elementGenerator, parentElement);
        }
    }

    @Override
    protected void addResultMapWithBLOBsElement(XmlElement parentElement) {
        if (introspectedTable.getRules().generateResultMapWithBLOBs()) {
            AbstractXmlElementGenerator elementGenerator = new ResultMapWithBLOBsElementGenerator();
            initializeAndExecuteGenerator(elementGenerator, parentElement);
        }
    }
}
