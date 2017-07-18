package com.github.mte;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import java.util.List;

@ContextConfiguration("/context.xml")
public class SqlGenTest extends AbstractTransactionalJUnit4SpringContextTests {

    private static final String ALL_TABLE = "SHOW TABLES";
    private static final String FORMAT = "\n<table tableName=\"%s\" domainObjectName=\"%s\" escapeWildcards=\"true\"\n"
            + "\t\t enableCountByExample=\"true\" enableUpdateByExample=\"true\" enableDeleteByExample=\"true\"\n"
            + "\t\t enableSelectByExample=\"true\" delimitIdentifiers=\"true\" delimitAllColumns=\"true\"/>";

    @Test
    public void testMybatisCreate() throws Exception {
        List<String> tables = jdbcTemplate.queryForList(ALL_TABLE, String.class);
        for (String table : tables) {
            String tableName = table.toUpperCase().startsWith("T_") ? table.substring(2) : table;
            System.out.println(String.format(FORMAT, table, underlineToCamel(tableName)));
        }
    }

    /** 下划线 或 中横线 转 驼峰 且 首字母大写 返回 */
    private static String underlineToCamel(String param) {
        if (param == null || "".equals(param.trim())) {
            return "";
        }
        int len = param.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = param.charAt(i);
            if (c == '_' || c == '-') {
                if (++i < len) {
                    sb.append(Character.toUpperCase(param.charAt(i)));
                }
            } else {
                sb.append(c);
            }
        }
        String name = sb.toString();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
