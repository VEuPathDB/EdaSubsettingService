package org.veupathdb.service.eda.ss.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.veupathdb.service.eda.common.client.EdaSubsettingClient;
import org.veupathdb.service.eda.generated.model.APIEntity;
import org.veupathdb.service.eda.generated.model.APIStudyDetail;
import org.veupathdb.service.eda.generated.model.APIVariable;
import org.veupathdb.service.eda.ss.model.Study;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileDataSourceIntegTest {
//  private EdaSubsettingClient client;
//  private String baseUrl = System.getenv("BASE_URL");
//  private String authKey = System.getenv("AUTH_KEY");
//
//  @BeforeAll
//  public void beforeAll() {
//    client = new EdaSubsettingClient(baseUrl, Map.entry("Auth-Key", authKey));
//  }
//
//  @Test
//  public void test() {
//    client.getTabularDataStream()
//  }
//
//  private class DataGenerator {
//    private SecureRandom RANDOM = new SecureRandom();
//
//    public String getRandomStudy() {
//      final List<String> studyIds = client.getStudies();
//      return client.getStudies().stream()
//          .skip(RANDOM.nextInt(studyIds.size()))
//          .findFirst()
//          .get();
//    }
//
//    public List<APIVariable> getRandomVariables(String studyId) {
//      final APIStudyDetail studyDetail = client.getStudy(studyId).orElseThrow();
//      final List<APIVariable> studyVariables = collectVars(studyDetail.getRootEntity());
//      final int numVars = RANDOM.nextInt(10);
//      Collections.shuffle(studyVariables);
//      return studyVariables.stream()
//          .limit(numVars)
//          .collect(Collectors.toList());
//    }
//
//    private List<APIVariable> collectVars(APIEntity root) {
//      List<APIVariable> variables = root.getVariables();
//      for (APIEntity child: root.getChildren()) {
//        variables.addAll(collectVars(child));
//      }
//      return variables;
//    }
//  }
}
