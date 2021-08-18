package com.github.liuanxin;

import com.google.common.base.Joiner;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import java.util.List;

@ContextConfiguration("/context.xml")
public class GenerateTableTest extends AbstractTransactionalJUnit4SpringContextTests {

    private static final String TABLES = "SHOW TABLES";

    @Test
    public void generateTable() {
        // 输出当前库的所有表
        List<String> tables = jdbcTemplate.queryForList(TABLES, String.class);
        System.out.println("\"" + Joiner.on("\",\n\"").join(tables) + "\"");
    }
}
