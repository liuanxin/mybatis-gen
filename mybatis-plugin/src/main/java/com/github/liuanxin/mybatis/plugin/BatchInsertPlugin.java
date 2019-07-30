package com.github.liuanxin.mybatis.plugin;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.codegen.mybatis3.MyBatis3FormattingUtilities;

import java.util.List;

public class BatchInsertPlugin extends PluginAdapter {

    private static final String METHOD_BATCH_INSERT = "batchInsert";

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    @Override
    public boolean clientGenerated(Interface interfaze, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        Method mBatchInsert = new Method(METHOD_BATCH_INSERT);
        mBatchInsert.setReturnType(FullyQualifiedJavaType.getIntInstance());

        FullyQualifiedJavaType tList = FullyQualifiedJavaType.getNewListInstance();
        tList.addTypeArgument(introspectedTable.getRules().calculateAllFieldsClass());
        mBatchInsert.addParameter(new Parameter(tList, "list", "@Param(\"list\")"));
        interfaze.addMethod(mBatchInsert);
        return true;
    }

    /*
    <insert id="batchInsert" parameterType="map" keyColumn="id" keyProperty="id" useGeneratedKeys="true">
        insert into `t_user` (
        <foreach collection="list" index="index" item="item">
            <if test="index == 0">
                <trim suffixOverrides=",">
                    <if test="item.id != null">
                        `id`,
                    </if>
                    <if test="item.name != null">
                        `name`,
                    </if>
                    <if test="item.password != null">
                        `password`,
                    </if>
                </trim>
            </if>
        </foreach>
        ) values
        <foreach collection="list" item="item" separator=",">
            <trim prefix="(" suffix=")" suffixOverrides=",">
                <if test="item.id != null">
                    #{item.id,jdbcType=BIGINT},
                </if>
                <if test="item.name != null">
                    #{item.name,jdbcType=VARCHAR},
                </if>
                <if test="item.password != null">
                    #{item.password,jdbcType=VARCHAR},
                </if>
            </trim>
        </foreach>
    </insert>
    */
    @Override
    public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
        String table = introspectedTable.getFullyQualifiedTableNameAtRuntime();
        List<IntrospectedColumn> allColumns = introspectedTable.getAllColumns();

        XmlElement element = new XmlElement("insert");
        element.addAttribute(new Attribute("id", METHOD_BATCH_INSERT));
        element.addAttribute(new Attribute("parameterType", "map"));
        element.addAttribute(new Attribute("keyColumn", "id"));
        element.addAttribute(new Attribute("keyProperty", "id"));
        element.addAttribute(new Attribute("useGeneratedKeys", "true"));

        element.addElement(new TextElement("insert into " + table));
        element.addElement(new TextElement("("));

        XmlElement allColumn = new XmlElement("foreach");
        allColumn.addAttribute(new Attribute("collection", "list"));
        allColumn.addAttribute(new Attribute("item", "item"));
        allColumn.addAttribute(new Attribute("index", "index"));

        XmlElement addColumn = new XmlElement("if");
        addColumn.addAttribute(new Attribute("test", "index == 0"));

        XmlElement trimColumn = new XmlElement("trim");
        trimColumn.addAttribute(new Attribute("suffixOverrides", ","));
        for (IntrospectedColumn column : allColumns) {
            XmlElement single = new XmlElement("if");
            single.addAttribute(new Attribute("test", "item." + column.getJavaProperty() + " != null"));
            single.addElement(new TextElement(MyBatis3FormattingUtilities.getEscapedColumnName(column) + ","));
            trimColumn.addElement(single);
        }
        addColumn.addElement(trimColumn);
        allColumn.addElement(addColumn);

        element.addElement(allColumn);
        element.addElement(new TextElement(")"));
        element.addElement(new TextElement("values"));

        // foreach 所有插入的列，比较是否存在
        XmlElement values = new XmlElement("foreach");
        values.addAttribute(new Attribute("collection", "list"));
        values.addAttribute(new Attribute("item", "item"));
        values.addAttribute(new Attribute("separator", ","));

        XmlElement trimValueColumn = new XmlElement("trim");
        trimValueColumn.addAttribute(new Attribute("prefix", "("));
        trimValueColumn.addAttribute(new Attribute("suffix", ")"));
        trimValueColumn.addAttribute(new Attribute("suffixOverrides", ","));
        for (IntrospectedColumn column : allColumns) {
            XmlElement single = new XmlElement("if");
            single.addAttribute(new Attribute("test", "item." + column.getJavaProperty() + " != null"));
            single.addElement(new TextElement(MyBatis3FormattingUtilities.getParameterClause(column, "item.") + ","));
            trimValueColumn.addElement(single);
        }
        values.addElement(trimValueColumn);
        element.addElement(values);

        document.getRootElement().addElement(element);
        return true;
    }
}
