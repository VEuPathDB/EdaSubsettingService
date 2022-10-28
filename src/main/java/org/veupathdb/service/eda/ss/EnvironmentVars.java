package org.veupathdb.service.eda.ss;

import static org.gusdb.fgputil.runtime.Environment.getOptionalVar;
import static org.gusdb.fgputil.runtime.Environment.getRequiredVar;

public class EnvironmentVars {

  protected boolean _developmentMode;
  protected String _appDbSchema;
  protected String _userStudySchema;
  protected String _datasetAccessServiceUrl;
  protected String _binaryFilesDirectory;
  protected String _availableBinaryFilesPaths;
  protected String _dbBuild;
  protected boolean _fileBasedSubsettingDisabled;

  public void load() {
    _developmentMode = Boolean.parseBoolean(getOptionalVar("DEVELOPMENT_MODE", "true"));
    _appDbSchema = getOptionalVar("APP_DB_SCHEMA", "eda.");
    _userStudySchema = getOptionalVar("USER_STUDY_SCHEMA", "apidbuserdatasets.");
    _availableBinaryFilesPaths = getOptionalVar("AVAILABLE_BINARY_FILES_PATHS" ,"");
    _dbBuild = getOptionalVar("DB_BUILD", "");
    // TODO Make this mandatory once docker-compose changes are deployed.
    _binaryFilesDirectory = getOptionalVar("BINARY_FILES_DIR", "/tmp/binaryFilesDir");
    _datasetAccessServiceUrl = getRequiredVar("DATASET_ACCESS_SERVICE_URL");
    _fileBasedSubsettingDisabled = Boolean.parseBoolean(getOptionalVar("DEVELOPMENT_MODE", "false"));
  }

  public boolean isDevelopmentMode() {
    return _developmentMode;
  }

  public boolean isFileBasedSubsettingDisabled() {
    return _fileBasedSubsettingDisabled;
  }

  public String getAppDbSchema() {
    return _appDbSchema;
  }

  public String getUserStudySchema() {
    return _userStudySchema;
  }

  public String getDbBuild() {
    return _dbBuild;
  }

  public String getDatasetAccessServiceUrl() {
    return _datasetAccessServiceUrl;
  }

  public String getBinaryFilesDirectory() {
    return _binaryFilesDirectory;
  }

  public String getAvailableBinaryFilesPaths() {
    return _availableBinaryFilesPaths;
  }
}
