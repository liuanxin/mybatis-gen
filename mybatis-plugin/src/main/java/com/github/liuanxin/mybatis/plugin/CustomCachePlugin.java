package com.github.liuanxin.mybatis.plugin;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.internal.util.StringUtility;

import java.util.List;

/**
 * This plugin adds a cache element to generated sqlMaps.  This plugin
 * is for MyBatis3 targeted runtimes only.  The plugin accepts the
 * following properties (all are optional):
 *
 * cache_eviction
 * cache_flushInterval
 * cache_size
 * cache_readOnly
 * cache_type
 *
 * All properties correspond to properties of the MyBatis cache element and
 * are passed "as is" to the corresponding properties of the generated cache
 * element.  All properties can be specified at the table level, or on the
 * plugin element.  The property on the table element will override any
 * property on the plugin element.
 *
 * @author Jason Bennett
 * @author Jeff Butler
 *
 * @see org.mybatis.generator.plugins.CachePlugin
 */
public class CustomCachePlugin extends PluginAdapter {

    public enum CacheProperty {
        TYPE("cache_type", "type"), //$NON-NLS-1$ //$NON-NLS-2$
        EVICTION("cache_eviction", "eviction"), //$NON-NLS-1$ //$NON-NLS-2$
        FLUSH_INTERVAL("cache_flushInterval", "flushInterval"), //$NON-NLS-1$ //$NON-NLS-2$
        SIZE("cache_size", "size"), //$NON-NLS-1$ //$NON-NLS-2$
        READ_ONLY("cache_readOnly", "readOnly"), //$NON-NLS-1$ //$NON-NLS-2$
        BLOCKING("cache_blocking", "blocking"); //$NON-NLS-1$ //$NON-NLS-2$

        private String propertyName;
        private String attributeName;

        CacheProperty(String propertyName, String attributeName) {
            this.propertyName = propertyName;
            this.attributeName = attributeName;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public String getAttributeName() {
            return attributeName;
        }
    }

    public CustomCachePlugin() {
        super();
    }

    public boolean validate(List<String> warnings) {
        return true;
    }

    @Override
    public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
        XmlElement element = new XmlElement("cache"); //$NON-NLS-1$
        context.getCommentGenerator().addComment(element);

        for (CacheProperty cacheProperty : CacheProperty.values()) {
            addAttributeIfExists(element, introspectedTable, cacheProperty);
        }

        // cache 放在最上面
        // document.getRootElement().addElement(element);
        document.getRootElement().addElement(0, element);
        return true;
    }

    private void addAttributeIfExists(XmlElement element, IntrospectedTable introspectedTable,
                                      CacheProperty cacheProperty) {
        String property = introspectedTable.getTableConfigurationProperty(cacheProperty.getPropertyName());
        if (property == null) {
            property = properties.getProperty(cacheProperty.getPropertyName());
        }

        if (StringUtility.stringHasValue(property)) {
            element.addAttribute(new Attribute(cacheProperty.getAttributeName(), property));
        }
    }
}
