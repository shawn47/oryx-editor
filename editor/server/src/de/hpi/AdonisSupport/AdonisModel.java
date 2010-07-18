package de.hpi.AdonisSupport;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmappr.Attribute;
import org.xmappr.Element;
import org.xmappr.RootElement;

//<!ELEMENT MODEL (MODELATTRIBUTES, INSTANCE*, CONNECTOR*)>
//<!ATTLIST MODEL
//  id        ID    #IMPLIED
//  name      CDATA #REQUIRED
//  version   CDATA #REQUIRED
//  modeltype CDATA #REQUIRED
//  libtype   CDATA #REQUIRED
//  applib    CDATA #REQUIRED
//>

@RootElement("MODEL")
public class AdonisModel extends AdonisStencil{
	@Attribute(name="id")
	protected String id;
	@Attribute("name")
	protected String name;
	@Attribute("version")
	protected String version;
	@Attribute("modeltype")
	protected String modeltype;
	@Attribute("libtype")
	protected String libtype;
	@Attribute("applib")
	protected String applib;
		
	protected Map<String,String> inheritedProperties;
	
	/**
	 * these attributes have influences on the representation in oryx
	 * so they are stored including the type
	 */
	protected static Map<String,String> evaluatedAttributes;
	static { evaluatedAttributes = new HashMap<String,String>();
		evaluatedAttributes.put("World area","STRING");
//		evaluatedAttributes.put("Creation date","STRING");
//		evaluatedAttributes.put("Date last changed","STRING");
	};
	
	@Element(name="MODELATTRIBUTES")
	protected AdonisModelAttributes modelAttributes;
	
	@Element(name="INSTANCE")
	protected ArrayList<AdonisInstance> instance;
		
	@Element(name="CONNECTOR")
	protected ArrayList<AdonisConnector> connector;
	
	public String children;
	
	public String getId(){return id;}
	public void setId(String newId){id = newId;}
	
	public void setName(String value){name = value;}
	public String getName(){return name;}
	
	public void setVersion(String value){version = value;}
	public String getVersion(){return version;}
	
	public void setModeltype(String value){modeltype = value;}
	public String getModeltype(){return modeltype;}
	
	public void setLibtype(String value){libtype = value;}
	public String getLibtype(){return libtype;}
	
	public void setApplib(String value){applib = value;}
	public String getApplib(){return applib;}
	
	public AdonisModelAttributes getModelAttributes(){return modelAttributes;}
	
	public void setModelAttributes(AdonisModelAttributes list){	modelAttributes = list;}
		
	public ArrayList<AdonisInstance> getInstance(){return instance;}
	
	public void setInstance(ArrayList<AdonisInstance> list){instance = list;}
	
	public ArrayList<AdonisConnector> getConnector(){return connector;}
	
	public void setConnector(ArrayList<AdonisConnector> list){connector = list;}
	
	@Override
	public Map<String,String> getEvaluatedAttributes(){return evaluatedAttributes;}

	public void setInheritedProperties(HashMap<String, String> attributes) {inheritedProperties = attributes;}
	
	public void distributeChildren() throws JSONException{
		// put all stencils in the diagram in map ... all stencils have access to this map
		for (AdonisInstance instance : getInstance()){
			getModelInstance().put(instance.getName(),instance);
			instance.setModelInstances(getModelInstance());
			instance.setModel(this);
		}
		
		for (AdonisConnector edge : getConnector()){
			if ("is inside".equalsIgnoreCase(getOryxNameOf(edge.getStencilClass()))){
				AdonisStencil child = getModelInstance().get(edge.getFrom().getInstance());
				AdonisStencil parent = getModelInstance().get(edge.getTo().getInstance());
				child.setParent(parent);
			}
			edge.setModel(this);
		}
		System.err.println();
	}
	
	private Double[] bounds = null;
	
	private String[] filterArea(String area){
		String filteredArea = area.replace("w:", "");
		filteredArea = filteredArea.replace("h:", "");
		filteredArea = filteredArea.replace("cm","");
		return filteredArea.split(" ");
	}
	
	public Double[] getBounds() {
		if (bounds == null){
			bounds = new Double[4];
			try {
				for (AdonisAttribute aAttribute : getModelAttributes().getAttribute()){
					if ("world area".equalsIgnoreCase(getOryxNameOf(aAttribute.getName()))){
						// extract the numbers out of the string
						if (aAttribute.getElement() != null){
							String[] area = filterArea(aAttribute.getElement());
				
							// try to give the diagram a nice size
							bounds[2] = Double.parseDouble(area[0])*CENTIMETERTOPIXEL;
							bounds[3] = Double.parseDouble(area[1])*CENTIMETERTOPIXEL;
						} else {
							// a fallback solution
							bounds[2] = 1485.0;
							bounds[3] = 1050.0;
						}
						break;
					}
				}
			} catch (JSONException e){
				Log.e(e.getMessage());
			}
			bounds[0] = 0.0;
			bounds[1] = 0.0;
			if (bounds[2] == null) bounds[2] = 1485.0;
			if (bounds[3] == null) bounds[3] = 1050.0;
		}
		return bounds;
	}
	
	
	//*************************************************************************
	//* methods for creation of JSON representation
	//*************************************************************************
	

