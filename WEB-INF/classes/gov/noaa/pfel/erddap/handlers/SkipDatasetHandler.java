package gov.noaa.pfel.erddap.handlers;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class SkipDatasetHandler extends StateWithParent {
  private int level = 1;

  public SkipDatasetHandler(SaxHandler saxHandler, State completeState) {
    super(saxHandler, completeState);
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) {
    if (localName.equals("dataset")) {
      level++;
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {}

  @Override
  public void endElement(String uri, String localName, String qName) {
    if (localName.equals("dataset")) {
      level--;
      if (level == 0) {
        saxHandler.setState(this.completeState);
      }
    }
  }
}
