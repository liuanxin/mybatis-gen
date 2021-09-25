package com.github.liuanxin;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

@SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored", "SqlNoDataSourceInspection"})
@ContextConfiguration("/context.xml")
public class ClassGenerateTest2 extends AbstractTransactionalJUnit4SpringContextTests {

    /** 文件生成目录 */
    private static final String SAVE_PATH = "/home/xx/temp/";
    /** 生成 java 文件的目录 */
    private static final String JAVA_PATH = SAVE_PATH + "java/";
    /** 生成 xml 文件的目录 */
    private static final String XML_PATH = SAVE_PATH + "resources/mapper/";

    /** 全局包 */
    private static final String PACKAGE = "com.yicheng";
    /** 生成 java 文件(dao、entity、req、res、service)的包名 */
    private static final String PROJECT_PACKAGE = PACKAGE + ".xxx";

    /** 要生成代码的表名 */
    private static final Set<String> GENERATE_TABLES = Sets.newHashSet(Arrays.asList(
            "t_common",
            "t_user"
    ));

    private static final String REQ_PACKAGE = PROJECT_PACKAGE + ".req"; // 带 @ApiParam 注解
    private static final String RES_PACKAGE = PROJECT_PACKAGE + ".res"; // 带 @ApiReturn 注解
    private static final String MODEL_PACKAGE = PROJECT_PACKAGE + ".model"; // 只有表和字段注释
    private static final String DAO_PACKAGE = PROJECT_PACKAGE + ".repository";
    private static final String SERVICE_PACKAGE = PROJECT_PACKAGE + ".service";

    private static final String MODEL_SUFFIX = "";
    private static final String DAO_SUFFIX = "Mapper";
    private static final String SERVICE_SUFFIX = "Service";

    /**
     * 0. 使用 VALUES, 1. 使用 new, 2. 使用 VALUE
     *
     * INSERT INTO t_xx(c1, c2) VALUES('1', '2') ON DUPLICATE KEY UPDATE c1 = VALUES(c1), c2 = VALUES(c2)
     * 使用 VALUES(c1) 表示使用新值, 8.0.20 之后 VALUES 不再推荐使用, 从 8.0.19 开始推荐下面的方式
     * INSERT INTO t_xx(c1, c2) VALUES('1', '2') AS new ON DUPLICATE KEY UPDATE c1 = new.c1, c2 = new.c2
     *
     * 如果是用 MariaDB 且版本 <= 10.3.2 也同样使用 VALUES, 版本 >= 10.3.3 则使用 VALUE
     * 见: https://dev.mysql.com/doc/refman/8.0/en/insert-on-duplicate.html
     * 见: https://mariadb.com/kb/en/values-value/
     */
    private static final int DUPLICATE_TYPE = 0;

    /** true 收集删表语句 */
    private static final boolean COLLECT_DROP_TABLE = false;
    /** true 表示收集所有表的 sql */
    private static final boolean COLLECT_ALL_SQL = true;

    /** true 表示收集所有的数据字典 */
    private static final boolean COLLECT_ALL_DB_DICT = false;

    /** 是否把 tinyint(1) 映射成 Boolean */
    private static final boolean TINYINT1_TO_BOOLEAN = true;

    /** true 表示生成自定义的 xml 文件 */
    private static final boolean GENERATE_XML = true;

    // 上面是配置项, 下面的不用了

    private static final String DB = "SELECT DATABASE()";

    private static final String ALL_TABLE = "SELECT TABLE_NAME tn, TABLE_COMMENT tc FROM information_schema.`TABLES` WHERE table_schema = ?";

    private static final String ALL_COLUMN = "SELECT column_name cn, column_type ct, column_comment cc, column_default cd, " +
            "is_nullable ie, extra, column_key ck FROM information_schema.`COLUMNS` WHERE table_schema = ? AND table_name = ? " +
            "ORDER BY ordinal_position";

    private static final String CREATE_SQL = "SHOW CREATE TABLE %s";

    private static final Map<String, String> TYPE_MAP = maps(
            "tinyint(1)", "Boolean",
            "tinyint", "Boolean",
            "smallint", "Integer",
            "int", "Integer",
            "bigint", "Long",
            "text", "String",
            "longtext", "String",
            "varchar", "String",
            "char", "String",
            "datetime", "Date",
            "timestamp", "Date",
            "date", "Date",
            "time", "Date",
            "decimal", "BigDecimal"
    );
    private static final Map<String, String> TYPE_DB_MAP = maps(
            "tinyint", "TINYINT",
            "smallint", "SMALLINT",
            "bigint", "BIGINT",
            "int", "INTEGER",
            "text", "LONGVARCHAR",
            "longtext", "LONGVARCHAR",
            "varchar", "VARCHAR",
            "char", "VARCHAR",
            "datetime", "TIMESTAMP",
            "timestamp", "TIMESTAMP",
            "date", "TIMESTAMP",
            "time", "TIMESTAMP",
            "decimal", "DECIMAL"
    );
    private static final String TABLE_NAME = "tn";
    private static final String TABLE_COMMENT = "tc";
    private static final String COLUMN_NAME = "cn";
    private static final String COLUMN_TYPE = "ct";
    private static final String COLUMN_COMMENT = "cc";
    private static final String COLUMN_DEFAULT = "cd";
    private static final String EXTRA = "extra";
    private static final String IS_NULLABLE = "ie";
    private static final String COLUMN_KEY = "ck";