	/**
	 * write global attributes inherited from AdoXML 
	 */
	public void writeJSONinheritedProperties(JSONObject json) throws JSONException{
		JSONObject properties = getJSONObject(json, "properties");
		//XXX
//		JSONObject inherited = getJSONObject(properties,"inheritedProperties");
//		for (String key : inheritedProperties.keySet()){
//			inherited.put(key, inheritedProperties.get(key));
//		}
	}
	/**
	 * write the resource id
	 */
	@Override
	public void writeJSONresourceId(JSONObject json) throws JSONException{
		//XXX
		json.put("resourceId","oryx-canvas123");
	}
	/**
	 * write the stencil
	 */
	@Override
	public void writeJSONstencil(JSONObject json) throws JSONException{
		//XXX oryx-canvas123 vs diagram 
		JSONObject stencil = getJSONObject(json,"stencil");
		stencil.put("id","Diagram");
	}
	/**
	 * write the properties (including not used ones and except special attributes with a representation in oryx)
	 */
	@Override
	public void writeJSONproperties(JSONObject json) throws JSONException{
		JSONObject properties = getJSONObject(json, "properties");
//		XXX
//		properties.put("id",getId());
//		properties.put("name",getName());
//		properties.put("version",getVersion());
//		properties.put("modeltype",getModeltype());
//		properties.put("libtype",getLibtype());
//		properties.put("applib",getApplib());
		
//		TODO sort out
//		// store the unused attributes as triple in the properties
//		for (AdonisAttribute aAttribute : getModelAttributes().getAttribute()){
//			if (!getEvaluatedAttributes().containsKey(aAttribute.getName())){
//				aAttribute.write(properties);
//			}
//		}
	}
	/**
	 * write the used stencilsets to the file 
	 */
	public void writeJSONstencilset(JSONObject json) throws JSONException{
		JSONObject stencilset = getJSONObject(json, "stencilset");
		stencilset.put("url","/oryx//stencilsets/adonis/adonis.json");
	}
	/**
	 * write the namespace
	 */
	public void writeJSONnamespace(JSONObject json) throws JSONException{
		JSONObject stencilset = getJSONObject(json, "stencilset");
		stencilset.put("namespace","http://b3mn.org/stencilset/adonis#");
	}
	/**
	 * write down the used stencilset extensions
	 */
	public void writeJSONssextensions(JSONObject json) throws JSONException{
		getJSONArray(json, "ssextensions");
	}
	/**
	 * write all childshapes to the diagram
	 */
	@Override
	public void writeJSONchildShapes(JSONObject json) throws JSONException {
		JSONArray childShapes = getJSONArray(json,"childShapes");
		JSONObject shape = null;
		
		// writes all stencils - only connectors are missing
		super.writeJSONchildShapes(json);
		
		for (AdonisConnector aConnector : getConnector()){
			if (!"is inside".equalsIgnoreCase(aConnector.getStencilClass())){
				shape = new JSONObject();
				aConnector.write(shape);
				childShapes.put(shape);
			}
		}
	}
	
	/**
	 * write the bounds of the diagram
	 */
	@Override
	public void writeJSONbounds(JSONObject json) throws JSONException {
		JSONObject bounds = getJSONObject(json,"bounds");
		JSONObject temp = getJSONObject(bounds,"upperLeft");
		temp.put("x",getBounds()[0]);
		temp.put("y",getBounds()[1]);
		temp = getJSONObject(bounds,"lowerRight");
		temp.put("x",getBounds()[2]);
		temp.put("y",getBounds()[3]);
	}
	/**
	 * a diagram has no dockers - no implementation needed
	 */
	@Override
	public void writeJSONdockers(JSONObject json) throws JSONException {
		// not needed
	}
	
	/**
	 * overridden to make sure that every stencil is registered in the nameObjectMap 
	 */
	@Override
	public void write(JSONObject modelElement) throws JSONException {
		distributeChildren();

		super.write(modelElement);
	}
	/**
	 * a diagram has no outgoing edges
	 */
	@Override
	public void writeJSONoutgoing(JSONObject json) throws JSONException {
		// nothing to do
	}
	
	@Override
	public void writeJSONtarget(JSONObject json) throws JSONException{
		// not used
	}

}
