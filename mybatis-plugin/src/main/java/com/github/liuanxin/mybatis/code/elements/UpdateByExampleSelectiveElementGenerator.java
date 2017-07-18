package com.github.liuanxin.mybatis.code.elements;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.codegen.mybatis3.MyBatis3FormattingUtilities;
import org.mybatis.generator.codegen.mybatis3.xmlmapper.elements.AbstractXmlElementGenerator;

/** 比较时使用 Ognl 运算, 不使用 != null */
public class UpdateByExampleSelectiveElementGenerator extends AbstractXmlElementGenerator {

    public UpdateByExampleSelectiveElementGenerator() {
        super();
    }

    @Override
    public void addElements(XmlElement parentElement) {
        XmlElement answer = new XmlElement("update"); //$NON-NLS-1$

        answer.addAttribute(new Attribute("id", introspectedTable.getUpdateByExampleSelectiveStatementId())); //$NON-NLS-1$
        answer.addAttribute(new Attribute("parameterType", "map")); //$NON-NLS-1$ //$NON-NLS-2$
        context.getCommentGenerator().addComment(answer);

        StringBuilder sb = new StringBuilder();
        sb.append("update "); //$NON-NLS-1$
        sb.append(introspectedTable.getAliasedFullyQualifiedTableNameAtRuntime());
        answer.addElement(new TextElement(sb.toString()));

        XmlElement dynamicElement = new XmlElement("set"); //$NON-NLS-1$
        answer.addElement(dynamicElement);

        for (IntrospectedColumn introspectedColumn : introspectedTable.getAllColumns()) {
            XmlElement isNotNullElement = new XmlElement("if"); //$NON-NLS-1$
            sb.setLength(0);
            sb.append("@Ognl@isNotEmpty(").append(introspectedColumn.getJavaProperty("record.")).append(")");
            // sb.append(introspectedColumn.getJavaProperty("record.")).append(" != null"); //$NON-NLS-1$

            isNotNullElement.addAttribute(new Attribute("test", sb.toString())); //$NON-NLS-1$
            dynamicElement.addElement(isNotNullElement);

            sb.setLength(0);
            sb.append(MyBatis3FormattingUtilities.getAliasedEscapedColumnName(introspectedColumn));
            sb.append(" = "); //$NON-NLS-1$
            sb.append(MyBatis3FormattingUtilities.getParameterClause(introspectedColumn, "record.")); //$NON-NLS-1$
            sb.append(',');

            isNotNullElement.addElement(new TextElement(sb.toString()));
        }

        answer.addElement(getUpdateByExampleIncludeElement());

        if (context.getPlugins().sqlMapUpdateByExampleSelectiveElementGenerated(answer, introspectedTable)) {
            parentElement.addElement(answer);
        }
    }
}
