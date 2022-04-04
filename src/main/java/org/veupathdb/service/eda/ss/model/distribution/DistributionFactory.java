package org.veupathdb.service.eda.ss.model.distribution;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.sql.DataSource;
import jakarta.ws.rs.BadRequestException;
import org.gusdb.fgputil.Tuples;
import org.gusdb.fgputil.functional.TreeNode;
import org.veupathdb.service.eda.generated.model.BinSpecWithRange;
import org.veupathdb.service.eda.generated.model.ValueSpec;
import org.veupathdb.service.eda.ss.model.Entity;
import org.veupathdb.service.eda.ss.model.Study;
import org.veupathdb.service.eda.ss.model.StudySubsettingUtils;
import org.gusdb.fgputil.distribution.AbstractDistribution;
import org.gusdb.fgputil.distribution.DateBinDistribution;
import org.gusdb.fgputil.distribution.DiscreteDistribution;
import org.gusdb.fgputil.distribution.DistributionResult;
import org.gusdb.fgputil.distribution.DistributionStreamProvider;
import org.gusdb.fgputil.distribution.FloatingPointBinDistribution;
import org.gusdb.fgputil.distribution.IntegerBinDistribution;
import org.veupathdb.service.eda.ss.model.filter.Filter;
import org.veupathdb.service.eda.ss.model.variable.DateVariable;
import org.veupathdb.service.eda.ss.model.variable.FloatingPointVariable;
import org.veupathdb.service.eda.ss.model.variable.IntegerVariable;
import org.veupathdb.service.eda.ss.model.variable.VariableDataShape;
import org.veupathdb.service.eda.ss.model.variable.VariableWithValues;

public class DistributionFactory {

  private static class EdaDistributionStreamProvider<T extends VariableWithValues> implements DistributionStreamProvider {

    // used to produce the stream of distribution tuples
    private final DataSource _ds;
    private final Entity _targetEntity;
    protected final T _variable;
    private final List<Filter> _filters;

    private final TreeNode<Entity> _prunedEntityTree;
    private final long _subsetEntityCount;

    public EdaDistributionStreamProvider(
        DataSource ds, Study study, Entity targetEntity, T variable, List<Filter> filters) {
      _ds = ds;
      _targetEntity = targetEntity;
      _variable = variable;
      _filters = filters;

      // get entity tree pruned to those entities of current interest
      _prunedEntityTree = StudySubsettingUtils.pruneTree(
          study.getEntityTree(), _filters, _targetEntity);

      // get the number of entities in the subset
      _subsetEntityCount = StudySubsettingUtils.getEntityCount(
          _ds, _prunedEntityTree, _targetEntity, _filters);
    }

    @Override
    public Stream<Tuples.TwoTuple<String, Long>> getDistributionStream() {
      return StudySubsettingUtils.produceVariableDistribution(
          _ds, _prunedEntityTree, _targetEntity, _variable, _filters);
    }

    @Override
    public long getRecordCount() {
      return _subsetEntityCount;
    }
  }

  public static DistributionResult processDistributionRequest(
      DataSource ds, Study study, Entity targetEntity,
      VariableWithValues var, List<Filter> filters,
      ValueSpec apiValueSpec, Optional<BinSpecWithRange> incomingBinSpec) {
    try {
      AbstractDistribution.ValueSpec valueSpec = convertValueSpec(apiValueSpec);

      // inspect requested variable and select appropriate distribution
      AbstractDistribution distribution;
      if (var.getDataShape() == VariableDataShape.CONTINUOUS) {
        distribution = switch(var.getType()) {
          case INTEGER -> new IntegerBinDistribution(
              new EdaDistributionStreamProvider<>(ds, study, targetEntity, (IntegerVariable)var, filters),
              valueSpec, new EdaNumberBinSpec((IntegerVariable)var, incomingBinSpec));
          case NUMBER -> new FloatingPointBinDistribution(
              new EdaDistributionStreamProvider<>(ds, study, targetEntity, (FloatingPointVariable)var, filters),
              valueSpec, new EdaNumberBinSpec((FloatingPointVariable)var, incomingBinSpec));
          case DATE -> new DateBinDistribution(
              new EdaDistributionStreamProvider<>(ds, study, targetEntity, (DateVariable)var, filters),
              valueSpec, new EdaDateBinSpec((DateVariable)var, incomingBinSpec));
          default -> throw new BadRequestException("Among continuous variables, " +
              "distribution endpoint supports only date, integer, and number types; " +
              "requested variable '" + var.getId() + "' is type " + var.getType());
        };
      }
      else {
        if (incomingBinSpec.isPresent()) {
          throw new BadRequestException("Bin spec is allowed/required only for continuous variables.");
        }
        distribution = new DiscreteDistribution(
            new EdaDistributionStreamProvider<>(
                ds, study, targetEntity, var, filters), valueSpec);
      }

      return distribution.generateDistribution();
    }
    catch(IllegalArgumentException e) {
      // underlying lib sometimes throws this; indicates bad request data coming in
      throw new BadRequestException("Variable " + var.getId() + " of type " + var.getType() + ": " + e.getMessage());
    }
  }

  private static AbstractDistribution.ValueSpec convertValueSpec(ValueSpec apiValueSpec) {
    return switch(apiValueSpec) {
      case COUNT -> AbstractDistribution.ValueSpec.COUNT;
      case PROPORTION -> AbstractDistribution.ValueSpec.PROPORTION;
    };
  }
}