    private static final int WRAP_COUNT = 5;
    private static final int ALIAS_WRAP_COUNT = 2;

    @Test
    public void generateClass() {
        deleteDirectory(new File(SAVE_PATH));
        String dbName = jdbcTemplate.queryForObject(DB, String.class);
        List<Map<String, Object>> tables = jdbcTemplate.queryForList(ALL_TABLE, dbName);
        StringBuilder dbSbd = new StringBuilder();
        StringBuilder mdSbd = new StringBuilder();
        System.out.println("========================================");
        List<String> tableList = Lists.newArrayList();
        for (Map<String, Object> table : tables) {
            String tableName = toStr(table.get(TABLE_NAME));
            tableList.add(tableName);
            String tableComment = toStr(table.get(TABLE_COMMENT));
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(ALL_COLUMN, dbName, tableName);

            Map<String, Object> sqlMap = jdbcTemplate.queryForMap(String.format(CREATE_SQL, tableName));
            String createSql = toStr(sqlMap.get("Create Table"))
                    .replace("CREATE TABLE ", "CREATE TABLE IF NOT EXISTS ")
                    .replace(" DEFAULT NULL ", " ")
                    .replace(" USING BTREE", "")
                    .replaceFirst(" CHARACTER SET utf8(.*?)([ ])", "$2")
                    .replaceFirst(" DEFAULT CHARSET=utf8(.*?)([; ])", " DEFAULT CHARSET=utf8mb4$2")
                    .replaceFirst(" COLLATE=utf8(.*?)([; ])", "$2")
                    .replaceFirst(" ROW_FORMAT=DYNAMIC([ ])", "$2")
                    .replaceFirst(" AUTO_INCREMENT=([0-9]*?)([ ])", "$2")
                    .replaceAll(" COLLATE utf8(.*?)([ ])", "$2")
                    + ";\n\n\n";

            String dbDict = generateDbDict(tableName, tableComment, columns);
            if (!GENERATE_TABLES.isEmpty() && GENERATE_TABLES.contains(tableName)) {
                System.out.printf("%s : %s\n", tableName, tableComment);
                if (COLLECT_DROP_TABLE) {
                    dbSbd.append("DROP TABLE IF EXISTS `").append(tableName).append("`").append(";\n");
                }
                dbSbd.append(createSql);
                mdSbd.append(dbDict);

                req(tableName, columns);
                res(tableName, columns);
                service(tableName);
                model(tableName, tableComment, columns);
                dao(tableName, tableComment);
                if (GENERATE_XML) {
                    xml(tableName, columns);
                }
                System.out.println("========================================");
            } else {
                if (COLLECT_ALL_SQL) {
                    if (COLLECT_DROP_TABLE) {
                        dbSbd.append("DROP TABLE IF EXISTS `").append(tableName).append("`").append(";\n");
                    }
                    dbSbd.append(createSql);
                }
                if (COLLECT_ALL_DB_DICT) {
                    mdSbd.append(dbDict);
                }
            }
        }
        System.out.println("\"" + Joiner.on("\",\n\"").join(tableList) + "\"");
        System.out.println("========================================");
        if (dbSbd.length() > 0 || mdSbd.length() > 0) {
            if (dbSbd.length() > 0) {
                writeFile(new File(SAVE_PATH + dbName + ".sql"), "\n" + dbSbd.toString().trim() + "\n");
            }
            if (mdSbd.length() > 0) {
                writeFile(new File(SAVE_PATH + "db-dict.md"), "\n" + mdSbd.insert(0, "### 数据库字典\n\n").toString().trim() + "\n\n-----\n");
            }
            System.out.println("========================================");
        }
    }

