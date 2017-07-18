package com.github.liuanxin.mybatis.plugin;

import com.google.common.base.CaseFormat;
import org.mybatis.generator.api.FullyQualifiedTable;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.TopLevelClass;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 给字段增加注释(从表结构中读取). 目前只针对 mysql */
public class CustomModelCommentPlugin extends PluginAdapter {

    @Override
    public boolean validate(List<String> list) {
        return true;
    }

    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        generateToString(topLevelClass, introspectedTable);
        return true;
    }

    @Override
    public boolean modelRecordWithBLOBsClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        generateToString(topLevelClass, introspectedTable);
        return true;
    }

    @Override
    public boolean modelPrimaryKeyClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        generateToString(topLevelClass, introspectedTable);
        return true;
    }

    private void generateToString(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        FullyQualifiedTable table = introspectedTable.getFullyQualifiedTable();
        // 类注释(对应的表注释)
        String comment = table.getRemarks();
        if (comment == null || comment.trim().length() == 0) comment = "no comment on table";

        /*
        topLevelClass.addJavaDocLine("/**");
        topLevelClass.addJavaDocLine(" * " + comment + " --> " + table.getIntrospectedTableName());
        topLevelClass.addJavaDocLine(" *");
        topLevelClass.addJavaDocLine(" * Generate on " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        topLevelClass.addJavaDocLine(" *//*");
        */
        topLevelClass.addJavaDocLine("/** " + comment + " --> " + table.getIntrospectedTableName() + " */");

        List<Field> fields = topLevelClass.getFields();
        Map<String, Field> map = new HashMap<String, Field>();
        for (Field field : fields) {
            map.put(field.getName(), field);
        }
        List<IntrospectedColumn> columns = introspectedTable.getAllColumns();
        for (IntrospectedColumn column : columns) {
            Field f = map.get(column.getJavaProperty());
            if (f != null) {
                f.getJavaDocLines().clear();
                String remarks = column.getRemarks();
                if (remarks != null) {
                    remarks = remarks.trim();
                    if (remarks.length() > 0) {
                        String columnName = column.getActualColumnName();
                        if ("java.util.Date".equalsIgnoreCase(column.getFullyQualifiedJavaType().toString())) {
                            String propertyName = CaseFormat.UPPER_UNDERSCORE
                                    .converterTo(CaseFormat.LOWER_CAMEL)
                                    .convert(columnName);
                            f.addJavaDocLine(String.format("/** 为 \"%s\" 提供查询的起始值 */", remarks));
                            f.addJavaDocLine(String.format("private Date %sStart;", propertyName));
                            f.addJavaDocLine(String.format("/** 为 \"%s\" 提供查询的结束值 */", remarks));
                            f.addJavaDocLine(String.format("private Date %sEnd;", propertyName));
                        }
                        // 属性注释(对应的字段注释)
                        f.addJavaDocLine("/** " + remarks + " --> " + columnName + " */");
                    }
                }
            }
        }
    }

}

