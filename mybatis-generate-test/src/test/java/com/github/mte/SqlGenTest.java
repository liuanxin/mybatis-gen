package com.github.mte;

import com.google.common.base.CaseFormat;
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
            String entityName = CaseFormat.UPPER_UNDERSCORE.converterTo(CaseFormat.UPPER_CAMEL)
                    .convert(table.toUpperCase().startsWith("T_") ? table.substring(2) : table);

            System.out.println(String.format(FORMAT, table, entityName));
        }
    }
}
