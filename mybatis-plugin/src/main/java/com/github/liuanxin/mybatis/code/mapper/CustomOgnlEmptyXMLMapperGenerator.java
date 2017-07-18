package com.github.liuanxin.mybatis.code.mapper;

import com.github.liuanxin.mybatis.code.elements.InsertSelectiveElementGenerator;
import com.github.liuanxin.mybatis.code.elements.UpdateByExampleSelectiveElementGenerator;
import com.github.liuanxin.mybatis.code.elements.UpdateByPrimaryKeySelectiveElementGenerator;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.codegen.mybatis3.xmlmapper.elements.AbstractXmlElementGenerator;

public class CustomOgnlEmptyXMLMapperGenerator extends CustomAliasXMLMapperGenerator {

    // 下面三个在比较时使用 Ognl 运算, 不使用 != null

    @Override
    protected void addInsertSelectiveElement(XmlElement parentElement) {
        if (introspectedTable.getRules().generateInsertSelective()) {
            AbstractXmlElementGenerator elementGenerator = new InsertSelectiveElementGenerator();
            initializeAndExecuteGenerator(elementGenerator, parentElement);
        }
    }

    @Override
    protected void addUpdateByExampleSelectiveElement(XmlElement parentElement) {
        if (introspectedTable.getRules().generateUpdateByExampleSelective()) {
            AbstractXmlElementGenerator elementGenerator = new UpdateByExampleSelectiveElementGenerator();
            initializeAndExecuteGenerator(elementGenerator, parentElement);
        }
    }

    @Override
    protected void addUpdateByPrimaryKeySelectiveElement(XmlElement parentElement) {
        if (introspectedTable.getRules().generateUpdateByPrimaryKeySelective()) {
            AbstractXmlElementGenerator elementGenerator = new UpdateByPrimaryKeySelectiveElementGenerator();
            initializeAndExecuteGenerator(elementGenerator, parentElement);
        }
    }
}