    private static void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
            boolean flag = dir.delete();
            if (!flag) {
                throw new RuntimeException(String.format("文件(%s)删除失败", dir));
            }
        }
    }

    private static void writeFile(File file, String content) {
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        if (file.exists()) {
            file.delete();
        }
        try {
            Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
            System.out.println("生成: " + file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String generateDbDict(String tableName, String tableComment, List<Map<String, Object>> columns) {
        StringBuilder sbd = new StringBuilder();
        sbd.append("-----\n\n#### ").append(tableName);
        if (tableComment != null && !tableComment.trim().isEmpty()) {
            sbd.append("(`").append(tableComment).append("`)");
        }
        sbd.append("\n\n");
        sbd.append("| 序号 | 字段名 | 字段类型 | 是否可空 | 默认值 | 字段说明 |\n");
        sbd.append("| :--- | :---- | :------ | :------ | :----- | :------ |\n");
        for (int i = 0; i < columns.size(); i++) {
            Map<String, Object> column = columns.get(i);
            String columnName = toStr(column.get(COLUMN_NAME));
            String columnType = toStr(column.get(COLUMN_TYPE));
            String isNullable = toStr(column.get(IS_NULLABLE));
            Object cd = column.get(COLUMN_DEFAULT);
            Object extra = column.get(EXTRA);
            String columnDefault;
            if (cd == null) {
                if (extra != null && !extra.toString().isEmpty()) {
                    columnDefault = extra.toString().replace("on update ", "ON UPDATE ");
                } else {
                    columnDefault = "NULL";
                }
            } else {
                if (extra != null && !extra.toString().isEmpty()) {
                    columnDefault = cd + " " + extra.toString().replace("on update ", "ON UPDATE ");
                } else {
                    columnDefault = cd.toString();
                }
            }

            String columnComment = toStr(column.get(COLUMN_COMMENT));
            String comment;
            if ("pri".equalsIgnoreCase(toStr(column.get(COLUMN_KEY)))) {
                if (columnComment.contains("主键")) {
                    comment = columnComment;
                } else {
                    comment = "主键" + ("".equals(columnComment) ? "" : String.format("(%s)", columnComment));
                }
            } else {
                comment = columnComment;
            }
            sbd.append("| ").append(i + 1)
                    .append(" | ").append(columnName)
                    .append(" | ").append(columnType)
                    .append(" | ").append("yes".equalsIgnoreCase(isNullable) ? "✔" : "✘")
                    .append(" | ").append(columnDefault.replace(" DEFAULT_GENERATED", ""))
                    .append(" | ").append(comment).append(" |\n");
        }
        return sbd.append("\n").toString();
    }

    private static final String REQ_RES = "package %s;\n" +
            "\n" +
            "%s\n" +
            "%s" +
            "\n" +
            "@Data\n" +
            "public class %s implements Serializable {\n" +
            "    private static final long serialVersionUID = 1L;\n" +
            "%s" +
            "}\n";
    private static String reqAndRes(String classPackage, String tableName, List<Map<String, Object>> columns, boolean req) {
        Set<String> importSet = Sets.newHashSet("import lombok.Data;\n");
        Set<String> javaImportSet = Sets.newHashSet("import java.io.Serializable;\n");
        StringBuilder sbd = new StringBuilder();
        for (Map<String, Object> column : columns) {
            String columnName = toStr(column.get(COLUMN_NAME));
            String columnType = toStr(column.get(COLUMN_TYPE));
            if (columnType.contains(" ")) {
                columnType = columnType.substring(0, columnType.indexOf(" "));
            }
            if (!(TINYINT1_TO_BOOLEAN && "tinyint(1)".equalsIgnoreCase(columnType))) {
                columnType = (columnType.contains("(") ? columnType.substring(0, columnType.indexOf("(")) : columnType);
            }
            String columnComment = toStr(column.get(COLUMN_COMMENT));

            sbd.append("\n");
            String fieldName = toField(columnName);
            if (!"".equals(columnComment)) {
                if (req) {
                    sbd.append(tab(1)).append("@ApiParam(\"").append(replaceQuote(columnComment)).append("\")\n");
                    importSet.add("import com.github.liuanxin.api.annotation.ApiParam;\n");
                } else {
                    sbd.append(tab(1)).append("@ApiReturn(\"").append(replaceQuote(columnComment)).append("\")\n");
                    importSet.add("import com.github.liuanxin.api.annotation.ApiReturn;\n");
                }
            }
            String fieldType = TYPE_MAP.get(columnType.toLowerCase());
            if (fieldType == null) {
                throw new RuntimeException(String.format("column-type(%s) has no field mapping", columnType));
            }
            sbd.append(tab(1)).append("private ").append(fieldType).append(" ").append(fieldName).append(";\n");
            if ("Date".equals(fieldType)) {
                javaImportSet.add("import java.util.Date;\n");
            } else if ("BigDecimal".equals(fieldType)) {
                javaImportSet.add("import java.math.BigDecimal;\n");
            }
        }

        List<String> noJavaList = Lists.newArrayList(importSet);
        Collections.sort(noJavaList);
        String noJavaJoin = Joiner.on("").join(noJavaList);

        List<String> javaList = Lists.newArrayList(javaImportSet);
        Collections.sort(javaList);
        String javaJoin = Joiner.on("").join(javaList);
        return String.format(REQ_RES, classPackage, noJavaJoin, javaJoin, toClass(tableName), sbd);
    }
    private static void req(String tableName, List<Map<String, Object>> columns) {
        tableName = tableName.toUpperCase().startsWith("T_") ? tableName.substring(2) : tableName;
        String content = reqAndRes(REQ_PACKAGE, tableName + "_req", columns, true);
        writeFile(new File(JAVA_PATH + REQ_PACKAGE.replace(".", "/"), toClass(tableName + "_req") + ".java"), content);
    }
    private static void res(String tableName, List<Map<String, Object>> columns) {
        tableName = tableName.toUpperCase().startsWith("T_") ? tableName.substring(2) : tableName;
        String content = reqAndRes(RES_PACKAGE, tableName + "_res", columns, false);
        writeFile(new File(JAVA_PATH + RES_PACKAGE.replace(".", "/"), toClass(tableName + "_res") + ".java"), content);
    }

    private static final String MODEL = "package %s;\n" +
            "\n" +
            "%s\n" +
            "%s" +
            "\n" +
            "/** %s */\n" +
            "@Data\n" +
            "@TableName(\"%s\")\n" +
            "public class %s implements Serializable {\n" +
            "    private static final long serialVersionUID = 1L;\n" +
            "%s" +
            "}\n";
    private static void model(String tableName, String tableComment, List<Map<String, Object>> columns) {
        Set<String> importSet = Sets.newHashSet("import com.baomidou.mybatisplus.annotation.TableName;\n", "import lombok.Data;\n");
        Set<String> javaImportSet = Sets.newHashSet("import java.io.Serializable;\n");
        StringBuilder sbd = new StringBuilder();
        for (Map<String, Object> column : columns) {
            String columnName = toStr(column.get(COLUMN_NAME));
            String columnType = toStr(column.get(COLUMN_TYPE));
            if (columnType.contains(" ")) {
                columnType = columnType.substring(0, columnType.indexOf(" "));
            }
            if (!(TINYINT1_TO_BOOLEAN && "tinyint(1)".equalsIgnoreCase(columnType))) {
                columnType = (columnType.contains("(") ? columnType.substring(0, columnType.indexOf("(")) : columnType);
            }
            String columnComment = toStr(column.get(COLUMN_COMMENT));
            columnComment = ("".equals(columnComment) ? "" : (" " + columnComment + " -->"));

            sbd.append("\n");
            String fieldName = toField(columnName);
            if (columnName.equals(fieldName)) {
                if (!"".equals(columnComment)) {
                    sbd.append(tab(1)).append(String.format("/**%s %s */\n", columnComment, columnName));
                }
            } else {
                if ("".equals(columnComment)) {
                    sbd.append(tab(1)).append(String.format("/** %s */\n", columnName));
                } else {
                    sbd.append(tab(1)).append(String.format("/**%s %s */\n", columnComment, columnName));
                }
            }
            String fieldType = TYPE_MAP.get(columnType.toLowerCase());
            if (fieldType == null) {
                throw new RuntimeException(String.format("column-type(%s) has no field mapping", columnType));
            }
            switch (fieldName) {
                case "id":
                    importSet.add("import com.baomidou.mybatisplus.annotation.TableId;\n");
                    sbd.append(tab(1)).append("@TableId\n");
                    break;
                case "createTime":
                    importSet.add("import com.baomidou.mybatisplus.annotation.FieldFill;\n");
                    importSet.add("import com.baomidou.mybatisplus.annotation.TableField;\n");
                    sbd.append(tab(1)).append("@TableField(fill = FieldFill.INSERT)\n");
                    break;
                case "updateTime":
                    importSet.add("import com.baomidou.mybatisplus.annotation.FieldFill;\n");
                    importSet.add("import com.baomidou.mybatisplus.annotation.TableField;\n");
                    sbd.append(tab(1)).append("@TableField(fill = FieldFill.INSERT_UPDATE)\n");
                    break;
                case "deleted":
                case "isDelete":
                case "isDeleted":
                case "deleteFlag":
                case "delFlag":
                    importSet.add("import com.baomidou.mybatisplus.annotation.TableLogic;\n");
                    if (columnType.equalsIgnoreCase("int") || columnType.equalsIgnoreCase("bigint")) {
                        sbd.append(tab(1)).append("@TableLogic(value = \"0\", delval = \"UNIX_TIMESTAMP()\")\n");
                    } else {
                        sbd.append(tab(1)).append("@TableLogic(value = \"0\", delval = \"1\")\n");
                    }
                    break;
            }
            sbd.append(tab(1)).append(String.format("private %s %s;\n", fieldType, fieldName));
            if ("Date".equals(fieldType)) {
                javaImportSet.add("import java.util.Date;\n");
            } else if ("BigDecimal".equals(fieldType)) {
                javaImportSet.add("import java.math.BigDecimal;\n");
            }
        }

        List<String> noJavaList = Lists.newArrayList(importSet);
        Collections.sort(noJavaList);
        String noJavaJoin = Joiner.on("").join(noJavaList);

        List<String> javaList = Lists.newArrayList(javaImportSet);
        Collections.sort(javaList);
        String javaJoin = Joiner.on("").join(javaList);
        String handleTableName = tableName.toUpperCase().startsWith("T_") ? tableName.substring(2) : tableName;
        String modelClass = toClass(handleTableName) + MODEL_SUFFIX;
        String comment = (tableComment != null && !tableComment.isEmpty()) ? (tableComment + " --> " + tableName) : tableName;
        String content = String.format(MODEL, MODEL_PACKAGE, noJavaJoin, javaJoin, comment, tableName, modelClass, sbd);
        writeFile(new File(JAVA_PATH + MODEL_PACKAGE.replace(".", "/"), modelClass + ".java"), content);
    }

    private static final String DAO = "package %s;\n" +
            "\n" +
            "import com.baomidou.mybatisplus.core.mapper.BaseMapper;\n" +
            "import %s;\n" +
            "import org.apache.ibatis.annotations.Mapper;\n" +
            "import org.apache.ibatis.annotations.Param;\n" +
            "\n" +
            "import java.util.List;\n" +
            "\n" +
            "/** %s */\n" +
            "@Mapper\n" +
            "public interface %s extends BaseMapper<%s> {\n" +
            (GENERATE_XML ? (
                    "\n" +
                            "    int insertOrUpdate(%s record);\n" +
                            "\n" +
                            "    int batchDynamicInsertOrUpdate(@Param(\"list\") List<%s> list);\n" +
                            "\n" +
                            "    int batchInsertOrUpdate(@Param(\"list\") List<%s> list);\n"
            ) : "") +
            "}\n";
    private static void dao(String tableName, String tableComment) {
        String handleTableName = tableName.toUpperCase().startsWith("T_") ? tableName.substring(2) : tableName;
        String daoClassName = toClass(handleTableName) + DAO_SUFFIX;
        String modelClassName = toClass(handleTableName) + MODEL_SUFFIX;
        String modelClassPath = tableToModel(handleTableName);
        String comment = (tableComment != null && !tableComment.isEmpty()) ? (tableComment + " --> " + tableName) : tableName;
        String content = GENERATE_XML
                ? String.format(DAO, DAO_PACKAGE, modelClassPath, comment, daoClassName, modelClassName, modelClassName, modelClassName , modelClassName)
                : String.format(DAO, DAO_PACKAGE, modelClassPath, comment, daoClassName, modelClassName);
        writeFile(new File(JAVA_PATH + DAO_PACKAGE.replace(".", "/"), daoClassName + ".java"), content);
    }

    private static void xml(String tableName, List<Map<String, Object>> columns) {
        String handleTableName = tableName.toUpperCase().startsWith("T_") ? tableName.substring(2) : tableName;
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n" +
                String.format("<mapper namespace=\"%s\">\n", tableToDao(handleTableName)) +
                xmlMap(handleTableName, false, columns) + "\n" +
                xmlMap(handleTableName, true, columns) + "\n" +
                "\n" +
                xmlSql(handleTableName, false, columns) + "\n" +
                xmlSql(handleTableName, true, columns) + "\n" +
                "\n" +
                xmlInsertOrUpdate(tableName, columns) + "\n" +
                "\n" +
                xmlBatchDynamicInsertOrUpdate(tableName, columns) + "\n" +
                "\n" +
                xmlBatchInsertOrUpdate(tableName, columns) + "\n" +
                "</mapper>\n";
        writeFile(new File(XML_PATH, toClass(handleTableName) + ".xml"), content);
    }
    private static String xmlMap(String tableName, boolean alias, List<Map<String, Object>> columns) {
        StringBuilder sbd = new StringBuilder();
        sbd.append(String.format("%s<resultMap id=\"%s\" type=\"%s\">\n",
                tab(1), tableToResultMap(tableName, alias), tableToModel(tableName)));
        for (Map<String, Object> column : columns) {
            String columnName = toStr(column.get(COLUMN_NAME));
            String columnType = toStr(column.get(COLUMN_TYPE));
            if (columnType.contains(" ")) {
                columnType = columnType.substring(0, columnType.indexOf(" "));
            }
            columnType = (columnType.contains("(") ? columnType.substring(0, columnType.indexOf("(")) : columnType);

            String jdbcType = TYPE_DB_MAP.get(columnType.toLowerCase());
            if (jdbcType == null) {
                throw new RuntimeException(String.format("column-type(%s) has no jdbc mapping", columnType));
            }

            sbd.append(String.format("%s<%s column=\"%s\" jdbcType=\"%s\" property=\"%s\" />\n",
                    tab(2), ("id".equals(columnName) ? "id" : "result"),
                    toColumn(tableName, columnName, alias), jdbcType, toField(columnName)));
        }
        sbd.append(String.format("%s</resultMap>", tab(1)));
        return sbd.toString();
    }
    private static String xmlSql(String tableName, boolean alias, List<Map<String, Object>> columns) {
        StringBuilder sbd = new StringBuilder();
        sbd.append(String.format("%s<sql id=\"%s\">\n%s", tab(1), tableToSql(tableName, alias), tab(2)));
        StringBuilder columnBuilder = new StringBuilder();
        int count = 0;
        for (int i = 0; i < columns.size(); i++) {
            Map<String, Object> column = columns.get(i);

            String columnName = toStr(column.get(COLUMN_NAME));
            columnBuilder.append(toColumnName(tableName, columnName, alias));
            count++;
            if ((i + 1) != columns.size()) {
                columnBuilder.append(", ");
            }
            if (count % (alias ? ALIAS_WRAP_COUNT : WRAP_COUNT) == 0 && !columnBuilder.toString().endsWith("`")) {
                columnBuilder.delete(columnBuilder.length() - 1, columnBuilder.length()).append("\n").append(tab(2));
                count = 0;
            }
        }
        return sbd.append(columnBuilder.toString().trim()).append("\n").append(tab(1)).append("</sql>").toString();
    }
    private static String xmlInsertOrUpdate(String tableName, List<Map<String, Object>> columns) {
        String handleTableName = tableName.toUpperCase().startsWith("T_") ? tableName.substring(2) : tableName;
        StringBuilder sbd = new StringBuilder();
        sbd.append(tab(1)).append("<insert id=\"insertOrUpdate\" parameterType=\"");
        sbd.append(tableToModel(handleTableName)).append("\"\n");
        sbd.append(tab(3)).append("keyColumn=\"id\" keyProperty=\"id\" useGeneratedKeys=\"true\">\n");
        sbd.append(tab(2)).append(String.format("INSERT INTO `%s`\n", tableName));

        sbd.append(tab(2)).append("<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">\n");
        for (Map<String, Object> column : columns) {
            String columnName = toStr(column.get(COLUMN_NAME));
            sbd.append(tab(3)).append(String.format("<if test=\"%s != null\">\n", toField(columnName)));
            sbd.append(tab(4)).append(String.format("`%s`,\n", toColumn(null, columnName, false)));
            sbd.append(tab(3)).append("</if>\n");
        }
        sbd.append(tab(2)).append("</trim>\n");

        sbd.append(tab(2)).append("<trim prefix=\"VALUES (\" suffix=\")\" suffixOverrides=\",\">\n");
        for (Map<String, Object> column : columns) {
            String columnName = toStr(column.get(COLUMN_NAME));
            String columnType = toStr(column.get(COLUMN_TYPE));
            if (columnType.contains(" ")) {
                columnType = columnType.substring(0, columnType.indexOf(" "));
            }
            columnType = (columnType.contains("(") ? columnType.substring(0, columnType.indexOf("(")) : columnType);

            sbd.append(tab(3)).append(String.format("<if test=\"%s != null\">\n", toField(columnName)));
            String jdbcType = TYPE_DB_MAP.get(columnType.toLowerCase());
            if (jdbcType == null) {
                throw new RuntimeException(String.format("column-type(%s) has no jdbc mapping", columnType));
            }
            sbd.append(tab(4)).append(String.format("#{%s,jdbcType=%s},\n", toField(columnName), jdbcType));
            sbd.append(tab(3)).append("</if>\n");
        }
        sbd.append(tab(2)).append("</trim>\n");

        String duplicate = (DUPLICATE_TYPE == 1) ? "AS new ON DUPLICATE KEY UPDATE" : "ON DUPLICATE KEY UPDATE";
        sbd.append(tab(2)).append(String.format("<trim prefix=\"%s\" suffixOverrides=\",\">\n", duplicate));
        for (Map<String, Object> column : columns) {
            String columnName = toStr(column.get(COLUMN_NAME));
            sbd.append(tab(3)).append(String.format("<if test=\"%s != null\">\n", toField(columnName)));
            String toColumn = toColumn(null, columnName, false);
            // 0. 使用 VALUES, 1. 使用 new, 2. 使用 VALUE
            String values;
            if (DUPLICATE_TYPE == 1) {
                values = String.format("new.`%s`", toColumn);
            } else if (DUPLICATE_TYPE == 2) {
                values = String.format("VALUE(`%s`)", toColumn);
            } else {
                values = String.format("VALUES(`%s`)", toColumn);
            }
            sbd.append(tab(4)).append(String.format("`%s` = %s,\n", toColumn, values));
            sbd.append(tab(3)).append("</if>\n");
        }
        sbd.append(tab(2)).append("</trim>\n");

        sbd.append(tab(1)).append("</insert>");
        return sbd.toString();
    }
    private static String xmlBatchDynamicInsertOrUpdate(String tableName, List<Map<String, Object>> columns) {
        StringBuilder sbd = new StringBuilder();
        sbd.append(tab(1)).append("<insert id=\"batchDynamicInsertOrUpdate\" parameterType=\"map\"" +
                " keyColumn=\"id\" keyProperty=\"id\" useGeneratedKeys=\"true\">\n");
        sbd.append(tab(2)).append(String.format("INSERT INTO `%s`\n", tableName));
        sbd.append(tab(2)).append("<foreach collection=\"list\" index=\"index\" item=\"item\" separator=\",\">\n");
        sbd.append(tab(3)).append("<if test=\"index == 0\">\n");
        sbd.append(tab(4)).append("<trim prefix=\"(\" suffix=\") VALUES\" suffixOverrides=\",\">\n");
        for (Map<String, Object> column : columns) {
            String columnName = toStr(column.get(COLUMN_NAME));

            sbd.append(tab(5)).append(String.format("<if test=\"item.%s != null\">\n", toField(columnName)));
            sbd.append(tab(6)).append(String.format("`%s`,\n", toColumn(null, columnName, false)));
            sbd.append(tab(5)).append("</if>\n");
        }
        sbd.append(tab(4)).append("</trim>\n");
        sbd.append(tab(3)).append("</if>\n");
        sbd.append("\n");
        sbd.append(tab(3)).append("<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">\n");
        for (Map<String, Object> column : columns) {
            String columnName = toStr(column.get(COLUMN_NAME));
            String columnType = toStr(column.get(COLUMN_TYPE));
            if (columnType.contains(" ")) {
                columnType = columnType.substring(0, columnType.indexOf(" "));
            }
            columnType = (columnType.contains("(") ? columnType.substring(0, columnType.indexOf("(")) : columnType);

            sbd.append(tab(4)).append(String.format("<if test=\"item.%s != null\">\n", toField(columnName)));
            String jdbcType = TYPE_DB_MAP.get(columnType.toLowerCase());
            if (jdbcType == null) {
                throw new RuntimeException(String.format("column-type(%s) has no jdbc mapping", columnType));
            }
            sbd.append(tab(5)).append(String.format("#{item.%s,jdbcType=%s},\n", toField(columnName), jdbcType));
            sbd.append(tab(4)).append("</if>\n");
        }
        sbd.append(tab(3)).append("</trim>\n");
        sbd.append("\n");
        String duplicate = (DUPLICATE_TYPE == 1) ? "AS new ON DUPLICATE KEY UPDATE" : "ON DUPLICATE KEY UPDATE";
        sbd.append(tab(3)).append("<if test=\"(index + 1) == list.size\">\n");
        sbd.append(tab(4)).append(String.format("<trim prefix=\"%s\" suffixOverrides=\",\">\n", duplicate));
        for (Map<String, Object> column : columns) {
            String columnName = toStr(column.get(COLUMN_NAME));
            sbd.append(tab(5)).append(String.format("<if test=\"item.%s != null\">\n", toField(columnName)));
            String toColumn = toColumn(null, columnName, false);
            // 0. 使用 VALUES, 1. 使用 new, 2. 使用 VALUE
            String values;
            if (DUPLICATE_TYPE == 1) {
                values = String.format("new.`%s`", toColumn);
            } else if (DUPLICATE_TYPE == 2) {
                values = String.format("VALUE(`%s`)", toColumn);
            } else {
                values = String.format("VALUES(`%s`)", toColumn);
            }
            sbd.append(tab(6)).append(String.format("`%s` = %s,\n", toColumn, values));
            sbd.append(tab(5)).append("</if>\n");
        }
        sbd.append(tab(4)).append("</trim>\n");
        sbd.append(tab(3)).append("</if>\n");

        sbd.append(tab(2)).append("</foreach>\n");
        sbd.append(tab(1)).append("</insert>");
        return sbd.toString();
    }
    private static String xmlBatchInsertOrUpdate(String tableName, List<Map<String, Object>> columns) {
        StringBuilder sbd = new StringBuilder();
        sbd.append(tab(1)).append("<insert id=\"batchInsertOrUpdate\" parameterType=\"map\">\n");
        sbd.append(tab(2)).append(String.format("INSERT INTO `%s` (\n", tableName));
        for (int i = 0; i < columns.size(); i++) {
            Map<String, Object> column = columns.get(i);
            sbd.append(tab(3)).append(String.format("`%s`", toColumn(null, toStr(column.get(COLUMN_NAME)), false)));
            if (i < columns.size() - 1) {
                sbd.append(",");
            }
            sbd.append("\n");
        }
        sbd.append(tab(2)).append(")\n");

        sbd.append(tab(2)).append("<foreach collection=\"list\" item=\"item\" open=\"(\" separator=\"),(\" close=\")\">\n");
        for (int i = 0; i < columns.size(); i++) {
            Map<String, Object> column = columns.get(i);
            String columnType = toStr(column.get(COLUMN_TYPE));
            if (columnType.contains(" ")) {
                columnType = columnType.substring(0, columnType.indexOf(" "));
            }
            columnType = (columnType.contains("(") ? columnType.substring(0, columnType.indexOf("(")) : columnType);
            String jdbcType = TYPE_DB_MAP.get(columnType.toLowerCase());
            if (jdbcType == null) {
                throw new RuntimeException(String.format("column-type(%s) has no jdbc mapping", columnType));
            }
            String columnName = toStr(column.get(COLUMN_NAME));
            sbd.append(tab(3)).append(String.format("#{item.%s,jdbcType=%s}", toField(columnName), jdbcType));
            if (i < columns.size() - 1) {
                sbd.append(",");
            }
            sbd.append("\n");
        }
        sbd.append(tab(2)).append("</foreach>\n");

        String duplicate = (DUPLICATE_TYPE == 1) ? "AS new ON DUPLICATE KEY UPDATE" : "ON DUPLICATE KEY UPDATE";
        sbd.append(tab(2)).append(duplicate).append("\n");
        for (int i = 0; i < columns.size(); i++) {
            Map<String, Object> column = columns.get(i);
            String columnName = toStr(column.get(COLUMN_NAME));
            String toColumn = toColumn(null, columnName, false);
            // 0. 使用 VALUES, 1. 使用 new, 2. 使用 VALUE
            String values;
            if (DUPLICATE_TYPE == 1) {
                values = String.format("new.`%s`", toColumn);
            } else if (DUPLICATE_TYPE == 2) {
                values = String.format("VALUE(`%s`)", toColumn);
            } else {
                values = String.format("VALUES(`%s`)", toColumn);
            }
            sbd.append(tab(3)).append(String.format("`%s` = %s", toColumn, values));
            if (i < columns.size() - 1) {
                sbd.append(",");
            }
            sbd.append("\n");
        }
        sbd.append(tab(1)).append("</insert>");
        return sbd.toString();
    }

    private static final String SERVICE = "package %s;\n" +
            "\n" +
            "import com.baomidou.mybatisplus.core.toolkit.IdWorker;\n" +
            "import %s;\n" +
            "import %s;\n" +
            "import lombok.RequiredArgsConstructor;\n" +
            "import lombok.extern.slf4j.Slf4j;\n" +
            "import org.springframework.stereotype.Service;\n" +
            "import org.springframework.transaction.annotation.Transactional;\n" +
            "\n" +
            "import java.util.Collections;\n" +
            "import java.util.List;\n" +
            "\n" +
            "@Slf4j\n" +
            "@RequiredArgsConstructor\n" +
            "@Service\n" +
            "public class %s {\n" +
            "\n" +
            "    private final %s $$var$$;\n" +
            "\n" +
            "    @Transactional\n" +
            "    public int add($$entity$$ record) {\n" +
            "        if (record == null) {\n" +
            "            return 0;\n" +
            "        }\n" +
            "\n" +
            "        if (record.getId() == null || record.getId() <= 0) {\n" +
            "            record.setId(IdWorker.getId());\n" +
            "        }\n" +
            "        return $$var$$.insert(record);\n" +
            "    }\n" +
            "\n" +
            (GENERATE_XML ?
            ("    @Transactional\n" +
            "    public int addOrUpdate($$entity$$ record) {\n" +
            "        return $$var$$.insertOrUpdate(record);\n" +
            "    }\n" +
            "\n" +
            "    @Transactional\n" +
            "    public int batchAddOrUpdate(List<$$entity$$> list) {\n" +
            "        if (list == null || list.isEmpty()) {\n" +
            "            return 0;\n" +
            "        }\n" +
            "\n" +
            "        for ($$entity$$ record : list) {\n" +
            "            if (record.getId() == null || record.getId() <= 0) {\n" +
            "                record.setId(IdWorker.getId());\n" +
            "            }\n" +
            "        }\n" +
            "        return $$var$$.batchInsertOrUpdate(list);\n" +
            "    }\n" +
            "\n") : "") +
            "    @Transactional\n" +
            "    public int updateById($$entity$$ record) {\n" +
            "        return record == null ? 0 : $$var$$.updateById(record);\n" +
            "    }\n" +
            "\n" +
            "    @Transactional\n" +
            "    public int deleteById(Long id) {\n" +
            "        return (id == null || id <= 0) ? 0 : $$var$$.deleteById(id);\n" +
            "    }\n" +
            "\n" +
            "    public $$entity$$ queryById(Long id) {\n" +
            "        return (id == null || id <= 0) ? null : $$var$$.selectById(id);\n" +
            "    }\n" +
            "\n" +
            "    public List<$$entity$$> queryByIds(List<Long> ids) {\n" +
            "        if (ids == null || ids.isEmpty()) {\n" +
            "            return Collections.emptyList();\n" +
            "        }\n" +
            "        return $$var$$.selectBatchIds(ids);\n" +
            "    }\n" +
            "}\n";
    private static void service(String tableName) {
        tableName = tableName.toUpperCase().startsWith("T_") ? tableName.substring(2) : tableName;
        String serviceInfo = SERVICE.replace("$$var$$", toField(tableName) + DAO_SUFFIX)
                .replace("$$entity$$", toClass(tableName) + MODEL_SUFFIX);
        String content = String.format(serviceInfo,
                SERVICE_PACKAGE, tableToDao(tableName), tableToModel(tableName),
                toClass(tableName) + SERVICE_SUFFIX, toClass(tableName) + DAO_SUFFIX);
        writeFile(new File(JAVA_PATH + SERVICE_PACKAGE.replace(".", "/"), toClass(tableName) + SERVICE_SUFFIX + ".java"), content);
    }

    private static String toColumn(String tableName, String columnName, boolean alias) {
        return alias ? (tableToAlias(tableName) + "_" + columnName) : columnName;
    }

    private static String toColumnName(String tableName, String columnName, boolean alias) {
        if (alias) {
            return String.format("`%s`.`%s` AS `%s`", tableToAlias(tableName), columnName, toColumn(tableName, columnName, true));
        } else {
            return "`" + columnName + "`";
        }
    }
    private static String replaceQuote(String str) {
        return str.replace("\"", "\\\"");
    }

    private static String tableToAlias(String tableName) {
        StringBuilder sbd = new StringBuilder();
        for (String s : tableName.split("_")) {
            sbd.append(s.charAt(0));
        }
        return sbd.toString();
    }

    private static String tableToSql(String tableName, boolean alias) {
        return tableName + "_column" + (alias ? "_alias" : "");
    }

    private static String tableToResultMap(String tableName, boolean alias) {
        return toClass(tableName) + "Map" + (alias ? "Alias" : "");
    }

    private static String tableToModel(String tableName) {
        return MODEL_PACKAGE + "." + toClass(tableName) + MODEL_SUFFIX;
    }

    private static String tableToDao(String tableName) {
        return DAO_PACKAGE + "." + toClass(tableName) + DAO_SUFFIX;
    }

    private static String tab(int count) {
        StringBuilder sbd = new StringBuilder();
        for (int i = 0; i < (count * 4); i++) {
            sbd.append(" ");
        }
        return sbd.toString();
    }

    private static String toClass(String tableName) {
        String name = toField(tableName);
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private static String toField(String param) {
        if (param == null || "".equals(param.trim())) {
            return "";
        }
        int len = param.length();
        StringBuilder sbd = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = param.charAt(i);
            if (c == '_') {
                i++;
                if (i < len) {
                    sbd.append(Character.toUpperCase(param.charAt(i)));
                }
            } else {
                sbd.append(c);
            }
        }
        return sbd.toString();
    }

    private static String toStr(Object obj) {
        return obj == null ? "" : obj.toString().trim();
    }

    public static <K, V> Map<K, V> maps(Object... keysAndValues) {
        Map<K, V> result = Maps.newLinkedHashMap();
        if (keysAndValues != null && keysAndValues.length > 1) {
            for (int i = 0; i < keysAndValues.length; i += 2) {
                if (keysAndValues.length > (i + 1)) {
                    result.put((K) keysAndValues[i], (V) keysAndValues[i + 1]);
                }
            }
        }
        return result;
    }
}
