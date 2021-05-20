package org.veupathdb.service.eda.ss.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.veupathdb.service.eda.generated.model.APIStudyOverview;
import org.veupathdb.service.eda.generated.model.ScaleOption;
import org.veupathdb.service.eda.generated.model.ScaleOptionImpl;
import org.veupathdb.service.eda.generated.model.UnitOption;
import org.veupathdb.service.eda.generated.model.UnitOptionImpl;
import org.veupathdb.service.eda.generated.model.UnitsGroup;
import org.veupathdb.service.eda.generated.model.UnitsGroupImpl;

public class UnitsAndScale {

  public static final String LINEAR_SCALE_ID = "linear";

  private static List<UnitsGroup> _unitsGroups = new ArrayList<>();
  private static List<ScaleOption> _scaleOptions = new ArrayList<>();

  public void addUnitsGroup(UnitsGroup unitsGroup) {
    _unitsGroups.add(unitsGroup);
  }

  public void addScaleOption(ScaleOption scaleOption) {
    _scaleOptions.add(scaleOption);
  }

  public List<UnitsGroup> getUnitsGroups() {
    return _unitsGroups;
  }

  public List<ScaleOption> getScaleOptions() {
    return _scaleOptions;
  }

  public Optional<UnitsGroup> getUnitsGroup(String unitsId) {
    return _unitsGroups.stream()
        .filter(group -> group.getMembers().stream()
            .anyMatch(unitOption -> unitOption.getUnitsId().equals(unitsId)))
        .findFirst();
  }

  public static UnitsAndScale fromApplicationMetadata(List<APIStudyOverview> overviews, Function<String,Study> studyGetter) {
    UnitsAndScale uas = new UnitsAndScale();
    UnitsGroup allGroup = new UnitsGroupImpl();
    allGroup.setUnitsGroupId("all");
    allGroup.setDisplayName("All Units");
    allGroup.setMembers(getAllUnits(overviews, studyGetter));
    uas.addUnitsGroup(allGroup);
    // FIXME: scale not currently provided by the DB; thus assume linear for all
    ScaleOption linear = new ScaleOptionImpl();
    linear.setScaleId(LINEAR_SCALE_ID);
    linear.setDisplayName("Linear");
    uas.addScaleOption(linear);
    return uas;
  }

  /**
   * Gathers all the unique units of all the vars of all the
   * entities of all the studies, converts them to UnitOptions
   * and puts them in a List, which it returns.
   */
  private static List<UnitOption> getAllUnits(List<APIStudyOverview> overviews, Function<String,Study> studyGetter) {
    Set<String> uniqueUnitIds = new HashSet<>();
    overviews.stream()
        .forEach(overview -> studyGetter.apply(overview.getId())
            .getEntityTree()
            .findAll(e -> true).stream()
            .map(node -> node.getContents().getVariables())
            .forEach(varList -> varList.stream()
                .map(var -> var.getUnitsId())
                .filter(Objects::nonNull)
                .forEach(id -> uniqueUnitIds.add(id))));
    return uniqueUnitIds.stream().map(id -> {
      UnitOption opt = new UnitOptionImpl();
      opt.setUnitsId(id);
      opt.setDisplayName(id);
      return opt;
    }).collect(Collectors.toList());
  }
}
