package com.github.liuanxin.mybatis.plugin;

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
        topLevelClass.addJavaDocLine(" * /");
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
                String columnName = column.getActualColumnName();

                /*
                String columnType = column.getFullyQualifiedJavaType().toString();
                if (Arrays.asList("java.util.Date", "java.sql.Time", "java.sql.Date").contains(columnType)) {
                    String propertyName = underlineToCamel(columnName);
                    f.addJavaDocLine(String.format("/** 为 \"%s\" 提供查询的起始值 * /", columnName));
                    f.addJavaDocLine(String.format("private Date %sStart;", propertyName));
                    f.addJavaDocLine(String.format("/** 为 \"%s\" 提供查询的结束值 * /", columnName));
                    f.addJavaDocLine(String.format("private Date %sEnd;", propertyName));
                }
                */
                if (remarks != null) {
                    remarks = remarks.trim();
                    if (remarks.length() > 0) {
                        // 属性注释(对应的字段注释)
                        f.addJavaDocLine("/** " + remarks + " --> " + columnName + " */");
                    }
                }
            }
        }
    }

    /** 下划线 或 中横线 转 驼峰 */
    private static String underlineToCamel(String param) {
        if (param == null || "".equals(param.trim())) {
            return "";
        }
        int len = param.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = param.charAt(i);
            if (c == '_' || c == '-') {
                i++;
                if (i < len) {
                    sb.append(Character.toUpperCase(param.charAt(i)));
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}

