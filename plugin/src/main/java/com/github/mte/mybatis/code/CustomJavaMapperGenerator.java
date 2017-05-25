package com.github.mte.mybatis.code;

import com.github.mte.mybatis.code.xmlmapper.CustomXMLMapperGenerator;
import org.mybatis.generator.codegen.AbstractXmlGenerator;
import org.mybatis.generator.codegen.mybatis3.javamapper.JavaMapperGenerator;

public class CustomJavaMapperGenerator extends JavaMapperGenerator {

    @Override
    public AbstractXmlGenerator getMatchedXMLGenerator() {
        return new CustomXMLMapperGenerator();
    }
}
