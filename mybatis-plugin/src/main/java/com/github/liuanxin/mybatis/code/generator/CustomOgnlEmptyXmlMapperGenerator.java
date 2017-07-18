package com.github.liuanxin.mybatis.code.generator;

import com.github.liuanxin.mybatis.code.mapper.CustomOgnlEmptyXMLMapperGenerator;
import org.mybatis.generator.codegen.AbstractXmlGenerator;
import org.mybatis.generator.codegen.mybatis3.javamapper.JavaMapperGenerator;

public class CustomOgnlEmptyXmlMapperGenerator extends JavaMapperGenerator {

    @Override
    public AbstractXmlGenerator getMatchedXMLGenerator() {
        return new CustomOgnlEmptyXMLMapperGenerator();
    }
}
