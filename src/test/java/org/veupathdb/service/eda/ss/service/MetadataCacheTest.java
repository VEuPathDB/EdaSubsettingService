package org.veupathdb.service.eda.ss.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.veupathdb.service.eda.ss.model.StudyOverview;
import org.veupathdb.service.eda.ss.model.db.StudyProvider;
import org.veupathdb.service.eda.ss.model.variable.binary.BinaryFilesManager;

import java.time.Duration;
import java.util.Date;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@ExtendWith(MockitoExtension.class)
public class MetadataCacheTest  {
  @Mock private BinaryFilesManager binaryFilesManager;
  @Mock private StudyProvider studyProvider;

  @Test
  @DisplayName("Test Overviews out of sync")
  public void test1() throws Exception {
    Date initialModDate = new Date();
    Date updatedModDate = new Date(initialModDate.getTime() + Duration.ofDays(1).toMillis());
    Mockito.when(studyProvider.getStudyOverviews())
        .thenReturn(List.of(
            new StudyOverview(
                "study-1",
                "study-1-internal",
                StudyOverview.StudySourceType.CURATED,
                initialModDate)))
        .thenReturn(List.of(
            new StudyOverview(
                "study-1",
                "study-1-internal",
                StudyOverview.StudySourceType.CURATED,
                updatedModDate)));
    MetadataCache instance = new MetadataCache(binaryFilesManager, studyProvider, Duration.ofSeconds(1L));
    List<StudyOverview> initialOverviews = instance.getStudyOverviews();
    Assertions.assertEquals(initialModDate, initialOverviews.get(0).getLastModified());
    await()
        .atMost(3, SECONDS)
        .pollInterval(Duration.ofMillis(100L))
        .until(() -> instance.getStudyOverviews().get(0).getLastModified().equals(updatedModDate));
  }
}