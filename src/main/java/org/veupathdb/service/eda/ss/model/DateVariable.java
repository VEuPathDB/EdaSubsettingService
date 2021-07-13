package org.veupathdb.service.eda.ss.model;

import java.util.List;
import org.veupathdb.service.eda.generated.model.BinUnits;

public class DateVariable extends VariableWithValues {

	private final String displayRangeMin;
	private final String displayRangeMax;
	private final String rangeMin;
	private final String rangeMax;
	private final BinUnits binUnitsOverride;
	private final BinUnits binUnits;

	public DateVariable(String providerLabel, String id, Entity entity,
			VariableDataShape dataShape, VariableDisplayType displayType,
			String displayName, Integer displayOrder, String parentId, String definition, List<String> vocabulary, String displayRangeMin,
			String displayRangeMax, String rangeMin, String rangeMax, String binUnitsOverride,
			String binUnits, Boolean isTemporal, Boolean isFeatured, Boolean isMergeKey,
			Number distinctValuesCount, Boolean isMultiValued) {

		super(providerLabel, id, entity, VariableType.DATE, dataShape, displayType, displayName, displayOrder, parentId, definition,
				vocabulary, isTemporal, isFeatured, isMergeKey, distinctValuesCount, isMultiValued);

		String errPrefix = "In entity " + entity.getId() + " variable " + id + " has a null ";

		if (dataShape == VariableDataShape.CONTINUOUS) {
      this.binUnits = BinUnits.valueOf(binUnits.toUpperCase());
      this.binUnitsOverride = binUnitsOverride == null ? null :
          BinUnits.valueOf(binUnitsOverride.toUpperCase());
    }
		else {
		  this.binUnits = null;
		  this.binUnitsOverride = null;
    }

    this.displayRangeMin = displayRangeMin;
    this.displayRangeMax = displayRangeMax;

		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;
	}

	public String getDisplayRangeMin() {
		return displayRangeMin;
	}

	public String getDisplayRangeMax() {
		return displayRangeMax;
	}

	public String getRangeMin() {
		return rangeMin;
	}

	public String getRangeMax() {
		return rangeMax;
	}

	public BinUnits getBinUnitsOverride() {
		return binUnitsOverride;
	}

	public BinUnits getBinUnits() {
		return binUnits;
	}

	public BinUnits getDefaultBinUnits() {
	  return binUnitsOverride != null ? binUnitsOverride : binUnits;
  }
}
