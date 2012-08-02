package com.fasterxml.jackson.databind.jsonschema;

import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsonschema.types.ObjectSchema;
import com.fasterxml.jackson.databind.jsonschema.types.Schema;
import com.fasterxml.jackson.databind.jsonschema.types.ArraySchema.Items;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

/**
 * @author Ryan Heaton
 */
public class TestGenerateJsonSchema
    extends com.fasterxml.jackson.databind.BaseMapTest
{
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    public static class SimpleBean
    {
        private int property1;
        private String property2;
        private String[] property3;
        private Collection<Float> property4;
        @JsonProperty(required = true)
        private String property5;
        
        public int getProperty1()
        {
            return property1;
        }

        public void setProperty1(int property1)
        {
            this.property1 = property1;
        }

        public String getProperty2()
        {
            return property2;
        }

        public void setProperty2(String property2)
        {
            this.property2 = property2;
        }

        public String[] getProperty3()
        {
            return property3;
        }

        public void setProperty3(String[] property3)
        {
            this.property3 = property3;
        }

        public Collection<Float> getProperty4()
        {
            return property4;
        }

        public void setProperty4(Collection<Float> property4)
        {
            this.property4 = property4;
        }
        
        public String getProperty5()
        {
            return property5;
        }

        public void setProperty5(String property5)
        {
            this.property5 = property5;
        }
    }

    public class TrivialBean {
        public String name;
    }

    //@JsonSerializableSchema(id="myType")
    public class BeanWithId {
        public String value;
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();
    
    /**
     * tests generating json-schema stuff.
     */
    public void testGeneratingJsonSchema()
        throws Exception
    {
    	DefaultSerializerProvider sp = new DefaultSerializerProvider.Impl();
        ObjectMapper m = new ObjectMapper();
        m.setSerializerProvider(sp);
        Schema schema = m.generateJsonSchema(SimpleBean.class);
        
        assertNotNull(schema);

        // test basic equality, and that equals() handles null, other obs
        assertTrue(schema.equals(schema));
        assertFalse(schema.equals(null));
        assertFalse(schema.equals("foo"));

        assertTrue(schema.isObjectSchema());
        ObjectSchema object = schema.asObjectSchema();
        assertNotNull(object);
        Map<String,Schema> properties = object.getProperties();
        assertNotNull(properties);
        Schema prop1 = properties.get("property1");
        assertNotNull(prop1);
        assertTrue(prop1.isIntegerSchema());
        assertFalse(prop1.getRequired());
        
        Schema prop2 = properties.get("property2");
        assertNotNull(prop2);
        assertTrue(prop2.isStringSchema());
        assertFalse(prop2.getRequired());
        
        Schema prop3 = properties.get("property3");
        assertNotNull(prop3);
        assertTrue(prop3.isArraySchema());
        assertFalse(prop3.getRequired());
        Items items = prop3.asArraySchema().getItems();
        assertTrue(items.isSingleItems());
        Schema itemType = items.asSingleItems().getSchema();
        assertNotNull(itemType);
        assertTrue(itemType.isStringSchema());
        
        Schema prop4 = properties.get("property4");
        assertNotNull(prop4);
        assertTrue(prop4.isArraySchema());
        assertFalse(prop4.getRequired());
        items = prop4.asArraySchema().getItems();
        assertTrue(items.isSingleItems());
        itemType = items.asSingleItems().getSchema();
        assertNotNull(itemType);
        assertTrue(itemType.isNumberSchema());
        
        Schema prop5 = properties.get("property5");
        assertNotNull(prop5);
        assertTrue(prop5.getRequired());
        
        

//        assertEquals("array", property3Schema.get("type").asText());
//        assertEquals(false, property3Schema.path("required").booleanValue());
//        assertEquals("string", property3Schema.get("items").get("type").asText());
//        JsonNode property4Schema = propertiesSchema.get("property4");
//        assertNotNull(property4Schema);
//        assertEquals("array", property4Schema.get("type").asText());
//        assertEquals(false, property4Schema.path("required").booleanValue());
//        assertEquals("number", property4Schema.get("items").get("type").asText());
    }
    
    @JsonFilter("filteredBean")
    private static class FilteredBean {
    	
    	@JsonProperty
    	private String secret = "secret";
    	
    	@JsonProperty
    	private String obvious = "obvious";
    	
    	public String getSecret() { return secret; }
    	public void setSecret(String s) { secret = s; }
    	
    	public String getObvious() { return obvious; }
    	public void setObvious(String s) {obvious = s; }
    }
    
    public static FilterProvider secretFilterProvider = new SimpleFilterProvider()
    .addFilter("filteredBean", SimpleBeanPropertyFilter.filterOutAllExcept(new String[]{"obvious"}));
    /** */
    public void testGeneratingJsonSchemaWithFilters() throws Exception {
    	ObjectMapper mapper = new ObjectMapper();
    	mapper.setFilters(secretFilterProvider);
    	Schema schema = mapper.generateJsonSchema(FilteredBean.class);
//    	JsonNode node = schema.getSchemaNode().get("properties");
//    	assertTrue(node.has("obvious"));
//    	assertFalse(node.has("secret"));
    }

    /**
     * Additional unit test for verifying that schema object itself
     * can be properly serialized
     */
    public void testSchemaSerialization()
            throws Exception
    {
        Schema jsonSchema = MAPPER.generateJsonSchema(SimpleBean.class);
	Map<String,Object> result = writeAndMap(MAPPER, jsonSchema);
	assertNotNull(result);
	// no need to check out full structure, just basics...
	assertEquals("object", result.get("type"));
	// only add 'required' if it is true...
	assertNull(result.get("required"));
	assertNotNull(result.get("properties"));
    }

    public void testInvalidCall()
        throws Exception
    {
        // not ok to pass null
        try {
            MAPPER.generateJsonSchema(null);
            fail("Should have failed");
        } catch (IllegalArgumentException iae) {
            verifyException(iae, "class must be provided");
        }
    }

    /**
     * Test for [JACKSON-454]
     */
    public void testThatObjectsHaveNoItems() throws Exception
    {
        Schema jsonSchema = MAPPER.generateJsonSchema(TrivialBean.class);
        String json = jsonSchema.toString().replaceAll("\"", "'");
        // can we count on ordering being stable? I think this is true with current ObjectNode impl
        // as perh [JACKSON-563]; 'required' is only included if true
        assertEquals("{'type':'object','properties':{'name':{'type':'string'}}}",
                json);
    }

    public void testSchemaId() throws Exception
    {
        Schema jsonSchema = MAPPER.generateJsonSchema(BeanWithId.class);
        String json = jsonSchema.toString().replaceAll("\"", "'");
        assertEquals("{'type':'object','id':'myType','properties':{'value':{'type':'string'}}}",
                json);
    }
}
