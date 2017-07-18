package com.github.liuanxin.mybatis.code.elements;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.XmlElement;

import static org.mybatis.generator.internal.util.StringUtility.stringHasValue;

/** 多加一个 result map 的别名, 供多表时使用 */
public class ResultMapWithBLOBsElementGenerator extends
        org.mybatis.generator.codegen.mybatis3.xmlmapper.elements.AbstractXmlElementGenerator {

    public ResultMapWithBLOBsElementGenerator() {
        super();
    }

    @Override
    public void addElements(XmlElement parentElement) {
        addElement(parentElement, false);
        // 在 resultMap 上加别名(当多表查询时会用到)
        addElement(parentElement, true);
    }

    private void addElement(XmlElement parentElement, boolean isAlias) {
        XmlElement answer = new XmlElement("resultMap"); //$NON-NLS-1$

        answer.addAttribute(new Attribute("id", isAlias ?
                introspectedTable.getResultMapWithBLOBsId() + "_Alias" : introspectedTable.getResultMapWithBLOBsId()));

        String returnType;
        if (introspectedTable.getRules().generateRecordWithBLOBsClass()) {
            returnType = introspectedTable.getRecordWithBLOBsType();
        } else {
            // table has BLOBs, but no BLOB class - BLOB fields must be
            // in the base class
            returnType = introspectedTable.getBaseRecordType();
        }

        answer.addAttribute(new Attribute("type", //$NON-NLS-1$
                returnType));

        if (!introspectedTable.isConstructorBased()) {
            answer.addAttribute(new Attribute("extends", isAlias ?
                    introspectedTable.getBaseResultMapId() + "_Alias" : introspectedTable.getBaseResultMapId()));
        }

        context.getCommentGenerator().addComment(answer);

        if (introspectedTable.isConstructorBased()) {
            if (isAlias) {
                addResultMapConstructorElements(answer, introspectedTable.getTableConfiguration().getTableName());
            } else {
                addResultMapConstructorElements(answer, null);
            }
        } else {
            if (isAlias) {
                addResultMapElements(answer, introspectedTable.getTableConfiguration().getTableName());
            } else {
                addResultMapElements(answer, null);
            }
        }

        if (context.getPlugins()
                .sqlMapResultMapWithBLOBsElementGenerated(answer,
                        introspectedTable)) {
            parentElement.addElement(answer);
        }
    }

    public static String getRenamedColumnNameForResultMap(IntrospectedColumn introspectedColumn, String tableName) {
        if (stringHasValue(introspectedColumn.getTableAlias())) {
            return introspectedColumn.getTableAlias() + '_' + introspectedColumn.getActualColumnName();
        } else if (stringHasValue(tableName)) {
            return tableName + '_' + introspectedColumn.getActualColumnName();
        } else {
            return introspectedColumn.getActualColumnName();
        }
    }

    private void addResultMapElements(XmlElement answer, String tableName) {
        for (IntrospectedColumn introspectedColumn : introspectedTable
                .getBLOBColumns()) {
            XmlElement resultElement = new XmlElement("result"); //$NON-NLS-1$

            resultElement
                    .addAttribute(new Attribute(
                            "column", getRenamedColumnNameForResultMap(introspectedColumn, tableName))); //$NON-NLS-1$
            resultElement.addAttribute(new Attribute(
                    "property", introspectedColumn.getJavaProperty())); //$NON-NLS-1$
            resultElement.addAttribute(new Attribute(
                    "jdbcType", introspectedColumn.getJdbcTypeName())); //$NON-NLS-1$

            if (stringHasValue(introspectedColumn
                    .getTypeHandler())) {
                resultElement.addAttribute(new Attribute(
                        "typeHandler", introspectedColumn.getTypeHandler())); //$NON-NLS-1$
            }

            answer.addElement(resultElement);
        }
    }

    private void addResultMapConstructorElements(XmlElement answer, String tableName) {
        XmlElement constructor = new XmlElement("constructor"); //$NON-NLS-1$

        for (IntrospectedColumn introspectedColumn : introspectedTable
                .getPrimaryKeyColumns()) {
            XmlElement resultElement = new XmlElement("idArg"); //$NON-NLS-1$

            resultElement
                    .addAttribute(new Attribute(
                            "column", getRenamedColumnNameForResultMap(introspectedColumn, tableName))); //$NON-NLS-1$
            resultElement.addAttribute(new Attribute(
                    "jdbcType", introspectedColumn.getJdbcTypeName())); //$NON-NLS-1$
            resultElement.addAttribute(new Attribute("javaType", //$NON-NLS-1$
                    introspectedColumn.getFullyQualifiedJavaType().getFullyQualifiedName()));

            if (stringHasValue(introspectedColumn
                    .getTypeHandler())) {
                resultElement.addAttribute(new Attribute(
                        "typeHandler", introspectedColumn.getTypeHandler())); //$NON-NLS-1$
            }

            constructor.addElement(resultElement);
        }

        for (IntrospectedColumn introspectedColumn : introspectedTable
                .getNonPrimaryKeyColumns()) {
            XmlElement resultElement = new XmlElement("arg"); //$NON-NLS-1$

            resultElement
                    .addAttribute(new Attribute(
                            "column", getRenamedColumnNameForResultMap(introspectedColumn, tableName))); //$NON-NLS-1$
            resultElement.addAttribute(new Attribute(
                    "jdbcType", introspectedColumn.getJdbcTypeName())); //$NON-NLS-1$

            if (introspectedColumn.getFullyQualifiedJavaType().isPrimitive()) {
                // need to use the MyBatis type alias for a primitive byte
                resultElement.addAttribute(new Attribute("javaType", //$NON-NLS-1$
                        '_' + introspectedColumn.getFullyQualifiedJavaType().getShortName()));

            } else if ("byte[]".equals(introspectedColumn.getFullyQualifiedJavaType() //$NON-NLS-1$
                    .getFullyQualifiedName())) {
                // need to use the MyBatis type alias for a primitive byte arry
                resultElement.addAttribute(new Attribute("javaType", //$NON-NLS-1$
                        "_byte[]")); //$NON-NLS-1$
            } else {
                resultElement.addAttribute(new Attribute("javaType", //$NON-NLS-1$
                        introspectedColumn.getFullyQualifiedJavaType().getFullyQualifiedName()));
            }

            if (stringHasValue(introspectedColumn
                    .getTypeHandler())) {
                resultElement.addAttribute(new Attribute(
                        "typeHandler", introspectedColumn.getTypeHandler())); //$NON-NLS-1$
            }

            constructor.addElement(resultElement);
        }

        answer.addElement(constructor);
    }
}
