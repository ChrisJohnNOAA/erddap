package gov.noaa.pfel.erddap.filetypes;

import gov.noaa.pfel.erddap.dataset.TableWriter;
import gov.noaa.pfel.erddap.dataset.TableWriterAllWithMetadata;
import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.util.EDStatic;

@FileTypeClass(
    fileTypeExtension = ".json",
    fileTypeName = ".ncoJsonHeader",
    infoUrl = "https://nco.sourceforge.net/nco.html#json",
    versionAdded = "2.25",
    contentType = "application/json",
    addContentDispositionHeader = false)
public class NcoJsonHeaderFiles extends NcoJsonFiles {

  @Override
  public void writeTableToFileFormat(DapRequestInfo requestInfo, TableWriter tableWriter)
      throws Throwable {
    if (tableWriter instanceof TableWriterAllWithMetadata twalwm) {
      String jsonp = EDStatic.getJsonpFromQuery(requestInfo.language(), requestInfo.userDapQuery());
      saveAsNcoJson(requestInfo.outputStream(), twalwm, jsonp, false);
    }
  }

  @Override
  public void writeGridToStream(DapRequestInfo requestInfo) throws Throwable {
    saveAsNcoJson(
        requestInfo.language(),
        requestInfo.requestUrl(),
        requestInfo.userDapQuery(),
        requestInfo.outputStream(),
        requestInfo.getEDDGrid(),
        false);
  }

  @Override
  public String getHelpText(int language) {
    return EDStatic.messages.get(Message.FILE_HELP_NCO_JSON_HEADER, language);
  }
}
