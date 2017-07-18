package com.github.liuanxin.mybatis.code.generator;

import com.github.liuanxin.mybatis.code.mapper.CustomAliasXMLMapperGenerator;
import org.mybatis.generator.codegen.AbstractXmlGenerator;
import org.mybatis.generator.codegen.mybatis3.javamapper.JavaMapperGenerator;

public class CustomAliasXmlMapperGenerator extends JavaMapperGenerator {

    @Override
    public AbstractXmlGenerator getMatchedXMLGenerator() {
        return new CustomAliasXMLMapperGenerator();
    }
}
