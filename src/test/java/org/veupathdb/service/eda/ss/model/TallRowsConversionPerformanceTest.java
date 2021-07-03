package org.veupathdb.service.eda.ss.model;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.Timer;
import org.gusdb.fgputil.functional.FunctionalInterfaces.SupplierWithException;
import org.junit.jupiter.api.Test;
import org.veupathdb.service.eda.common.model.VariableDef;

public class TallRowsConversionPerformanceTest {

  private static final Logger LOG = LogManager.getLogger(TallRowsConversionPerformanceTest.class);

  // specify the entity to convert
  private static final Function<TestModel, Entity> TEST_ENTITY = m -> m.observation;

  // specify how many records of that entity to convert
  private static final int NUM_RECORDS_TO_PROCESS = 20000;

  // whether to cache a sample record (can help determine perf bottleneck)
  private static final boolean CACHE_SAMPLE_RECORD = false;

  // location to dump if you want to look at results afterward (null value results in no writing)
  private static final String OUTPUT_FILE_LOCATION = null; //"/Users/rdoherty/tabular_results.txt";

  @Test
  public void doTallRowsPerfTest() throws Exception {
    Entity entity = TEST_ENTITY.apply(new TestModel());
    List<VariableSpecification> varSpecs = new ArrayList<>();
    for (Variable var : entity.getVariables()) {
      varSpecs.add(
          var.getType() != Variable.VariableType.NUMBER ?
              new VariableSpecification(var, null, null) :
              new VariableSpecification(var, ((NumberVariable)var).getUnitsId(), ((NumberVariable)var).getScaleId()));
    }
    List<VariableDef> outputColumnVars = StudySubsettingUtils.getTabularOutputColumns(entity, varSpecs);
    List<String> outputColumns = outputColumnVars.stream().map(var -> var.getVariableId()).collect(Collectors.toList());
    TallRowsGeneratedResultIterator iterator = new TallRowsGeneratedResultIterator(entity, NUM_RECORDS_TO_PROCESS, CACHE_SAMPLE_RECORD);
    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(OUTPUT_STREAM_PROVIDER.get()))) {
      Timer t = new Timer();
      StudySubsettingUtils.writeWideRows(iterator, writer, outputColumns, entity);
      writer.flush();
      LOG.info("Time to dump " + NUM_RECORDS_TO_PROCESS + " entity records: " + t.getElapsedString());
    }
  }

  private static final SupplierWithException<OutputStream> OUTPUT_STREAM_PROVIDER =
      () -> OUTPUT_FILE_LOCATION == null

      // write to nowhere for general performance testing
      ? new OutputStream() { @Override public void write(int b) throws IOException { } }

      // use local file if you want to look at the results after the test runs
      : new FileOutputStream(OUTPUT_FILE_LOCATION);

}
