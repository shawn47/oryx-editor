package de.hpi.visio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.hpi.visio.data.Page;
import de.hpi.visio.data.Shape;
import de.hpi.visio.data.VisioDocument;
import de.hpi.visio.data.XForm;
import de.hpi.visio.util.ImportConfigurationUtil;
import de.hpi.visio.util.ShapeUtil;

/**
 * The VisioDataCleaner prepares the xmappr-generated Java classes to be mapped
 * to the Diagram Api of Oryx. 
 * - Resolves MasterIds, when in *.vdx-data the nameU (type) was only set by a reference to a master-shape.
 * - Checks diagrams and all shapes for bounds
 * - Merges markers and shapes to be oryx-conform shapes (e.g. task with a loop-marker)
 * - Sets properties from shape'S nameU values
 * @author Lauritz Thamsen
 */
public class VisioDataCleaner {
	
	private ImportConfigurationUtil importUtil;
	private ShapeUtil shapeUtil;
	
	public VisioDataCleaner(ImportConfigurationUtil importUtil, ShapeUtil shapeUtil) {
		this.importUtil = importUtil;
		this.shapeUtil = shapeUtil;
	}

	public Page checkAndCleanVisioData(VisioDocument visioData) {
		Page page = resolveMasterReferences(visioData);
		Page checkedPage = checkPage(page);
		Page checkedAndCleanedPage = cleanPage(checkedPage);
		return checkedAndCleanedPage;
	}

	private Page resolveMasterReferences(VisioDocument visioData) {
		Map<String, String>masterIdToNameMapping = visioData.getMasterIdToNameMapping();
		Page firstPage = visioData.getFirstPage();
		for (Shape shape : firstPage.getShapes()) {
			if (shape.getName() == null || shape.getName().equals(""))
				if (shape.getMasterId() != null && !shape.getMasterId().equals("")) {
					String newName = masterIdToNameMapping.get(shape.getMasterId());
					shape.setName(newName);
				}
		}
		return firstPage;
	}

	private Page checkPage(Page visioPage) {
		Page halfCheckedPage = checkDiagramForBounds(visioPage);
		Page fullCheckedPage = checkAllShapesForBounds(halfCheckedPage);
		return fullCheckedPage;
	}

	private Page cleanPage(Page visioPage) {
		Page pageWithStandardShapes = normalizeNames(visioPage);
		Page pageWithConvertedMarkers = convertMarkersToTypeWithProperties(pageWithStandardShapes);
		Page pageWithConvertedProperties = convertSpecialTypesToTypesWithProperties(pageWithConvertedMarkers);
		Page pageWithRealSubprocesses = convertTaskWithMarkerToSubprocesses(pageWithConvertedProperties);
		return pageWithRealSubprocesses;
	}

	private Page checkDiagramForBounds(Page visioPage) {
		if (visioPage.getWidth() == null) {
			String heuristicValue = importUtil.getHeuristicValue("Default_Page_Width");
			visioPage.setWidth(Double.valueOf(heuristicValue));
		}
		if (visioPage.getHeight() == null) {
			String heuristicValue = importUtil.getHeuristicValue("Default_Page_Height");
			visioPage.setHeight(Double.valueOf(heuristicValue));
		}
		return visioPage;
	}
	
	private Page checkAllShapesForBounds(Page page) {
		List<Shape> allShapes = page.getShapes();
		List<Shape> shapesWithBounds = new ArrayList<Shape>();
		for (Shape shape : allShapes) {
			if (hasCompleteBoundaries(shape)) 
				shapesWithBounds.add(shape);
		}
		page.setShapes(shapesWithBounds);
		return page;
	}

	private Page normalizeNames(Page visioPage) {
		List<Shape> shapes = visioPage.getShapes();
		List<Shape> shapesWithNormalizedNames = new ArrayList<Shape>();
		for (Shape shape : shapes) {
			if (shape.getName() != null && shape.getName().contains(".")) {
				int end = shape.getName().indexOf(".");
				String cleanedName = shape.getName().substring(0,end);
				shape.setName(cleanedName);
			} 
			shapesWithNormalizedNames.add(shape);
		}
		visioPage.setShapes(shapesWithNormalizedNames);
		return visioPage;
	}
	
	private Page convertMarkersToTypeWithProperties(Page page) {
		String propertyElementsString = importUtil.getStencilSetConfig("areOnlyTaskProperties");
		String[] propertyElements = propertyElementsString.split(",");
		Shape resultingShape = null;
		for (String propertyElementName : propertyElements) {
			List<Shape> propertyShapes = page.getShapesByName(propertyElementName);
			for (Shape propertyShape : propertyShapes) {
				Boolean isOnlyAMarkerElement = Boolean.valueOf(importUtil.getStencilSetConfig("taskProperties." + propertyElementName + ".isMarker"));
				if (isOnlyAMarkerElement) {
					Shape containingShape = shapeUtil.getFirstShapeOfStencilThatContainsTheGivenShape(page.getShapes(), propertyShape, "Task");
					if (containingShape != null) { 
						resultingShape = containingShape;
					}
					page.removeShape(propertyShape);
				} else {
					resultingShape = propertyShape;
				}
				String propertyKey = importUtil.getStencilSetConfig("taskProperties." + propertyElementName + ".key");
				String propertyValue = importUtil.getStencilSetConfig("taskProperties." + propertyElementName + ".value");
				if (resultingShape != null && propertyKey != null && propertyValue != null) {
					resultingShape.putProperty(propertyKey, propertyValue);
				}
			}
		}
		return page;
	}
	
	private Page convertSpecialTypesToTypesWithProperties(Page page) {
		String[] nameUTypesIncludingProperties = importUtil.getStencilSetConfig("specialTypes").split(",");
		for (Shape shape : page.getShapes()) {
			for (String specialType : nameUTypesIncludingProperties) {
				if (specialType.equalsIgnoreCase(shape.getName())) {
					String[] keys = importUtil.getStencilSetConfig(shape.getName() + ".keys").split(",");
					String[] values = importUtil.getStencilSetConfig(shape.getName() + ".values").split(",");
					for (int i=0; i<keys.length; i++) {
						shape.putProperty(keys[i], values[i]);
					} 
				}
			}
		}
		return page;
	}
	
	private Page convertTaskWithMarkerToSubprocesses(Page page) {
		List<Shape> subprocessMarkers = page.getShapesByName("Collapsed Subprocess Marker");
		for (Shape marker : subprocessMarkers) {
			Shape containingShape = shapeUtil.getFirstShapeOfStencilThatContainsTheGivenShape(page.getShapes(), marker, "Task");
			if (containingShape != null) {
				containingShape.setName(importUtil.getStencilSetConfig("taskWithSubprocessMarker"));
			}	
			page.removeShape(marker);
		}
		return page;
	}



	private boolean hasCompleteBoundaries(Shape shape) {
		XForm xForm = shape.xForm;
		if ( xForm.height != null && xForm.width != null && xForm.positionX != null && xForm.positionY != null) {
			return true;
		} else {
			return false;
		}
	}
	
}
