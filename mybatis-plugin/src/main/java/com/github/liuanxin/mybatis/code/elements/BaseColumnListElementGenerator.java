package com.github.liuanxin.mybatis.code.elements;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.codegen.mybatis3.MyBatis3FormattingUtilities;
import org.mybatis.generator.codegen.mybatis3.xmlmapper.elements.AbstractXmlElementGenerator;

import java.util.Iterator;

/** 加一个生成全表名的 sql, 供多表时使用 */
public class BaseColumnListElementGenerator extends AbstractXmlElementGenerator {

    public BaseColumnListElementGenerator() {
        super();
    }

    @Override
    public void addElements(XmlElement parentElement) {
        XmlElement answer = new XmlElement("sql"); //$NON-NLS-1$

        answer.addAttribute(new Attribute("id", //$NON-NLS-1$
                introspectedTable.getBaseColumnListId()));

        context.getCommentGenerator().addComment(answer);

        StringBuilder sb = new StringBuilder();
        Iterator<IntrospectedColumn> iter = introspectedTable.getNonBLOBColumns().iterator();
        while (iter.hasNext()) {
            sb.append(MyBatis3FormattingUtilities.getSelectListPhrase(iter.next()));
            if (iter.hasNext()) {
                sb.append(", "); //$NON-NLS-1$
            }
            if (sb.length() > 60) {
                answer.addElement(new TextElement(sb.toString()));
                sb.setLength(0);
            }
        }
        if (sb.length() > 0) {
            answer.addElement((new TextElement(sb.toString())));
        }
        if (context.getPlugins().sqlMapBaseColumnListElementGenerated(
                answer, introspectedTable)) {
            parentElement.addElement(answer);
        }
        // 在列上加别名(当多表查询时会用到)
        generateAlias(parentElement);
    }

    private void generateAlias(XmlElement parentElement) {
        XmlElement answer = new XmlElement("sql"); //$NON-NLS-1$

        answer.addAttribute(new Attribute("id", //$NON-NLS-1$
                introspectedTable.getBaseColumnListId() + "_Alias"));

        context.getCommentGenerator().addComment(answer);

        StringBuilder sb = new StringBuilder();
        Iterator<IntrospectedColumn> iter = introspectedTable
                .getNonBLOBColumns().iterator();
        while (iter.hasNext()) {
            sb.append(getSelectListPhrase(iter.next()));
            if (iter.hasNext()) {
                sb.append(", "); //$NON-NLS-1$
            }
            if (sb.length() > 60) {
                answer.addElement(new TextElement(sb.toString()));
                sb.setLength(0);
            }
        }
        if (sb.length() > 0) {
            answer.addElement((new TextElement(sb.toString())));
        }
        if (context.getPlugins().sqlMapBaseColumnListElementGenerated(answer, introspectedTable)) {
            parentElement.addElement(answer);
        }
    }


    /**
     * 构建生成全表名的字段信息
     */
    private static String getSelectListPhrase(IntrospectedColumn introspectedColumn) {
        StringBuilder sb = new StringBuilder();
        String tableName = introspectedColumn.getIntrospectedTable().getTableConfiguration().getTableName();
        if (introspectedColumn.isColumnNameDelimited()) {
            sb.append(introspectedColumn.getContext().getBeginningDelimiter());
        }
        sb.append(tableName);
        if (introspectedColumn.isColumnNameDelimited()) {
            sb.append(introspectedColumn.getContext().getEndingDelimiter());
        }
        sb.append('.');
        sb.append(MyBatis3FormattingUtilities.getEscapedColumnName(introspectedColumn));
        sb.append(" as "); //$NON-NLS-1$
        if (introspectedColumn.isColumnNameDelimited()) {
            sb.append(introspectedColumn.getContext().getBeginningDelimiter());
        }
        sb.append(tableName);
        sb.append('_');
        sb.append(MyBatis3FormattingUtilities.escapeStringForMyBatis3(introspectedColumn.getActualColumnName()));
        if (introspectedColumn.isColumnNameDelimited()) {
            sb.append(introspectedColumn.getContext().getEndingDelimiter());
        }
        return sb.toString();
    }
}
